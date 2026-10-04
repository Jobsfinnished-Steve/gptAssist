# Manual test plan

This build has one purpose: display ChatGPT without an application URL allowlist and transport selected images as byte-identical `.txt` proxy attachments. It does not create or insert PHOTO_CONTEXT, download files, provide voice controls, or expose a URL-restriction toggle.

## Cloudflare and login — Galaxy S23 / Android 16

1. Install the new APK with a compatible signature (or use a separate test installation); preserve the user's data. Test a clean install and an upgrade separately. Record Android System WebView/Chrome version.
2. Launch the app on an ordinary network with automatic date/time enabled.
3. Confirm ChatGPT and all Cloudflare verification resources load without tapping an allowlist button.
4. Complete Cloudflare verification and ChatGPT login. Confirm the session remains after closing and reopening the app.
5. Repeat with Google login if used. Record any loop together with Android System WebView version and whether the same URL works in Chrome; do not record cookies or tokens.

## Upload proxy

1. Long-press the WebView and select `TEXT` (`text/plain`).
2. Press ChatGPT **+ → photo/image attachment** and select a Samsung camera JPEG with known GPS EXIF.
3. Confirm the system Files/document picker opens and allow the photo-location metadata permission after selection. Confirm exactly one `.jpg.txt` attachment appears and no PHOTO_CONTEXT block is inserted.
4. Open **Upload proxy diagnostics** before sending and record only:
   - original-access status;
   - selected/proxy counts;
   - byte identity;
   - GPS coordinates readable YES/NO;
   - GPS altitude readable YES/NO;
   - callback completion.
5. If proxy GPS is YES, download and independently parse the corresponding uploaded object. If its GPS is absent, investigate processing after the app/provider boundary; a model response alone is not proof. If proxy GPS is NO, check the known original, permission and provider path. NOT_CHECKED means the read/parser did not complete.
6. Repeat with `OCTET_STREAM` and `IMAGE_MIME`.
7. Select three images and verify three proxies appear in tap order.
8. Select PDF/TXT/ZIP files and verify they pass through unchanged without proxy disguise.
9. Cancel selection and rapidly start two chooser requests; verify no stale or duplicate attachment callback.
10. Verify no image is resized, recompressed, rewritten, moved, or renamed in the gallery.

## Regression cases (debug AND release)

- With broad photo-library permissions denied, choose a known GPS-bearing local JPEG from Files → Images or internal storage → DCIM. Grant photo-location permission. Expect ORIGINAL_AVAILABLE and GPS YES for a supported local document.
- Deny/dismiss location permission: upload completes once, diagnostics show the access failure and a no-readable-GPS notice appears when appropriate. Re-enable via system app settings and reselect. No endless permission/pre-chooser loop.
- Cancel or replace an upload while permission is pending: the old callback receives null once; only the latest selected upload can resume. Close/recreate the activity while pending: no stale callback or crash.
- Choose a cloud/vendor document: either verified readable GPS is present, or an explicit unresolved/provider status appears; do not promise recovery from redacted bytes.
- Choose a GPS-free screenshot followed by a GPS-bearing photo, then reverse their order. Both per-image results must remain visible. The first image's failure must not be masked by the last image.
- Choose two photos with the same display name from different folders; verify separate proxies, stable bytes and order. Recreate the activity and upload another image: older proxies must not be overwritten.
- Choose a photo from removable storage, if available; verify the mapped volume/Files URI path works without guessing an ID.
- Repeat TXT/PDF/ZIP and mixed image/document selections; preserve the existing non-image pass-through behavior. GPS preservation is implemented for image-only batches.
- Verify URL navigation remains unrestricted, no coordinate values appear in logs/diagnostics, and selected source files remain unchanged.
