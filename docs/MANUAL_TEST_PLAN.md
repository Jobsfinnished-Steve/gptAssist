# Manual test plan

Use test media with non-sensitive synthetic metadata. For every image case, verify original bytes are unchanged, attachment order matches `IMAGE_001…`, one `PHOTO_CONTEXT.txt` is appended, and no message is automatically sent.

| # | Scenario | Expected result |
|---|---|---|
| 1 | One JPEG | JPEG and one context file attach. |
| 2 | Multiple JPEGs | All originals and one context attach in chooser order. |
| 3 | Multi-select in Samsung Gallery | Selection order is retained. |
| 4 | Multi-select in system picker | Selection order is retained. |
| 5 | HEIC/HEIF | Original attaches; readable metadata is included. |
| 6 | PNG screenshot | `SCREENSHOT` only when MediaStore path/bucket supports it. |
| 7 | Photo with GPS | Coordinates appear with at most six decimals. |
| 8 | Photo without GPS | `gps=none`. |
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
