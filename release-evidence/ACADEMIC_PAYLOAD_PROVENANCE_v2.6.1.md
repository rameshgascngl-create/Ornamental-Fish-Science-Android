# Academic Payload Provenance — v2.6.1

## Scope

This release-hardening pass does not modify the academic ornamental-fish payload.

The build restores the payload from:

- Source container: `OrnamentalFish-v2.4.4-NEW-STORE-SOURCE.zip`
- Repository: `rameshgascngl-create/Zoology-and-Life-Sciences-Digital-Learning-Resources`
- Source ZIP Git blob SHA: `9175980f7814ba73533afd40776b3f17c9bcf835`

The academic-content integrity gate remains:

- `release-evidence/ACADEMIC_PAYLOAD_SHA256_v2.4.3.txt`
- Manifest Git blob SHA: `720723cb8f210efac863052c8c740d94954dfd2a`

## Why the labels differ

The v2.4.4 source ZIP is the transport/reconstruction container used by the Android build. The v2.4.3 SHA-256 manifest is retained as the controlling immutable hash set for the frozen academic payload inherited by later shell/store-hardening releases.

The version-label difference is therefore intentional only if the restored academic files continue to match the v2.4.3 hash manifest byte-for-byte.

## Release gate

The CI workflow MUST:

1. restore the v2.4.4 source payload;
2. verify the restored academic files against `ACADEMIC_PAYLOAD_SHA256_v2.4.3.txt`;
3. fail immediately on any hash mismatch;
4. only after a successful hash check, apply user-facing shell/version stamping for v2.6.1;
5. never rewrite the frozen academic payload merely to satisfy the version stamp.

A successful CI integrity check is the evidence that the v2.4.4 reconstruction container still reproduces the accepted frozen academic payload governed by the v2.4.3 hash manifest.

## v2.6.1 correction boundary

Permitted changes in this pass are limited to Android shell/store hardening, release identity, workflow verification, accessibility/localization shell strings, and release evidence. Academic fish descriptions, atlas data, species content, quiz content, and teaching payload are out of scope and must remain unchanged.
