package com.github.dwursteisen.beat.intro

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.ParticleEffectLoader
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.beat.BeatTheHighScore
import com.github.dwursteisen.beat.game.*
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.StateSystem
import com.github.dwursteisen.libgdx.v2
import ktx.ashley.entity
import ktx.log.debug
import ktx.log.info

class Intro : Component
class TextRender(var text: String = "",
                 var color: Color = Color.WHITE,
                 var scale: Float = 1f,
                 var halign: Int = Align.left
) : Component

class TapToSkip(val txt: String) : Component


class IntroScreen(private val assetsManager: AssetManager) : ScreenAdapter() {

    private lateinit var engine: PooledEngine

    private lateinit var viewport: Viewport

    private lateinit var batch: SpriteBatch

    private var assetsLoaded = false

    override fun show() {

        engine = PooledEngine()
        viewport = FitViewport(128f, 192f)
        batch = SpriteBatch()

        engine.entity {
            entity.add(StateComponent())
                    .add(Position(-128f * 0.5f v2 -192f * 0.5f))
                    .add(Size(128f v2 192f))
                    .add(Intro())
        }

        val font: BitmapFont = if (Config.customFont) {
            assetsManager["krungthep2.fnt"]
        } else {
            BitmapFont()
        }

        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        font.setUseIntegerPositions(false)

        engine.addSystem(IntroSystem(assetsManager))
        engine.addSystem(TapToSkipSystem())
        engine.addSystem(AnimationSystem())
        engine.addSystem(RenderSystem(batch))
        engine.addSystem(TextRenderSystem(batch, font))
        engine.addSystem(StateSystem())



        info { "Loading sprite sheets" }
        assetsManager.load("sheets/chicken", Aseprite::class.java)
        assetsManager.load("sheets/title_screen", Aseprite::class.java)
        assetsManager.load("sheets/renard", Aseprite::class.java)
        assetsManager.load("sheets/brick", Aseprite::class.java)
        assetsManager.load("sheets/clouds", Aseprite::class.java)
        assetsManager.load("sheets/free_chicken", Aseprite::class.java)
        assetsManager.load("sheets/egg", Aseprite::class.java)
        assetsManager.load("sheets/transition", Aseprite::class.java)
        assetsManager.load("sheets/transition2", Aseprite::class.java)
        assetsManager.load("sheets/transition3", Aseprite::class.java)
        assetsManager.load("sheets/wreckage", Aseprite::class.java)
        assetsManager.load("sheets/gate", Aseprite::class.java)

        info { "Loading shaders" }
        assetsManager.load("shaders/transition.frag", ShaderProgram::class.java)
        assetsManager.load("shaders/credits_pixel.frag", ShaderProgram::class.java)
        assetsManager.load("shaders/credits_ghost.frag", ShaderProgram::class.java)
        assetsManager.load("shaders/nop.frag", ShaderProgram::class.java)

        info { "Loading Particles" }
        val particleParameters = ParticleEffectLoader.ParticleEffectParameter()
        particleParameters.imagesDir = Gdx.files.internal("sheets")

        assetsManager.load("sheets/particles", ParticleEffect::class.java, particleParameters)
        assetsManager.load("sheets/plumes", ParticleEffect::class.java, particleParameters)

        info { "Loading sound effects" }
        assetsManager.load("sfx/beat_music.ogg", Music::class.java)
        assetsManager.load("sfx/beat_sfx_0.ogg", Sound::class.java)
        assetsManager.load("sfx/beat_sfx_1.ogg", Sound::class.java)
        assetsManager.load("sfx/beat_sfx_2.ogg", Sound::class.java)
        assetsManager.load("sfx/beat_sfx_3.ogg", Sound::class.java)

        info { "Loading Maps" }
        assetsManager.load("title_screen.tmx", TiledMap::class.java)

        if (Config.music) {
            val music: Music = assetsManager["sfx/beat_intro.ogg"]
            if (!music.isPlaying) {
                music.isLooping = true
                music.volume = 0.5f
                music.play()
            }
        }

    }

    override fun render(delta: Float) {

        if (!assetsLoaded) {
            assetsLoaded = assetsManager.update()
            if (assetsLoaded) {

                info { "All assets fully loaded. Can now skip introduction" }

                debug { "Adding Input Processor. Now listening for touch" }
                Gdx.input.inputProcessor = object : InputAdapter() {
                    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                        BeatTheHighScore.title()
                        return true
                    }
                }

                debug { "Add tap to skip text" }
                engine.entity {
                    val i18n: I18NBundle = assetsManager["i18n/messages"]
                    entity.add(StateComponent())
                            .add(Position(-128f * 0.5f v2 -110f))
                            .add(Size(128f v2 192f))
                            .add(TextRender(halign = Align.center, scale = 1 / 4f))
                            .add(TapToSkip(i18n["intro.tap.to.skip"]))
                }
            }
        }
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.projectionMatrix = viewport.camera.combined
        engine.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }
}