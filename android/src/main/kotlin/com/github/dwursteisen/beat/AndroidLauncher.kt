package com.github.dwursteisen.beat

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val config = AndroidApplicationConfiguration()
        super.onCreate(savedInstanceState)
        initialize(BeatTheHighScore(), config)
    }
}
