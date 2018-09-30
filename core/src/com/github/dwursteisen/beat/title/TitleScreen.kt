package com.github.dwursteisen.beat.title

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.beat.BeatTheHighScore
import com.github.dwursteisen.beat.game.*
import com.github.dwursteisen.beat.options.centerCamera
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.StateSystem
import com.github.dwursteisen.libgdx.ashley.get
import com.github.dwursteisen.libgdx.v2
import ktx.ashley.entity
import ktx.log.info
import ktx.scene2d.KVerticalGroup
import ktx.scene2d.textButton
import ktx.scene2d.verticalGroup

class Title(val target: Vector2) : Component
class TitleSystem : IteratingSystem(all(Title::class.java).get()) {
    private val duration = 1f
    private val state: ComponentMapper<StateComponent> = get()
    private val render: ComponentMapper<EntityRender> = get()
    private val size: ComponentMapper<Size> = get()
    private val position: ComponentMapper<Position> = get()
    private val title: ComponentMapper<Title> = get()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val alpha = Interpolation.bounceOut.invoke(Math.min(duration, entity[state].time / duration))
        val w = entity[render].texture.regionWidth * alpha
        val h = entity[render].texture.regionHeight * alpha

        entity[size].size.set(w, h)

        val x = entity[title].target.x - w * 0.5f
        val y = entity[title].target.y - h * 0.5f
        entity[position].position.set(x, y)
    }
}

class TitleScreen(private val assetManager: AssetManager) : ScreenAdapter() {

    private lateinit var viewport: Viewport
    private lateinit var viewportFullScreen: Viewport
    private lateinit var batch: SpriteBatch
    private lateinit var zeMap: OrthogonalTiledMapRenderer

    private lateinit var engine: PooledEngine

    private lateinit var stage: Stage

    private var transition: Boolean = false

    override fun show() {
        info { "Clean input processor" }
        Gdx.input.inputProcessor = null

        info { "Loading Title screen" }

        info { "Create Fields objects" }
        batch = SpriteBatch()
        viewport = FitViewport(screenWidth, screenHeight)
        viewport.centerCamera()

        viewportFullScreen = ScreenViewport()
        viewportFullScreen.centerCamera()

        stage = Stage(viewport, batch)
        info { "Load background map" }
        val tmxMap: TiledMap = assetManager["title_screen.tmx"]
        zeMap = OrthogonalTiledMapRenderer(tmxMap)

        info { "Configure engine" }
        engine = PooledEngine()
        engine.addSystem(TitleSystem())
        engine.addSystem(StateSystem())
        engine.addSystem(RenderSystem(batch))
        engine.addSystem(TransitionSystem(assetManager, viewportFullScreen, batch) {
            BeatTheHighScore.play()
            Gdx.gl.glClearColor(34.0f / 256f, 32f / 256f, 52f / 256f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        })

        val txt: Aseprite = assetManager["sheets/title_screen"]
        val logo = txt.frame(0)
        engine.entity {

            entity.add(StateComponent())
                    .add(Title(128 * 0.5 v2 screenHeight - logo.regionHeight * 0.5f - 5f))
                    .add(Position(0 v2 0))
                    .add(Size(0 v2 0))
                    .add(EntityRender(logo))
        }
        transition = false

        val i18n: I18NBundle = assetManager["i18n/messages"]
        stage.addActor(verticalGroup {
            setSize(screenWidth, 40f)
            align(Align.center)
            setPosition(0f, screenHeight - logo.regionHeight - 40f)

            space(4f)
            addButton(i18n["game.menu.start"], 0.7f) { BeatTheHighScore.play() }
            addButton(i18n["game.menu.options"], 0.9f) { BeatTheHighScore.options() }

        })
        Gdx.input.inputProcessor = stage
        stage.isDebugAll = Config.scene2d

        val music: Music = assetManager["sfx/beat_music.ogg"]
        music.stop()

        val introMusic: Music = assetManager["sfx/beat_intro.ogg"]
        introMusic.stop()

        resume()
    }

    private fun KVerticalGroup.addButton(name: String, delay: Float, onClick: () -> Unit) {
        textButton(name) {
            isTransform = true

            setSize(64f, 24f)
            setScale(0f)
            setOrigin(Align.center)

            val delayAction = DelayAction(delay).apply {
                action = ScaleToAction().apply {
                    setScale(1f)
                    duration = 0.5f
                    interpolation = Interpolation.bounceOut
                }
            }
            addAction(delayAction)
            addCaptureListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    onClick()
                }
            })
        }
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            engine.entity {
                entity.add(StateComponent())
                        .add(Transition(wayIn = true))
            }
        }

        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        Gdx.gl.glClearColor(91f / 256f, 110f / 256f, 225f / 256f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        zeMap.setView(viewport.camera as OrthographicCamera)
        zeMap.render()

        stage.act()
        stage.draw()

        engine.update(Math.min(delta, 1 / 60f))
    }

    override fun resume() {
        engine.getSystem(TransitionSystem::class.java).enabled = Config.shader
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        viewportFullScreen.update(width, height)
    }
}
