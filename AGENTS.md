# Repository guidance

- This is a Java Android WebView app reduced to ChatGPT navigation and byte-identical image upload proxies.
- Keep WebView navigation unrestricted; do not reintroduce an application URL allowlist or restriction toggle.
- Never resize, decode, recompress, rename, move, or overwrite selected source images.
- EXIF access is read-only; never call `saveAttributes`, and never log coordinates, altitude, timestamps, filenames, or URIs.
- Never auto-send a ChatGPT message or add analytics, external logging, advertising, download, or voice features.
- Preserve ordinary non-image uploads and callback exactly-once behavior.
- After changes run `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`.
- Cloudflare/login and attachment behavior require validation on a real Android device.
