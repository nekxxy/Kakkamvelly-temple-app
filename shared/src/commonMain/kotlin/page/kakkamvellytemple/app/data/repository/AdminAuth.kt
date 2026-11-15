package page.kakkamvellytemple.app.data.repository

/** Access-token provider for a signed-in admin. No sign-in flow exists in the app
 *  yet; wire the production provider when auth is added. Until then it returns null
 *  and every admin operation is denied. */
fun interface AdminAuthTokenProvider {
    suspend fun accessToken(): String?
}

/** The backend refused the write (401/403) or silently applied zero rows, which is
 *  what PostgREST returns for UPDATE/DELETE statements that fail the RLS USING
 *  clause. Both mean the caller is not an authorized admin; never treat either as
 *  success. */
class AdminDeniedException(message: String = "Admin write denied by backend") : Exception(message)

/** This operation is not enabled (e.g. real notification sends stay disabled). */
class NotApprovedException(message: String) : Exception(message)

/** The section moved on since the caller read it: a competing publish landed first.
 *  The publish was NOT applied; re-read the section and retry with the current
 *  revision. Raised from the database's expected-revision check (SQLSTATE 40001). */
class RevisionConflictException(message: String) : Exception(message)
