package com.github.dwursteisen.beat.credits

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.beat.BeatTheHighScore
import com.github.dwursteisen.beat.game.Config
import com.github.dwursteisen.beat.game.invoke
import com.github.dwursteisen.beat.game.screenHeight
import com.github.dwursteisen.beat.game.screenWidth
import com.github.dwursteisen.beat.options.centerCamera
import ktx.log.info
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.textButton
import ktx.scene2d.verticalGroup

class CreditsScreen(val assets: AssetManager) : ScreenAdapter() {


    private lateinit var viewport: Viewport
    private lateinit var fixedViewport: Viewport
    private lateinit var stage: Stage
    private lateinit var zeMap: OrthogonalTiledMapRenderer
    private lateinit var batch: SpriteBatch

    private lateinit var frameBuffer: FrameBuffer

    private lateinit var shader: ShaderProgram
    private lateinit var alpha: (Float) -> Float

    private lateinit var labelTxt: Label

    override fun show() {
        info { "Clean input processor" }
        Gdx.input.inputProcessor = null

        info { "Loading Title screen" }

        info { "Create Fields objects" }
        batch = SpriteBatch()
        viewport = FitViewport(screenWidth, screenHeight)
        viewport.centerCamera()

        fixedViewport = FitViewport(screenWidth, screenHeight)
        fixedViewport.centerCamera()

        stage = Stage(fixedViewport, batch)
        info { "Load background map" }
        val tmxMap = TmxMapLoader().load("credit.tmx")
        zeMap = OrthogonalTiledMapRenderer(tmxMap)

        info { "Creating scene2D" }
        val root = container {
            setSize(screenWidth, screenHeight)
            setPosition(0f, 0f)
            val i18n: I18NBundle = assets["i18n/messages"]
            align(Align.top)
            padTop(30f)
            verticalGroup {

                label(i18n["credit.text"]) {
                    setSize(screenWidth, screenHeight)
                    setWrap(true)
                    setAlignment(Align.top)
                    setPosition(0f, 0f)
                }
                space(8f)
                textButton(i18n["credit.author"]) {
                    setSize(screenWidth, screenHeight)
                    setPosition(0f, 0f)
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            Gdx.net.openURI("https://twitter.com/dwursteisen")
                        }
                    })
                }
            }
        }

        stage.addActor(root)
        stage.isDebugAll = Config.scene2d
        stage.addListener(
                object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        BeatTheHighScore.title()
                    }
                })
        Gdx.input.inputProcessor = stage

        info { "Configuring Shaders" }

        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, fixedViewport.screenWidth, fixedViewport.screenHeight, false)

        shader = if (Config.shader) {
            if (Config.credit == "pixel") {
                alpha = { a: Float -> 1f - Interpolation.exp10In.invoke(a / 3f) }
                assets["shaders/credits_pixel.frag"]
            } else {
                alpha = { a: Float -> Interpolation.linear.invoke(a / 3f) }
                assets["shaders/credits_ghost.frag"]
            }
        } else {
            alpha = { a: Float -> a }
            assets["shaders/nop.frag"]
        }


        time = 0f // reset
    }

    private var time = 0f
    private var timeColor = 0f

    override fun render(delta: Float) {

        timeColor += delta

        // rendering the buffer
        frameBuffer.begin()
        Gdx.gl.glClearColor(1f, 1f, 1f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        zeMap.setView(fixedViewport.camera as OrthographicCamera)
        zeMap.render()

        stage.act(delta)
        stage.draw()

        frameBuffer.end()

        viewport.apply()
        val region = TextureRegion(frameBuffer.colorBufferTexture)
        region.flip(false, true)

        time = Math.min(time + delta, 3f)

        shader.begin()
        shader.setUniformf("alpha", alpha(time))
        shader.setUniform2fv("res", floatArrayOf(screenWidth, screenHeight), 0, 2)
        shader.end()

        Gdx.gl.glClearColor(91f / 256f, 110f / 256f, 225f / 256f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.shader = shader
        batch.begin()
        batch.projectionMatrix = viewport.camera.combined
        batch.draw(region, 0f, 0f, screenWidth, screenHeight)
        batch.end()
        batch.shader = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        fixedViewport.update(width, height)
    }
}

