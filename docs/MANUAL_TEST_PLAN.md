# Manual test plan

Use test media with non-sensitive synthetic metadata. For every image case, verify original bytes are unchanged, attachment order matches `IMAGE_001…`, one `PHOTO_CONTEXT.txt` is appended, and no message is automatically sent.

| # | Scenario | Expected result |
|---|---|---|
| 1 | One JPEG | JPEG attaches and context is appended to the composer. |
| 2 | Multiple JPEGs | All originals attach in chooser order and one context block is appended to the composer. |
| 3 | Multi-select in Samsung Gallery | Selection order is retained. |
| 4 | Multi-select in system picker | Selection order is retained. |
| 5 | HEIC/HEIF | Original attaches; readable metadata is included. |
| 6 | PNG screenshot | `SCREENSHOT` only when MediaStore path/bucket supports it. |
| 7 | Photo with GPS | Coordinates appear with at most six decimals; EXIF altitude appears with at most one decimal. |
| 8 | Photo without GPS | `gps=none` and `gps_altitude_m=none`. |
| 9 | Allow media-location permission | Embedded GPS is read when the provider permits it. |
| 10 | Deny media-location permission | Upload continues and GPS is omitted. |
| 11 | Download without EXIF | MediaStore/file fallback is labeled accurately. |
| 12 | Edited image | Use `EDITED` only with trustworthy evidence; otherwise `UNKNOWN`. |
| 13 | Corrupt EXIF | Other metadata/photos and upload survive. |
| 14 | Photo versus PDF/TXT/ZIP | Ordinary files follow the original chooser path without context. |
| 15 | Cancel chooser | No attachment and no stale callback. |
| 16 | Manual fallback with existing draft | Two blank lines and context are appended; draft remains. |
| 17 | Repeat manual fallback | Identical context is not duplicated. |
| 18 | Restart app | ChatGPT login cookie/session behavior remains unchanged. |
| 19 | Toggle URL blocking | Existing allow/block behavior still works. |
| 20 | Galaxy S23 / Android 16 | Complete all chooser, permission, and WebView checks on hardware. |
| 21 | Attachment order | Photos correspond exactly to numbered blocks. |
| 22 | Send behavior | App never submits or clicks Send. |
| 23 | Original preservation | Compare size/hash before and after; no transform occurs. |

Also test picker cancellation during rotation/backgrounding, rapid duplicate chooser requests, page reload, large batches, unreadable content URIs, and both context/GPS settings from the WebView long-press menu. These WebView, ChatGPT-login, Samsung-picker, mixed-MIME acceptance, and Android 16 checks are **not** considered verified until performed on a physical device.

## Galaxy S23 / Android 16 GPS and altitude checks

1. Select a camera photo that Samsung Gallery reports as having location and altitude. Confirm the media-location dialog appears only after selection.
2. Allow access and confirm coordinates and `gps_altitude_m` are present when their EXIF tags exist.
3. Deny access and confirm `gps=none` and `gps_altitude_m=none` while the original photo still attaches.
4. Select a photo with coordinates but no altitude and confirm coordinates remain while `gps_altitude_m=none`.
5. Confirm permission handling does not reopen the picker and completes the WebView callback exactly once.

## Android 16 / Galaxy S23 v3 delivery matrix

| Test | Scenario | Expected result |
|---|---|---|
| A | Samsung camera photo known to contain GPS; grant media-location access | Coordinates and `gps_status=AVAILABLE`; altitude appears when its EXIF tags exist. |
| B | Same photo; deny media-location access | Photo still attaches; `gps=none`, `gps_altitude_m=none`, `gps_status=PERMISSION_DENIED`. |
| C | Photo without GPS with successful original access | `gps=none`, `gps_altitude_m=none`, `gps_status=NO_GPS_TAG`. |
| D | Original metadata access unavailable | Capture date remains when readable; location fields are `none` and `gps_status=ORIGINAL_ACCESS_FAILED`. |
| E | Select two photos | Exactly two image attachments, no TXT attachment, v3 context in composer, and first URI maps to `IMAGE_001`. |
| F | Type a draft before selecting images | Draft remains, two newlines and v3 context are appended, and nothing is sent automatically. |

On API 36 the GPS-enabled image-library path must use `ACTION_OPEN_DOCUMENT`. Confirm permission handling never reopens the picker and that selecting multiple images preserves `ClipData` order.
