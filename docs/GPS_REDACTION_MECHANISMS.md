# Android image GPS redaction mechanisms

## What Android can remove before gptAssist reads the file

Under scoped storage, Android may hide photo location metadata even though the app can read the image and ordinary EXIF. Android's documented unredacted MediaStore path requires all of the following:

1. read access to the selected item, including a Storage Access Framework document grant;
2. `ACCESS_MEDIA_LOCATION` on Android 10+;
3. a canonical `MediaStore` URI that the app can access;
4. `MediaStore.setRequireOriginal(mediaStoreUri)` before opening the stream.

See Android's official [Access location information from photos](https://developer.android.com/training/data-storage/shared/media#location-info-photos) documentation and [`MediaStore.setRequireOriginal`](https://developer.android.com/reference/android/provider/MediaStore#setRequireOriginal(android.net.Uri)).

A Photo Picker, cloud provider, Samsung provider, or Storage Access Framework URI is a capability for that selected item; it is not necessarily the canonical MediaStore URI. Random path segments must not be interpreted as MediaStore IDs. If the provider serves a redacted stream, copying it byte-for-byte only preserves the redacted bytes. Renaming that cache copy to `.txt` cannot restore GPS.

## What can happen after WebView hands off the proxy

If the cache proxy itself reports readable GPS immediately before the WebView callback, but the uploaded object later contains `NUL`, `0/0`, or `NaN` GPS fields, removal happened after gptAssist created and verified the proxy. That is consistent with upload-side image privacy processing, but it cannot be attributed to a particular ChatGPT server component without an authoritative OpenAI statement.

Filename and MIME experiments affect routing; they do not prove where redaction occurred. A manually renamed local `.txt` can follow a different Android provider and upload route from an image selected through Photo Picker or MediaStore.

## Why a Google Drive upload can retain EXIF

A Drive upload may receive bytes through a different source URI, Storage Access Framework grant, share flow, or file path and may store the object without an image privacy transformation. Its success demonstrates that one route preserved the bytes; it does not establish that Android returns identical bytes through every picker/provider or that every destination processes uploads the same way.

## How to distinguish the boundary

Before sending, use **Upload proxy diagnostics**:

- `Original access: ORIGINAL_AVAILABLE` confirms the `setRequireOriginal()` URI was readable.
- `Bytes identical: YES` confirms the chosen input stream was copied unchanged.
- `Proxy GPS coordinates readable: YES` confirms latitude/longitude are readable in the actual proxy handed to WebView.
- `Proxy GPS altitude readable: YES` confirms altitude is readable in that proxy.

If proxy GPS is `NO`, investigate the Android URI/permission/provider path. If proxy GPS is `YES` but the uploaded object is redacted, investigate the destination upload path. Do not log or display the coordinate or altitude values themselves.

## Restored original-file path

`ACTION_OPEN_DOCUMENT` is used instead of `ACTION_GET_CONTENT` for ordinary file selection. After an image-only selection, the app requests `ACCESS_MEDIA_LOCATION` on Android 10+. Denial/cancellation resumes the upload with an explicit diagnostic; it never traps the user before the chooser opens.

On Android 10+, `MediaStore.getMediaUri(context, documentUri)` maps supported MediaDocumentsProvider and ExternalStorageProvider documents while preserving their existing per-file grant. A missing broad `READ_MEDIA_IMAGES` grant is no longer an early rejection. Unmapped cloud/vendor/picker URIs remain unsupported for require-original access; no path segments or filenames are guessed into media IDs. Re-select a local DCIM photo through system Files to test that route.

See [getMediaUri](https://developer.android.com/reference/android/provider/MediaStore#getMediaUri(android.content.Context,android.net.Uri)). The public API can return a MediaStore Files URI and removable-storage volume IDs; both are accepted for items already classified as images.

The provider stream is hashed and parsed for GPS in debug AND release. Byte identity compares the readable copy-source stream with the upload provider stream, not with an independently retrieved camera original. Per-image results avoid hiding a failure behind the last successful selection. `NOT_CHECKED` indicates an unavailable/parser-failed GPS check, not proof that the camera never stored location. A `NO` can also mean the original has no GPS; verify against a known GPS-bearing original before assigning a cause.
