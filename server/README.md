# Temple server safeguards

This directory is server-only code, not shipped in the Android app. It has no
running listener, deployment entry point or enabled provider. Never put a
service-role key, hook secret or HMAC secret in mobile/desktop/browser code.

`npm ci && npm test` runs PostgreSQL-compatible database tests in PGlite plus
signature, retry and raster-normalization tests. Set TEST_DATABASE_URL to an
EMPTY disposable PostgreSQL database for actual multi-connection tests. Tests
create roles and schemas; never point them at a real project. The manual
no-release CI workflow supplies a disposable PostgreSQL 16 service.

Migrations 0006 and 0007 are additive preparation and have NOT been applied to a
hosted project. Both default to disabled. 0007 intentionally fails if its bucket
already exists rather than adopting unknown data/policies. Existing gallery
rows, revisions, website URLs and behavior remain unchanged. Neither migration
publishes a photo. Do not apply automatically from CI.

OTP handler: verifies Standard Webhooks signature/freshness on raw body, derives
keyed recipient/delivery identifiers, reserves cost in a separate committed RPC,
then dispatches through an injected server adapter and records outcome. The
provider adapter remains absent until sender/template/rate approval. An unknown
or failed recording never refunds the reservation. Do not retry unknown sends.
The same phone/code dedupes across changed retry webhook IDs. Preserve HMAC key
continuity for outstanding reservations; key rotation needs overlap. Retention
must be long enough for billing/replay and quota enforcement; never prune this
ledger merely to reset a budget. Ledger rows contain no plaintext OTP or phone.

No live amount is selected: an operator must set approved integer minor-unit
per-send worst-case cost, daily/monthly caps, unexpired rate quote, recipient
rolling-hour/day caps and owned-recipient HMAC allowlist. Reserve amount must
include all provider fees/taxes. Currency/rate changes need explicit review;
do not mix billing currencies in an active ledger. Native Auth limits and
CAPTCHA are additional requirements, not a monetary cap. Direct public /otp
must remain protected. The app CAPTCHA challenge/token path is not yet wired.

Photo slots: authenticated admin membership and disabled config are checked in
SQL, with path-bound private original insert and no client read/update/delete
policy. Originals remain private. Service-only normalization completion checks
object presence, but the worker must actually call normalizePhoto and store its
output before asserting completion. Client callers cannot mark a file
processed. Review is attributed to an admin and is NOT publication. No preview
capability or publication/data-renderer change has been enabled yet.

Before activation: inspect all existing storage RLS policies for broad OR grants,
verify on hosted Storage and actual admin/visitor JWTs, add upload byte/account
quotas and cleanup, and run image decoding in a patched isolated worker with
CPU/memory/time/filesystem/network limits. Sharp limits pixels, size and formats,
rotates orientation and removes metadata; re-encoding is risk reduction, not a
proof that an image is harmless. The library function alone is not an OS
sandbox. Do not expose it as an unrestricted request handler.

Remaining app work: picker/preview/progress/cancel/retry, bilingual errors,
private preview signing, transactional asset-publication state/outbox,
publication-aware serving, backwards-compatible asset IDs/renderer, and delayed
reference-safe deletion. Device UI, encrypted sessions, hosted provider delivery
and account configuration remain separate verification gates. Green tests do
not authorize live traffic, paid activation, uploads, publication or rollout.
