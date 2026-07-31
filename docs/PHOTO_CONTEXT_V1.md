# PHOTO_CONTEXT v1

`PHOTO_CONTEXT.txt` is UTF-8 text describing only the images selected in the same chooser operation. Selection order is preserved: the first selected photo is `IMAGE_001`, the second `IMAGE_002`, and so on. The text file is not numbered.

Every image block has the fixed field order:

- `captured_at`: local calendar date/time when known, otherwise `unknown`.
- `utc_offset`: EXIF `OffsetTimeOriginal`, or `unknown`. The phone's current zone is never substituted.
- `timestamp_source`: `EXIF_DATETIME_ORIGINAL`, `MEDIASTORE_DATE_TAKEN`, `FILE_LAST_MODIFIED`, or `UNKNOWN`.
- `gps`: embedded latitude/longitude (at most six decimal places), or `none`.
- `media_type`: conservative classification: `CAMERA_PHOTO`, `SCREENSHOT`, `DOWNLOADED`, `EDITED`, or `UNKNOWN`.

An unknown offset is expected when a camera saved wall-clock time without an offset. Only these minimum review fields are extracted; full EXIF is not dumped. Data stays on-device until the user chooses to send the ChatGPT attachments. It is not sent to an analytics or app-operated server. GPS can be disabled and permission denial simply produces `gps=none`.

## Example

```text
PHOTO_CONTEXT v1
This metadata describes the selected image attachments only.
Images correspond to IMAGE_001, IMAGE_002, ... in the same order in which they were selected.
PHOTO_CONTEXT.txt itself is not included in the image numbering.

[IMAGE_001]
captured_at=2026-07-30T18:42:13
utc_offset=+09:00
timestamp_source=EXIF_DATETIME_ORIGINAL
gps=37.49321,127.01482
media_type=CAMERA_PHOTO

[IMAGE_002]
captured_at=unknown
utc_offset=unknown
timestamp_source=UNKNOWN
gps=none
media_type=SCREENSHOT
```
