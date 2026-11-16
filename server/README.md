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
SQL, with path-bound private original insert and no client original-read/update/delete
policy. Processed derivatives have an authenticated admin-only preview SELECT
policy; visitors and anonymous callers cannot preview. Originals remain private. Service-only normalization completion checks
object presence, but the worker must actually call normalizePhoto and store its
output before asserting completion. Client callers cannot mark a file
processed. Review is attributed to an admin and is NOT publication. No preview
capability or publication/data-renderer change has been enabled in the app yet.

Before activation: inspect all existing storage RLS policies for broad OR grants,
verify on hosted Storage and actual admin/visitor JWTs, verify total-asset24 cap and per-admin5/hour issuance limit,
add project-wide byte accounting and cleanup/reconciliation, and run image decoding in a patched isolated worker with
CPU/memory/time/filesystem/network limits. Sharp limits pixels, size and formats,
rotates orientation and removes metadata; re-encoding is risk reduction, not a
proof that an image is harmless. The library function alone is not an OS
sandbox. Do not expose it as an unrestricted request handler.

Prepared but device-unverified app code now includes picker/private preview,
busy state, list refresh and private-review UI, with English/Malayalam copy.
Production navigation does not call it. Authenticated previews use admin JWTs,
not bearer signed URLs. Remaining app work: progress/cancel/reconciliation retry,
transactional asset-publication state/outbox,
publication-aware serving, backwards-compatible asset IDs/renderer, and delayed
reference-safe deletion. Device UI, encrypted sessions, hosted provider delivery
and account configuration remain separate verification gates. Green tests do
not authorize live traffic, paid activation, uploads, publication or rollout.


Original cleanup (migration0008): service-only batch returns processed/reviewed
original paths. cleanupPhotoOriginals validates the whole batch, deletes exact
originals through Storage API, then asks the server to confirm metadata absence
before recording original_erased_at. Failures remain pending. It never deletes
asset rows/derivatives or reopens capacity. Expired pending slots and uncertain
processing derivatives deliberately remain held until a separate worker-lease
and orphan-reconciliation protocol is implemented; do not reclaim them by age
alone, which could race an in-flight processor. No cleanup schedule/deployment
is enabled. Existing reservation history also blocks silent currency changes.


Processing fencing (migration0009): each worker must claim a two-minute lease,
write its derivative at an immutable asset/lease-token path, then complete with
that token before expiry. Concurrent claims serialize. Late/stale workers cannot
assert processing or overwrite the next worker's output. Former unfenced finish
RPC is revoked from service_role. Stale private derivatives remain quarantined
and counted conservatively until an orphan reconciler exists; never publish or
reclaim them merely from age. No background runner or deployment is enabled.
Each asset is limited to two processing claims, bounding generated derivative
slots rather than leaking storage across endless failed retries. Exhausted assets
remain held for explicit reconciliation. With the 24asset cap and max8MiB input,
4MiB output, this reserves at most384MiB of image payload for this workflow,
excluding other project data, database/storage overhead and retained legacy files.
These are engineering ceilings, not a claim about the live plan's free allowance.

### Credential-free decoder subprocess (still disabled)

`processPhoto` now defaults to `normalizeInSubprocess`, not an in-process Sharp
call. It starts one fixed Node entry point without a shell, service credentials,
proxy settings, preload hooks or inherited environment. Input is capped at
8 MiB, output at 4 MiB, and wall time at 20 seconds. An invalid result, child
error, nonzero exit or deadline fails closed without marking the asset processed.
The child disables Sharp cache, uses one Sharp thread, and only emits the
normalized JPEG. Pipe buffers are cleared when the operation settles.

This is **not an OS security sandbox**. The 64 MiB V8 heap flag does not bound
Sharp's native memory. A compromised native decoder can still access the host's
filesystem and network. A deployment with enforced CPU/native-memory limits,
no network and a minimal read-only filesystem remains an activation prerequisite.
There is no deployment manifest claiming to provide those restrictions here.
The service key stays in the parent, which only passes the raster bytes over
stdin. No uploads, migrations or public serving are enabled by this change.

### Private publication intent queue (0010, unapplied)

A separate default-disabled config governs `queue_reviewed_temple_photo`; enabling
private uploads does not enable publication requests. An admin can queue exact
captions, a reviewed asset and the current gallery revision with a caller-saved
request UUID. The RPC checks normalized-object presence. Identical retries return
the same intent; a changed payload with that UUID fails. Concurrent retries write
one row. At most 24 queued intents are accepted. Cancellation is idempotent and
works while new requests are disabled. Retrying a canceled UUID never restores it.

This stage is a private outbox only, not publication. It has no worker claim,
public bucket, visitor read policy, gallery mutation, revision increment or
verification change. The Kotlin repository has queue/cancel methods with an
independent default-off queue flag, but no production caller or UI entry point.
A future worker/finalizer must re-check membership, asset review and current
revision, honor cancellation and safely stage/confirm immutable derivatives
before a gallery manifest can change. Legacy visitor URLs remain untouched.

### Disabled publication manifest and read gateway (0011, unapplied)

The next stage adds a second **private** bucket and a separately default-disabled
`delivery_enabled` switch. No direct anonymous/admin object-read policy is added.
One publication intent per asset and two staging attempts bound derivative growth.
The conservative byte ceiling becomes 24 * (8 + 2 * 4 + 2 * 4) = 576 MiB before
cleanup, excluding database/project overhead and all unrelated storage. This is
an engineering ceiling, not a claim about a live free-tier allowance. Slots and
intent rows are retained; no automatic quota reopening or orphan reclamation.

The service worker claims a two-minute lease, copies only the reviewed normalized
JPEG to a unique immutable private target, then finalizes the exact token. SQL
re-checks active requester/reviewer membership, source asset, current gallery
revision, cancellation, lease and target object. Finalization creates the read
manifest, marks the request published, bumps gallery revision and resets committee
verification in one transaction. A canceled, expired or revision-conflicting
completion exposes no staged bytes. An uncertain write/finalization is not retried
automatically. An already published request cannot be canceled as an intent.
Per-photo unpublish/edit management is not implemented yet. The delivery switch
hides existing manifests and metadata too; disabling new requests alone does not.

`createGalleryPhotoHandler` is a prepared Fetch-compatible read handler, not a
running endpoint. It defaults off. It accepts only canonical asset/publication
IDs, looks up a current finalized manifest, reads at most 4 MiB of JPEG and
re-checks the manifest after download. No originals, caller URLs/paths, credentials,
signed redirects or internal object paths are returned. Responses use no-store
and nosniff. Server-only adapters enforce a fixed Supabase origin, no redirects,
insert-only targets and five-second network deadlines. No deployment, listener,
Edge-function configuration, hosting/egress budget control or live route exists.
The no-store gateway must NOT be activated before egress/rate limits and a hosting
budget are verified. Default-off source code is not an approval to publish.

The visitor source and URL resolver have an independent default-off upload flag.
When enabled later, they append canonical uploaded references to legacy photos.
Existing valid website image URLs and legacy gallery rows are unchanged. Uploaded
references never pass through the legacy admin slug editor or publish_gallery RPC.
The new image path is derived from the configured Supabase origin and two UUIDs,
never a remote URL. Empty/disabled manifests remove uploaded references on refresh;
legacy packaged fallbacks still keep the unverified notice where appropriate.
Actual gateway routing, authenticated Storage/RLS and Android/Malayalam pixels
remain unverified until hosted/device checks are available and approved.

### Retirement and captions (0012, unapplied)

Admins can edit a published caption using the current gallery revision. Each edit
bumps that revision, clears committee verification and records the actor, exact
new captions and revision in a private audit row. The original publication request
is unchanged. Concurrent edits cannot silently overwrite each other. A fixed
50-edit-per-photo audit ceiling bounds growth; no-op edits at the current revision
change nothing. New caption edits require new requests to be enabled.

Retirement is monotonic and works with both delivery and new requests disabled.
It removes the photo from the visitor metadata and current gateway manifest,
including after the global delivery switch is later enabled again. A repeated
retirement does not bump revision or create another audit row. Retirement never
deletes objects or reopens capacity; there is no reactivation RPC. It cannot
recall bytes someone already downloaded. A gateway read already buffered is
re-checked before return, but no distributed system can retract a response that
was already delivered. The Kotlin repository has gated edit/retire methods and
unit tests, but no production admin UI or live RPC wiring has been enabled.

### Durable gateway payload/request reservations (0013, unapplied)

The image handler now requires a server-only durable budget adapter. Before any
manifest/storage read, each canonical GET or HEAD reserves the full 4 MiB image
maximum and one request. SQL serializes reservations against configured UTC
hourly request, daily payload and monthly payload ceilings. Enabled=false,
zero/unset ceilings, stale validity or the delivery kill switch deny reservations.
Uncertain outcomes, missing images and HEAD requests keep the full reservation;
there is no refund, retry credit or ledger reset. Client roles cannot inspect or
change the budget. No IP, phone number or other visitor identifier is collected.
No live allowance or policy amount is configured by this source change.

This limits reserved **JPEG payload and canonical handler requests**, not a
provider's invoice. Rejected traffic, function invocations, metadata RPC traffic,
HTTP/JSON overhead, platform bandwidth accounting, other project traffic and an
attacker's requests outside this handler are not covered. Enforced edge rate
limits, service quotas, billing alerts/hard caps where supported, current hosted
allowances and a verified hosting route remain activation prerequisites. Do not
interpret the byte ledger as an assurance that a free-tier deployment cannot bill.

Android cache handling: the uploaded-photo gallery passes an explicit no-cache
policy to Coil for both grid and lightbox reads. Legacy website photos retain
their existing cache behavior. This is needed because Coil 3 does not respect
HTTP Cache-Control by default. Memory and disk cache reads/writes are disabled
for the uploaded route, but an image already displayed or copied cannot be
recalled by retirement. Compile/unit checks do not prove Android cache/runtime
behavior; actual-device checks remain an activation prerequisite.
