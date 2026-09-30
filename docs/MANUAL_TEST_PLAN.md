# Manual test plan

This build has one purpose: display ChatGPT without an application URL allowlist and transport selected images as byte-identical `.txt` proxy attachments. It does not create or insert PHOTO_CONTEXT, download files, provide voice controls, or expose a URL-restriction toggle.

## Cloudflare and login — Galaxy S23 / Android 16

1. Clear the old app's data, install the new debug APK, and update Android System WebView/Chrome if the device offers an update.
2. Launch the app on an ordinary network with automatic date/time enabled.
3. Confirm ChatGPT and all Cloudflare verification resources load without tapping an allowlist button.
4. Complete Cloudflare verification and ChatGPT login. Confirm the session remains after closing and reopening the app.
5. Repeat with Google login if used. Record any loop together with Android System WebView version and whether the same URL works in Chrome; do not record cookies or tokens.

## Upload proxy

1. Long-press the WebView and select `TEXT` (`text/plain`).
2. Press ChatGPT **+ → photo/image attachment** and select a Samsung camera JPEG with known GPS EXIF.
3. Confirm exactly one `.jpg.txt` attachment appears and no PHOTO_CONTEXT block is inserted.
4. Open **Upload proxy diagnostics** before sending and record only:
   - original-access status;
   - selected/proxy counts;
   - byte identity;
   - GPS coordinates readable YES/NO;
   - GPS altitude readable YES/NO;
   - callback completion.
5. If proxy GPS is YES but ChatGPT later reports `NUL`, `0/0`, or `NaN`, record it as destination-side sanitization. If proxy GPS is NO, record it as Android/provider-side redaction.
6. Repeat with `OCTET_STREAM` and `IMAGE_MIME`.
7. Select three images and verify three proxies appear in tap order.
8. Select PDF/TXT/ZIP files and verify they pass through unchanged without proxy disguise.
9. Cancel selection and rapidly start two chooser requests; verify no stale or duplicate attachment callback.
10. Verify no image is resized, recompressed, rewritten, moved, or renamed in the gallery.
