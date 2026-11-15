package page.kakkamvellytemple.app.data.repository

/** Access-token provider for a signed-in admin. No enrollment or sign-in flow exists
 *  in the app yet: the production provider is wired only after the owner's
 *  auth-channel decision. Until then it returns null and every admin operation
 *  is denied. */
fun interface AdminAuthTokenProvider {
    suspend fun accessToken(): String?
}

/** The backend refused the write (401/403) or silently applied zero rows, which is
 *  what PostgREST returns for UPDATE/DELETE statements that fail the RLS USING
 *  clause. Both mean the caller is not an authorized admin; never treat either as
 *  success. */
class AdminDeniedException(message: String = "Admin write denied by backend") : Exception(message)

/** The owner has not approved this operation (e.g. real notification sends). */
class NotApprovedException(message: String) : Exception(message)
