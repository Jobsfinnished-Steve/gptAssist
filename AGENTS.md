# Repository guidance

- This is a Java Android WebView app; preserve its existing structure and behavior.
- Never resize or recompress selected images. Read EXIF only; never call `saveAttributes`.
- Never auto-send a ChatGPT message or add analytics, external logging, or advertising SDKs.
- Never log coordinates or capture times. GPS permission denial must not block uploads.
- Keep ChatGPT DOM selectors isolated in `ChatGptComposerBridge.java`.
- Preserve ordinary non-image file uploads and existing privacy/URL restrictions.
- After changes run `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`.
- Logged-in WebView uploads and attachment behavior require validation on a real Android device.
