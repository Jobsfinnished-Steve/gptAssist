# GPS 업로드 회귀 수정

기준: `05a9a72` (master), 비교: `f1127fd`, `f8e6989`, `93bdb5f`, `f6ac36d`, `e33f98e`, `023292e`.
사용자 기기에 설치됐던 정확한 APK/커밋은 확인되지 않았다. 아래는 저장소 이력 비교이며, 모든 과거 버전을 실기기에서 재현했다는 뜻은 아니다.

## 이력에서 확인한 차이

- `f1127fd`: 사진 선택 후 위치 메타데이터 권한 요청과 선택 보관/재개 절차가 있었다. GPS를 별도 PHOTO_CONTEXT 파일로 전달했다.
- `f8e6989`: 컨텍스트를 입력창에 전달하는 흐름으로 변경했다. 당시 GPT가 GPS를 읽었다는 사실만으로 JPEG 원본 EXIF 보존을 입증할 수는 없다.
- `93bdb5f` 이후: 이미지 바이트를 `.txt`로 위장하는 프록시 실험이 추가됐다.
- `f6ac36d`: 미디어/위치 권한 사전 요청을 추가했고, `e33f98e`는 파일 선택을 막는 사전 요청을 되돌렸다.
- 현재 `023292e`/`05a9a72`: URL 제한은 제거됐지만 위치 권한 실행 요청도 없다. 원본 resolver는 전체 미디어 권한이 없으면 선택 파일의 개별 접근권한까지 무시한다. 원본 조회에 실패하면 선택 URI에서 그대로 복사한다. GPS/해시 검사는 debug에서만 실행되고 다중 선택 결과는 마지막 사진 기준이다.

## 수정 내용

1. 일반 파일 선택을 ACTION_OPEN_DOCUMENT로 바꿔, 로컬 문서 접근권한을 받는다.
2. 사진을 선택한 다음 ACCESS_MEDIA_LOCATION을 요청한다. 전체 갤러리 권한 사전 요청으로 파일 선택을 막지 않는다. 거부/취소도 선택한 업로드를 한 번만 재개하며, 이전 선택은 되살리지 않는다.
3. Android 10+에서는 공식 getMediaUri API로 문서 URI를 MediaStore URI로 매핑하고 setRequireOriginal로 연다. 개별 문서 접근권한이 있는데 READ_MEDIA_IMAGES가 없다는 이유만으로 중단하지 않는다. 지원되지 않는 공급자는 진단으로 남긴다.
4. 모든 빌드에서 실제 업로드 Provider의 바이트 해시와 GPS를 검사한다. NaN/무한대/범위 밖 좌표는 성공으로 표시하지 않는다. 정상적인 위도/경도 0은 허용한다.
5. 사진별 진단과 GPS 미확인 안내를 제공한다. GPS가 원래 없는 사진과 Android가 가린 사진을 임의로 동일시하지 않는다.
6. 같은 이름의 선택 파일과 Activity 재생성으로 프록시가 덮어써지지 않게 한다.

URL allowlist, 자동 메시지 전송, PHOTO_CONTEXT 삽입은 추가하지 않았다. 이미지 원본은 수정하지 않는다. 혼합 파일 선택은 기존처럼 원본 URI를 전달한다.

## 적용

번들 `gps-upload-fix.patch`는 기준 커밋에 대한 변경 전체이며, `files/`에는 변경 파일들이 저장소 경로대로 들어 있다. 두 방식 중 하나만 사용한다.

```sh
git switch -c fix/gps-upload
git apply --check /path/to/gps-upload-fix.patch
git apply /path/to/gps-upload-fix.patch
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
git add app README.md docs
git commit -m "Restore original photo GPS access after document selection"
```

Git을 사용하지 않을 경우 `files/` 안의 파일을 저장소 루트에 같은 경로로 덮어쓴다. 새 Java 파일과 테스트 파일도 포함해야 한다. 변경된 브랜치에서 CI를 통과시킨 뒤 PR을 생성/merge한다.

## 검증 범위

- `git diff --check`와 기준 커밋에 대한 패치 적용 검사를 수행한다.
- JDK 17 독립 smoke 검증 28개가 통과했다. 권한 대기 중 선택 취소/교체/중복 완료, 콜백 한 번 완료, NaN/좌표 경계값 처리를 검사했다. Android framework 검증을 대체하지 않는다.
- `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`는 모두 Gradle 8.6 다운로드 시 `Network is unreachable`로 중단됐다. Android 컴파일/lint/JUnit 성공 또는 APK 생성 완료를 주장하지 않는다.
- Galaxy S23 / Android 16에서 `docs/MANUAL_TEST_PLAN.md`를 실행해야 한다. 업로드 직전 GPS YES와 업로드 후 실제 파일 EXIF를 비교해야 남은 제거 지점을 구별할 수 있다.

## 한계

이 패치는 앱의 원본 접근 및 진단 결함을 수정한다. GPT/업로드 서버가 받은 이후 JPEG를 변환한다면 `.txt` 이름만으로 보존을 보장할 수 없다. 프록시 GPS가 YES인데 수신 파일에서 사라질 경우 별도 텍스트 메타데이터 전달 여부를 다음 단계에서 검토한다. 원본 자체에 GPS가 없거나 공급자가 이미 삭제한 경우 좌표를 복원하지 않는다.
