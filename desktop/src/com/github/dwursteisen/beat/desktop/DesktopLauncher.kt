package com.github.dwursteisen.beat.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.dwursteisen.beat.BeatTheHighScore
import io.anuke.gif.GifRecorder

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        LwjglApplication(object : BeatTheHighScore() {

            lateinit var recorder: GifRecorder

            override fun create() {
                recorder = GifRecorder(SpriteBatch())
                super.create()
            }

            override fun render() {
                if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
                    BeatTheHighScore.nextLevel() // little cheat..
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                    recorder.takeScreenshot()
                }
                super.render()
                recorder.update()
            }
        }, config)
    }
}
