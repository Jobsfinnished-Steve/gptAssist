# Codex/cloud Android build setup

This project uses Gradle 8.6 and Android Gradle Plugin 8.2.2. Use a supported JDK (JDK 17 is recommended), not JDK 25:

```sh
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
```

Install Android SDK platform 35 and matching build tools, accept licenses, and set `ANDROID_HOME` (or `sdk.dir` in an untracked `local.properties`). The first build also needs network access to Google's Maven repository, Maven Central, and JitPack so Gradle can download AGP and dependencies.

Then run:

```sh
chmod +x gradlew
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

In the current cloud environment, the default JDK 25 is incompatible with this Gradle version, while the JDK 17 retry cannot resolve the uncached Android Gradle Plugin because repository access is unavailable.
