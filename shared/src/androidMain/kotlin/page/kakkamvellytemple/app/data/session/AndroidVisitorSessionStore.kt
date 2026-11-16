package page.kakkamvellytemple.app.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Prepared Android adapter. AES-GCM key is non-exportable in Android Keystore;
 * only authenticated ciphertext is stored, in app-private noBackupFilesDir.
 * No plain preference fallback. Requires device tests (restart, tamper, key loss,
 * restore) before production wiring. A corrupt/missing key means sign in again.
 * One instance per session manager; the manager serializes all calls. */
class AndroidVisitorSessionStore(context: Context) : VisitorSessionStore {
    private val base = File(context.applicationContext.noBackupFilesDir, "visitor-session.enc")
    private val file = AtomicFile(base)
    private fun deleteCiphertext() {
        file.delete()
        // AtomicFile.delete returns no status. Never silently confirm erasure
        // if its base or recovery files remain on disk.
        check(listOf(base, File(base.path + ".bak"), File(base.path + ".new")).none { it.exists() }) {
            "Local session erasure could not be confirmed"
        }
    }
    private val alias = "kvt.visitor.session.aes.v1"
    private val aad = "kvt.visitor.session.v1".toByteArray(Charsets.UTF_8)
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(create: Boolean): SecretKey? {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        if (!create) return null
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    override suspend fun save(session: VisitorSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true))
        cipher.updateAAD(aad)
        val plain = json.encodeToString(session).toByteArray(Charsets.UTF_8)
        val encrypted = try { cipher.doFinal(plain) } finally { plain.fill(0) }
        val stream = file.startWrite()
        try {
            stream.write(byteArrayOf(1, cipher.iv.size.toByte()))
            stream.write(cipher.iv)
            stream.write(encrypted)
            file.finishWrite(stream)
        } catch (e: Exception) { file.failWrite(stream); throw e }
    }
    override suspend fun load(): VisitorSession? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        try {
            require(file.baseFile.length() in 30..65536)
            val bytes = file.openRead().use { it.readBytes() }
            require(bytes[0].toInt() == 1 && bytes[1].toInt() == 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(false) ?: error("Session key unavailable"),
                GCMParameterSpec(128, bytes.copyOfRange(2, 14)))
            cipher.updateAAD(aad)
            val plain = cipher.doFinal(bytes.copyOfRange(14, bytes.size))
            try { json.decodeFromString<VisitorSession>(plain.toString(Charsets.UTF_8)) }
            finally { plain.fill(0) }
        } catch (_: Exception) {
            // No exception details containing payloads are logged or surfaced.
            deleteCiphertext()
            null
        }
    }
    override suspend fun clear() = withContext(Dispatchers.IO) { deleteCiphertext() }
}
