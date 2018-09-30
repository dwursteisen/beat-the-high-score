package com.github.dwursteisen.beat.options

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.beat.BeatTheHighScore
import com.github.dwursteisen.beat.game.Config
import com.github.dwursteisen.beat.game.screenHeight
import com.github.dwursteisen.beat.game.screenWidth
import ktx.log.info
import ktx.scene2d.*
import com.badlogic.gdx.utils.Array as GdxArray

fun Viewport.centerCamera() {
    camera.position.set(worldWidth / 2, worldHeight / 2, 0f)
    camera.update()
}

inline fun <reified T> Collection<T>.asGdx(): GdxArray<T> {
    return GdxArray(toTypedArray())
}

class OptionsScreen(assetManager: AssetManager) : ScreenAdapter() {


    private val bundle: I18NBundle = assetManager["i18n/messages"]

    private lateinit var stage: Stage
    override fun show() {

        info { "Load Options screens with values $Config" }
        stage = Stage(FitViewport(screenWidth, screenHeight))

        val myRoot = table {

            setFillParent(true)
            scrollPane {
                setSmoothScrolling(false)
                setScrollingDisabled(true, false)
                setPosition(0f, 0f)
                setFillParent(false)
                setFadeScrollBars(false)
                setSize(screenWidth, screenHeight - 20f)
                table {

                    label(bundle["options.title"]) {
                        setAlignment(Align.center)
                    }.cell(colspan = 2, fillX = true)
                    row()

                    // -- sound -- //
                    addToggle(bundle["options.sfx"], { Config.sfx = it }, { Config.sfx })
                    addToggle(bundle["options.music"], { Config.music = it }, { Config.music })

                    // -- behaviors -- //
                    addSelect(bundle["options.interpolation"], { Config.interpolation = it }, { Config.interpolation },
                        mapOf(
                            bundle["options.interpolation.current"] to "CURRENT",
                            bundle["options.interpolation.linear"] to "LINEAR",
                            bundle["options.interpolation.elastic"] to "ELASTIC",
                            bundle["options.interpolation.pow2"] to "POW2",
                            bundle["options.interpolation.bounce"] to "BOUNCE"
                        ), "CURRENT")
                    addToggle(bundle["options.box2d"], { Config.box2d = it }, { Config.box2d })
                    addSelect(bundle["options.level"], {
                        Config.level = it
                        BeatTheHighScore.updateLevel(it)
                    }, { Config.level },
                        mapOf(
                            "level 0" to "level0.tmx",
                            "level 1" to "level1.tmx",
                            "level 2" to "level2.tmx",
                            "level 3" to "level3.tmx",
                                "level 4" to "level4.tmx",
                                "level 5" to "level5.tmx",
                                "level 6" to "level6.tmx",
                                "level 7" to "level7.tmx",
                                "level 8" to "level8.tmx"
                        ), "level0.txml")
                    // -- rendering -- //
                    addToggle(bundle["options.font"], { Config.customFont = it }, { Config.customFont })
                    addToggle(bundle["options.shaders"], { Config.shader = it }, { Config.shader })
                    addSelect(bundle["options.transitions"], { Config.transitions = it }, { Config.transitions }, mapOf(
                        bundle["options.transitions.checkerboards"] to "sheets/transition",
                            bundle["options.transitions.gradiant"] to "sheets/transition2",
                            bundle["options.transitions.explode"] to "sheets/transition3"
                    ), "sheets/transition2")
                    addSelect(bundle["options.credit"], { Config.credit = it }, { Config.credit }, mapOf(
                            bundle["options.credit.pixel"] to "pixel",
                            bundle["options.credit.ghost"] to "ghost"
                    ), "sheets/transition2")
                    addToggle(bundle["options.sprites"], { Config.sprites = it }, { Config.sprites })
                    addSelect(bundle["options.viewport"], { Config.viewport = it }, { Config.viewport }, mapOf(
                        bundle["options.fitviewport"] to "FitViewport",
                        bundle["options.fillviewport"] to "FillViewport",
                        bundle["options.screenviewport"] to "ScreenViewport",
                        bundle["options.stretchviewport"] to "StretchViewport",
                        bundle["options.extendviewport"] to "ExtendViewport"
                    ), "Fitviewport")

                    // -- debug -- //
                    addToggle(bundle["options.hitbox"], { Config.hitbox = it }, { Config.hitbox })
                    addToggle(bundle["options.position"], { Config.position = it }, { Config.position })
                    addToggle(bundle["options.direction"], { Config.direction = it }, { Config.direction })
                    addToggle(bundle["options.particles"], { Config.particles = it }, { Config.particles })
                    addToggle(bundle["options.scene2d"], { Config.scene2d = it }, { Config.scene2d })
                }
            }
            row()
            textButton(bundle["options.back"]) {
                addCaptureListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        BeatTheHighScore.title()
                    }
                })
            }
        }


        Gdx.app.input.inputProcessor = stage

        stage.addActor(myRoot)
        stage.isDebugAll = Config.scene2d
        stage.viewport.centerCamera()
    }

    private fun KTableWidget.addSelect(name: String, setter: (String) -> Unit, getter: () -> String, options: Map<String, String>, default: String) {

        label(name, style = "option").cell(align = Align.left, fillX = true, expandX = true)
        val select = selectBoxOf(options.keys.asGdx(), style = "option")
                .cell(align = Align.left)
        select.selected = getter().let {
            options.entries.filter { p -> p.value == it }.map { it.key }.firstOrNull() ?: default
        }
        select.addCaptureListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                setter(options[select.selected]!!)
            }
        })
        row()
    }

    private fun KTableWidget.addToggle(name: String, setter: (Boolean) -> Unit, getter: () -> Boolean) {
        // label(name)
        checkBox(name, style = "option") {
            align(Align.left)
            val value = getter()
            isChecked = value
            addCaptureListener(clickListener(setter))
        }.cell(colspan = 2, align = Align.left)
        row()
    }

    private fun clickListener(pref: (Boolean) -> Unit): ClickListener {
        return object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                with((event?.listenerActor as CheckBox)) {
                    pref(!isChecked)
                }
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(91f / 256f, 110f / 256f, 225f / 256f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height)
    }
}
