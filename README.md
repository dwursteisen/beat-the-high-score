# Beat the high-score: build a game using libGDX and Kotlin

Playing games is fun but making games is even better, especially with Kotlin and libGDX. Let's build together a breakout game and let's explore the gaming framework libGDX.

This session will show some libGDX's concepts: how to draw and animate elements of our game, how collision system works to destroy our bricks, what Kotlin brings to libGDX. Then we will dive into more advanced topics like shaders, in order to handle pixels from our images. Why are we doing all of this? To break the high score, of course!

## How to build the game?

`./gradlew build`

## How to run the game?

`./gradlew desktop:run`

## How to build the APK for Android?

Create a `keystore.properties` file in this directory:
```
storePassword=storePassword
keyPassword=keyPassword
keyAlias=keyAlias
storeFile=storeFile.jks
```

Create a `local.properties` file in this directory if you didn't have set `ANDROID_HOME`:
```
sdk.dir=<path to the android sdk>
```


`./gradlew android:assembleRelease -PANDROID=true`

Because the android plugin is a bit buggy with IntelliJ, Android project is enable only on demand, thanks
to the parameter `ANDROID`

## How to grab the game from Google Play Store?

[Google Play Store page](https://play.google.com/store/apps/details?id=com.github.dwursteisen.beat)