 ```
Google has announced that, starting in 2026/2027, all apps on certified Android devices
will require the developer to submit personal identity details directly to Google.
Since the developers of this app do not agree to this requirement, this app will no longer 
work on certified Android devices after that time.
```

<pre>Send a coffee to 
woheller69@t-online.de 
<a href= "https://www.paypal.com/signin"><img  align="left" src="https://www.paypalobjects.com/webstatic/de_DE/i/de-pp-logo-150px.png"></a>

  
Or via this link (with fees)
<a href="https://www.paypal.com/donate?hosted_button_id=XVXQ54LBLZ4AA"><img  align="left" src="https://img.shields.io/badge/Donate%20with%20Debit%20or%20Credit%20Card-002991?style=plastic"></a></pre>


| **RadarWeather** | **Gas Prices** | **Smart Eggtimer** |
|:---:|:---:|:---:|
| [<img src="https://github.com/woheller69/weather/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.weather/) | [<img src="https://github.com/woheller69/spritpreise/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.spritpreise/) | [<img src="https://github.com/woheller69/eggtimer/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.eggtimer/) |
| **Bubble** | **hEARtest** | **GPS Cockpit** |
| [<img src="https://github.com/woheller69/Level/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.level/) | [<img src="https://github.com/woheller69/audiometry/blob/new/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.audiometry/) | [<img src="https://github.com/woheller69/gpscockpit/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.gpscockpit/) |
| **Audio Analyzer** | **LavSeeker** | **TimeLapseCam** |
| [<img src="https://github.com/woheller69/audio-analyzer-for-android/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.audio_analyzer_for_android/) |[<img src="https://github.com/woheller69/lavatories/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.lavatories/) | [<img src="https://github.com/woheller69/TimeLapseCamera/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.TimeLapseCam/) |
| **Arity** | **Cirrus** | **solXpect** |
| [<img src="https://github.com/woheller69/arity/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.arity/) | [<img src="https://github.com/woheller69/omweather/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.omweather/) | [<img src="https://github.com/woheller69/solXpect/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.solxpect/) |
| **gptAssist** | **dumpSeeker** | **huggingAssist** |
| [<img src="https://github.com/woheller69/gptassist/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.gptassist/) | [<img src="https://github.com/woheller69/dumpseeker/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.dumpseeker/) | [<img src="https://github.com/woheller69/huggingassist/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.hugassist/) |
| **FREE Browser** | **whoBIRD** | **PeakOrama** |
| [<img src="https://github.com/woheller69/browser/blob/newmaster/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.browser/) | [<img src="https://github.com/woheller69/whoBIRD/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.whobird/) | [<img src="https://github.com/woheller69/PeakOrama/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.PeakOrama/) |
| **Whisper** | **Seamless** | **SherpaTTS** |
| [<img src="https://github.com/woheller69/whisperIME/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.whisper/) | [<img src="https://github.com/woheller69/seamless/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.seemless/) | [<img src="https://github.com/woheller69/ttsengine/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.ttsengine/) |

# gptAssist Photo Upload

This fork keeps the upstream Java WebView structure but intentionally exposes only the functionality needed for the GPS-preserving upload experiment:

- an unrestricted ChatGPT WebView with normal navigation and authentication resources;
- persistent cookies, including third-party cookies required by current Cloudflare/auth flows;
- ordinary document uploads unchanged;
- byte-identical cache proxies for selected images;
- upload-proxy MIME selection and non-sensitive diagnostics from the long-press menu.

The upstream URL blocker, restriction toggle/button, download helper, voice permission flow, star/upgrade prompts, PHOTO_CONTEXT generation, and automatic composer integration are not part of this reduced build.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/org.woheller69.gptassist/)

## License

This app is licensed under the GPLv3.

The app uses:
- Parts from GMaps WV (https://gitlab.com/divested-mobile/maps) which is licensed under the GPLv3

## Contributing

If you find a bug, please open an issue in the Github repository, assuming one does not already exist.
- Clearly describe the issue including steps to reproduce when it is a bug. In some cases screenshots can be supportive.
- Make sure you mention the Android version and the device you have used when you encountered the issue.
- Make your description as precise as possible.

If you know the solution to a bug please report it in the corresponding issue and if possible modify the code and create a pull request.



## GPS-preserving upload proxy fork

This fork is intentionally reduced to a ChatGPT WebView and one upload experiment. It has no application URL allowlist, URL-blocking switch, download helper, voice-control code, PHOTO_CONTEXT generation, composer automation, analytics, or advertising. The WebView uses its normal user agent, accepts first- and third-party cookies needed by Cloudflare/authentication, and permits normal HTTPS navigation.

When all selected files are images, the app copies the readable source bytes unchanged into `cacheDir/chatgpt_upload_proxy/`, exposes each proxy through a narrow read-only provider, and returns the proxies in selection order. The default proxy name/MIME is `original.jpg.txt` with `text/plain`; developer menu modes can report octet-stream or the original image MIME. Non-image files pass through unchanged. See [Android GPS redaction mechanisms](docs/GPS_REDACTION_MECHANISMS.md) and the [manual test plan](docs/MANUAL_TEST_PLAN.md).

This remains a fork of upstream [gptAssist](https://github.com/woheller69/gptassist) and retains its GPLv3 license and attribution.

### Download a debug APK from GitHub Actions

Open the repository's **Actions** tab, select **Build Debug APK**, and choose **Run workflow**. After the run finishes, open it and download **gptassist-photo-context-debug-apk** from **Artifacts**. Extract the downloaded ZIP and install the APK; Android may require you to allow installation from unknown sources for the app used to open it.
