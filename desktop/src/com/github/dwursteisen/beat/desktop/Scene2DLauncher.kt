package com.github.dwursteisen.beat.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.dwursteisen.beat.game.screenHeight
import com.github.dwursteisen.beat.game.screenWidth
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.selectBoxOf
import ktx.scene2d.textButton
import ktx.scene2d.verticalGroup
import com.badlogic.gdx.utils.Array as GdxArray

object Scene2DLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        LwjglApplication(object : ApplicationAdapter() {

            lateinit var stage: Stage

            override fun create() {

                Scene2DSkin.defaultSkin = Skin(Gdx.files.internal("skin/skin2d.json"))
                Scene2DSkin.defaultSkin.getFont("button").setUseIntegerPositions(false)
                Scene2DSkin.defaultSkin.getFont("button-reduit").setUseIntegerPositions(false)

                stage = Stage(FitViewport(screenWidth, screenHeight))
                val scene = verticalGroup {
                    setFillParent(true)

                    label("Hello KotlinConf!")

                    container {
                        textButton("Click On Me") {
                            //label.color = Color.RED
                        }
                    }
                    label("Another label...")

                    selectBoxOf(GdxArray<String>().apply {
                        add("value 1")
                        add("value 2")
                        add("value 3")
                    })


                }
                stage.addActor(scene)

                stage.isDebugAll = true
            }

            override fun render() {
                Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                stage.act()
                stage.draw()
            }

            override fun resize(width: Int, height: Int) {
                stage.viewport.update(width, height)
            }
        }, config)
    }
}
