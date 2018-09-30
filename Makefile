version:
	./gradlew android:incrementVersion -PANDROID=true

apk:
	./gradlew android:assembleRelease -PANDROID=true

release: version apk
	echo "RELEASE into android/build/outputs/apk"
