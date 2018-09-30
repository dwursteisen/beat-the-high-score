package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.MapObjects
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.beat.BeatTheHighScore
import com.github.dwursteisen.beat.intro.TextRender
import com.github.dwursteisen.beat.intro.TextRenderSystem
import com.github.dwursteisen.libgdx.aseprite.AnimationSlice
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.aseprite.AsepriteJson
import com.github.dwursteisen.libgdx.ashley.*
import com.github.dwursteisen.libgdx.v2
import ktx.ashley.entity
import ktx.log.info
import ktx.scene2d.horizontalGroup
import ktx.scene2d.textButton
import com.badlogic.gdx.utils.Array as GdxArray
import com.github.dwursteisen.libgdx.ashley.Position as AshleyPosition

sealed class ShapeType(val filed: Boolean) {
    object FilledRectangle : ShapeType(true)
    object FilledCircle : ShapeType(true)
    object Rectangle : ShapeType(false)
    object Circle : ShapeType(false)
}

const val ballRadius = 4f
const val playerWidth = 48f
const val playerHeight = 32f
const val hitTime = 0.2f

const val EVENT_BRICK_TOUCHED = 1
const val EVENT_BRICK_IDLE = 2
const val EVENT_BRICK_EXPLODED = 3
const val EVENT_CAMERA_IDLE = 4
const val EVENT_CAMERA_SHAKE = 5

const val EVENT_PLAYER_IDLE = 6
const val EVENT_PLAYER_TOUCH = 7

const val EVENT_WIN = 8
const val EVENT_LOOSE = 9

const val EVENT_UPDATE_GATE = 10

const val screenWidth = 128f
const val screenHeight = 224f

class CameraHolder(val camera: Camera, val amplitude: Vector2 = Vector2()) : Component
class Player(val hitbox: Vector2 = Vector2(), val offsetHitbox: Vector2 = Vector2(), val direction: Vector2 = Vector2(), var win: Boolean = false) : Component
class PlayerTouch : Component
class Ball(val direction: Vector2) : Component
class Brick(var hit: Int = 3, val body: Body) : Component
class Gate(var open: Boolean = true, var openTime: Float = 3f, var closeTime: Float = 1f) : Component
class DeadZone : Component
class Cloud(val origin: Vector2 = Vector2(), val offset: Float = 0f) : Component
class BallCopy(var alpha: Float = 1f) : Component
class MapLayer(val map: OrthogonalTiledMapRenderer) : Component
class Position(var position: Vector2) : Component
class Size(var size: Vector2) : Component
class ShapeToRender(var type: ShapeType, var color: Color) : Component
class Move(val duration: Float = 0f, var delay: Float, val from: Vector2 = Vector2(), val target: Vector2 = Vector2(), val interpolation: Interpolation = Interpolation.linear) : Component
class Hitbox(val size: Vector2, val offset: Vector2) : Component

class AnimatedHitbox(var slices: AnimationSlice = NO_SLICE_ANIMATION) : Component

class StageComponent(val stage: Stage) : Component

class Transition(val duration: Float = 0.5f, val wayIn: Boolean = true, var done: Boolean = false) : Component

class Debugable : Component
class DebugCollision(var hit: Float = 0f) : Component
class ParticleEntity(var particle: ParticleEffectPool.PooledEffect? = null) : Component

val NO_TEXTURE = TextureRegion()
val NO_ANIMATION = Animation<TextureRegion>(0f)
val NO_SLICE_ANIMATION = AnimationSlice(0f)

// --- RENDERING --- //
class Animated(var animation: Animation<TextureRegion> = NO_ANIMATION) : Component

class EntityRender(
        var texture: TextureRegion = NO_TEXTURE,
        val zLevel: Int = 0,
        var enabled: Boolean = true,
        val offset: Vector2 = Vector2(),
        var hFlip: Boolean = false,
        var alpha: Float = 1f) : Component

fun Float.between(min: Float, max: Float): Boolean {
    return this in min..max
}

fun Rectangle.set(position: Vector2, size: Vector2): Rectangle {
    return this.set(position.x, position.y, size.x, size.y)
}

val Double.seconds: Float
    get() = this.toFloat()

val Int.second: Float
    get() = this.toFloat()

class GameScreen(private val assets: AssetManager, var levelName: String = "level0.tmx") : ScreenAdapter() {

    private lateinit var viewport: Viewport
    private lateinit var viewportScreen: Viewport
    private lateinit var shapeBatch: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var zeMap: OrthogonalTiledMapRenderer
    private lateinit var engine: PooledEngine
    private lateinit var eventBus: EventBus

    private lateinit var world: World
    private var hitbox = false

    private lateinit var debugRenderer: Box2DDebugRenderer
    private lateinit var buttons: Stage

    private lateinit var backgroundColor: Color

    override fun show() {

        info { "Load Game screen" }

        val font: BitmapFont = if (Config.customFont) {
            assets["krungthep2.fnt"]
        } else {
            BitmapFont()
        }

        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        font.setUseIntegerPositions(false)

        backgroundColor = Color(91f / 256f, 110f / 256f, 225f / 256f, 1f)

        info { "Clean input processor" }
        Gdx.input.inputProcessor = null

        viewport = ViewportFactory.create(screenWidth, screenHeight)
        viewportScreen = ScreenViewport()


        info { "Create Fields objects" }
        engine = PooledEngine()
        world = World(0 v2 0, true)
        shapeBatch = ShapeRenderer()
        batch = SpriteBatch()

        info { "Create buttons menu" }
        val i18n: I18NBundle = assets["i18n/messages"]
        buttons = Stage(viewport, batch)
        buttons.addActor(horizontalGroup {
            setSize(screenWidth, 16f)
            setPosition(0f, screenHeight - 16f)
            align(Align.topLeft)
            textButton(i18n["game.menu.button"]).addCaptureListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    BeatTheHighScore.title()
                }
            })
        })
        buttons.isDebugAll = Config.scene2d
        Gdx.input.inputProcessor = buttons

        eventBus = EventBus()

        debugRenderer = Box2DDebugRenderer()

        info { "Create particles generators" }
        val hitParticleEffect: ParticleEffect = assets["sheets/particles"]
        hitParticleEffect.start()

        val hitParticle = ParticleEffectPool(hitParticleEffect, 3, 8)

        val featherEffect: ParticleEffect = assets["sheets/plumes"]
        featherEffect.start()

        val featherParticle = ParticleEffectPool(featherEffect, 3, 8)

        info { "Load tile map file: $levelName" }
        val tmxMap = TmxMapLoader().load(levelName)
        zeMap = OrthogonalTiledMapRenderer(tmxMap)
        val layers = zeMap.map.layers.mapIndexed { id, name -> name.name to id }

        val layerForeground = layers.filter { it.first.startsWith("front_") }.map { it.second }
        val layerBackground = layers.map { it.second }.minus(layerForeground)

        info { "Adding systems to the engine" }
        // -- ashley entities -- //
        engine.addSystem(DebugResetCollisionSystem())

        // input
        engine.addSystem(PlayerControlSystem(viewport))

        // game entities
        engine.addSystem(MoveSystem())
        engine.addSystem(BallSystem(eventBus, assets))
        engine.addSystem(BrickSystem(eventBus, world, assets, featherParticle))
        engine.addSystem(PlayerSystem(eventBus, assets))
        engine.addSystem(GateSystem(eventBus, assets))
        engine.addSystem(CloudSystem())
        engine.addSystem(FreeChickenSystem())
        engine.addSystem(BallCopySystem(assets))
        engine.addSystem(AlphaBallCopySystem())

        // render
        engine.addSystem(FirstLayerMapRendersystem(layerBackground.toIntArray()))
        engine.addSystem(AnimationSystem())
        engine.addSystem(SlicesAnimationSystem())
        engine.addSystem(FreeChickenParticleSystem(batch))
        engine.addSystem(RenderSystem(batch))
        engine.addSystem(ParticleImpactSystem(hitParticle, batch))
        engine.addSystem(SecondLayerMapRendersystem(layerForeground.toIntArray()))
        engine.addSystem(CameraSystem(eventBus))
        engine.addSystem(Scene2DSystem())

        // debug
        engine.addSystem(DebugPositionSystem(batch))
        engine.addSystem(DebugShapeSystem(shapeBatch))
        engine.addSystem(DebugDirectionSystem(shapeBatch))

        engine.addSystem(StateSystem())

        engine.addSystem(TransitionSystem(assets, viewportScreen, batch) { e ->
            if (e.getComponent(Transition::class.java).wayIn) {
                val player = engine.getEntitiesFor(all(Player::class.java).get()).first()
                val isWin = player.getComponent(Player::class.java).win
                val delay = if (isWin) 5f else 3f
                if (isWin) {
                    dropChickens()
                }
                runLater({
                    if (isWin) {
                        BeatTheHighScore.nextLevel()
                    } else {
                        BeatTheHighScore.play()
                    }
                }, delay)
            } else {
                engine.removeEntity(e)
            }
        })
        engine.addSystem(object : IteratingSystem(all(CameraHolder::class.java).get()) {
            override fun processEntity(entity: Entity?, deltaTime: Float) {
                viewport.apply()
                batch.projectionMatrix = viewport.camera.combined
            }

        })
        engine.addSystem(TextRenderSystem(batch, font))

        val mapBlock = tmxMap.layers["bricks"]?.objects ?: MapObjects()

        info { "Starting to create level briks (${mapBlock.count} elements)" }
        val startOffset = screenHeight * 0.5f
        // --- BRICKS --- //
        val blocks = mapBlock.map {
            it as RectangleMapObject
            val props = BrickProperties(it.properties)

            val size = it.rectangle.width v2 it.rectangle.height
            val pos = if (props.hit > 0) {
                props.x v2 (props.y + startOffset)
            } else {
                props.x v2 props.y
            }

            val entity = engine.createEntity()
                    .add(Brick(hit = props.hit, body = createBox2DRect(pos.cpy().sub(-it.rectangle.width * 0.5f, startOffset - it.rectangle.height * 0.5f), size.cpy().sub(it.rectangle.width * 0.5f, it.rectangle.height * 0.5f))))
                    .add(Debugable())
                    .add(DebugCollision())
                    .add(Position(pos))
                    .add(Size(size))
                    .add(StateComponent())
                    .add(Hitbox(size.cpy(), Vector2.Zero.cpy()))
                    .add(ShapeToRender(type = ShapeType.Rectangle, color = Color.CHARTREUSE))

            if (props.hit > 0) {
                entity.add(Animated())
                        .add(EntityRender())
            } else if (props.hit == -2) {
                entity.add(Gate())
                        .add(AnimatedHitbox())
                        .add(Animated())
                        .add(EntityRender(zLevel = -1))
            }

            entity

        }.sortedWith(compareBy({ it.getComponent(Position::class.java).position.y }, { it.getComponent(Position::class.java).position.x }))


        val breakableBricks = blocks.filter { it.getComponent(Brick::class.java).hit > 0 }
        breakableBricks
                .forEachIndexed { index, entity ->
                    val delay = index.toFloat() * 0.1f
                    entity.add(Move(
                            duration = 0.7f,
                            delay = delay,
                            target = entity.getComponent(Position::class.java).position.cpy().sub(0f, startOffset),
                            from = entity.getComponent(Position::class.java).position.cpy(),
                            interpolation = Interpolation.pow2Out
                    ))

                }

        blocks.forEach { engine.addEntity(it) }

        val deadZones = tmxMap.layers["deadZone"]?.objects ?: MapObjects()
        info { "Add ${deadZones.count} dead zone(s) to the game" }
        deadZones.map {
            it as RectangleMapObject
            val props = TiledProperties(it.properties)

            val size = it.rectangle.width v2 it.rectangle.height
            val pos = props.x v2 props.y

            engine.entity {
                entity.add(Debugable())
                        .add(DebugCollision())
                        .add(Position(pos))
                        .add(Size(size))
                        .add(DeadZone())
                        .add(ShapeToRender(type = ShapeType.Rectangle, color = Color.RED))
            }

        }


        info { "Add others entities to the game" }
        // --- BALL --- //
        engine.entity {

            val ball: Aseprite = assets["sheets/egg"]
            entity.add(Ball(direction = 0 v2 0))
                    .add(Debugable())
                    .add(Position(((screenWidth - ballRadius) * 0.5f) v2 25 + playerHeight))
                    .add(Size(8 v2 9))
                    .add(ShapeToRender(type = ShapeType.Rectangle, color = Color.WHITE))
                    .add(EntityRender(ball.frame(0)))
                    .add(StateComponent())
                    .add(Rotation(origin = 4 v2 4))
        }

        (1..4).forEach {
            engine.entity {
                val ball: Aseprite = assets["sheets/egg"]
                entity.add(BallCopy(alpha = 1f / it))
                        .add(Position(-100 v2 -100)) // hide it by default
                        .add(Size(8 v2 9))
                        .add(EntityRender(ball.frame(0), zLevel = -1))
                        .add(StateComponent())
                        .add(Rotation(origin = 4 v2 4))
            }
        }

        // --- PLAYER --- //
        val playerJson: AsepriteJson = assets["sheets/chicken.json"]
        val bounds = playerJson.slices("hitbox").keys[0].bounds

        engine.entity {
            entity.add(Player(hitbox = bounds.w v2 bounds.h, offsetHitbox = bounds.x v2 (playerHeight - bounds.y) - bounds.h))
                    .add(Debugable())
                    .add(DebugCollision())
                    .add(EntityRender())
                    .add(Animated())
                    .add(Position((screenWidth - playerWidth) * 0.5f v2 25))
                    .add(Size(playerWidth v2 playerHeight))
                    .add(ShapeToRender(type = ShapeType.Rectangle, color = Color.BLUE))
                    .add(StateComponent())
        }

        // --- FOX --- //
        val fox: Aseprite = assets["sheets/renard"]
        engine.entity {
            entity.add(Debugable())
                    .add(EntityRender())
                    .add(Animated(fox["idle"]))
                    .add(Position((screenWidth - 64) v2 (screenHeight - 32)))
                    .add(Size(64 v2 32))
                    .add(ShapeToRender(type = ShapeType.Rectangle, color = Color.BLUE))
                    .add(StateComponent())
        }

        // --- CAMERA --- //
        engine.entity {
            entity.add(CameraHolder(viewport.camera))
                    .add(Position(screenWidth * 0.5f v2 screenHeight * 0.5f))
                    .add(StateComponent())
        }

        // -- MAP --- //
        engine.entity {
            entity.add(MapLayer(zeMap))
        }

        // --- SCENE 2D --- //
        engine.entity { entity.add(StageComponent(buttons)) }

        // --- CLOUDS --- //
        val clouds: Aseprite = assets["sheets/clouds"]
        val cloudsName = listOf("cloud1", "cloud2", "cloud3")
        val cloudsPosition = listOf(0 v2 screenWidth * 0.5f, 80 v2 screenHeight * 0.5)

        cloudsPosition.forEachIndexed { index, it ->

            engine.entity {
                val texture = clouds[cloudsName.pickOne()].getKeyFrame(0f)
                entity.add(Debugable())
                        .add(Cloud(origin = it, offset = index.toFloat() * 2.5f))
                        .add(Position(it.cpy()))
                        .add(Size(64 v2 64))
                        .add(ShapeToRender(type = ShapeType.Rectangle, color = Color.BLACK))
                        .add(EntityRender(texture = texture, zLevel = -2))
                        .add(StateComponent())
            }
        }

        engine.entity {
            entity.add(Transition(wayIn = false))
                    .add(StateComponent())

        }

        eventBus.register(object : EventListener {
            override fun onEvent(event: Event, eventData: EventData) {
                info { "Player just loose the game!" }
                // destroy the ball
                val balls = engine.getEntitiesFor(Family.all(Ball::class.java).get())
                engine.removeAll(balls)


                val txt = i18n["game.loose"]
                // display loose message
                for (x in -2..2 step 4) {
                    for (y in -2..2 step 4) {
                        engine.entity {
                            entity.add(TextRender(txt, color = Color.BLACK, scale = 1f, halign = Align.center))
                                    .add(Position((0 + x) v2 (64 + y)))
                                    .add(Size(screenWidth v2 90))
                        }

                    }
                }

                engine.entity {
                    entity.add(TextRender(txt, color = Color.WHITE, scale = 1f, halign = Align.center))
                            .add(Position(0 v2 64))
                            .add(Size(screenWidth v2 90))
                }

                // add transition to same level
                engine.entity {
                    entity.add(Transition(wayIn = true))
                            .add(StateComponent())
                }
            }

        }, EVENT_LOOSE)


        eventBus.register(object : EventListener {
            override fun onEvent(event: Event, eventData: EventData) {
                info { "Player just win the game!" }

                engine.getEntitiesFor(all(Player::class.java).get()).forEach {
                    it.getComponent(Player::class.java).win = true
                }
                // destroy the ball
                val balls = engine.getEntitiesFor(Family.all(Ball::class.java).get())
                engine.removeAll(balls)


                val txt = i18n["game.win"]
                // display loose message
                for (x in -2..2 step 4) {
                    for (y in -2..2 step 4) {
                        engine.entity {
                            entity.add(TextRender(txt, color = Color.BLACK, scale = 0.7f, halign = Align.center))
                                    .add(Position((0 + x) v2 (64 + y)))
                                    .add(Size(screenWidth v2 90))
                        }

                    }
                }

                engine.entity {
                    entity.add(TextRender(txt, color = Color.WHITE, scale = 0.7f, halign = Align.center))
                            .add(Position(0 v2 64))
                            .add(Size(screenWidth v2 90))
                }

                // add transition to same level
                engine.entity {
                    entity.add(Transition(wayIn = true))
                            .add(StateComponent())
                }
            }

        }, EVENT_WIN)

        info { "Add BOX2d information" }
        // -- box2d -- //
        hitbox = Config.hitbox
        addWalls()

        if (Config.music) {
            val music: Music = assets["sfx/beat_music.ogg"]
            if (!music.isPlaying) {
                music.isLooping = true
                music.volume = 0.2f
                music.play()
            }
        }
        resume()
    }

    fun createBox2DRect(position: Vector2, size: Vector2): Body {

        val groundBodyDef = BodyDef()
        groundBodyDef.position.set(position)
        groundBodyDef.type = BodyDef.BodyType.StaticBody
        val groundBody = world.createBody(groundBodyDef)

        val groundBox = PolygonShape()
        groundBox.setAsBox(size.x, size.y)

        // 2. Create a FixtureDef, as usual.
        val fd = FixtureDef()
        fd.density = 1f
        fd.friction = 0.5f
        fd.restitution = 0.3f
        fd.shape = groundBox
        // Create a fixture from our polygon shape and add it to our ground body
        groundBody.createFixture(fd)
        // Clean up after ourselves
        groundBox.dispose()

        return groundBody
    }

    private fun addWalls() {
        // TODO: create it from the map -> create bricks with infine lives
        createBox2DRect(screenWidth * 0.5f v2 0, screenWidth * 0.5f v2 10)
        createBox2DRect(-5 v2 screenHeight * 0.5, 5 v2 screenHeight * 0.5)
        createBox2DRect((screenWidth + 5) v2 screenHeight * 0.5, 5 v2 screenHeight * 0.5)
    }

    override fun render(delta: Float) {

        viewport.apply()

        world.step(1 / 45f, 6, 2)

        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        shapeBatch.projectionMatrix = viewport.camera.combined
        batch.projectionMatrix = viewport.camera.combined

        eventBus.update(delta)

        zeMap.setView(viewport.camera as OrthographicCamera)

        engine.update(Math.min(delta, 1 / 60f))

        val bodies = GdxArray<Body>()
        world.getBodies(bodies)
        bodies.forEach {
            val et = it.userData as? Entity
            et?.getComponent(Position::class.java)?.position?.set(it.position)
            et?.getComponent(Rotation::class.java)?.degree = it.angle * MathUtils.radiansToDegrees
        }
        if (hitbox) {
            debugRenderer.render(world, viewport.camera.combined)
        }
    }

    private fun dropChickens() {
        // remove everything
        engine.entities.filter { it.getComponent(TextRender::class.java) == null }
                .forEach { engine.removeEntity(it) }

        val gdxArray = GdxArray<Body>()
        world.getBodies(gdxArray)
        gdxArray.forEach {
            world.destroyBody(it)
        }

        backgroundColor = Color(34f / 256f, 32f / 256f, 52f / 256f, 1f)
        val chickenSize = 16 v2 16

        for (i in 0..30) {
            val headsOrTail = MathUtils.randomBoolean()
            val position = if (headsOrTail) {
                screenWidth * 0.5f v2 -16
            } else {
                screenWidth * 0.5f v2 screenHeight + 16
            }

            val impulse = if (headsOrTail) {
                MathUtils.random(-10f, 10f) v2 280f
            } else {
                MathUtils.random(-10f, 10f) v2 -280f
            }

            runLater({
                val et = engine.entity {
                    val spriteData: Aseprite = assets["sheets/free_chicken"]
                    val chickenAnimation = spriteData["fly"]
                    entity.add(Position(position = position))
                            .add(Size(chickenSize))
                            .add(StateComponent())
                            .add(EntityRender())
                            .add(Debugable())
                            .add(Rotation())
                            .add(Animated(animation = chickenAnimation))
                }

                val bodyDef = BodyDef()
                // Set its world position
                bodyDef.position.set(position.x, position.y)
                bodyDef.type = BodyDef.BodyType.DynamicBody
                bodyDef.angle = 0f
                //  bodyDef.gravityScale = gravityScale
                // Apply a force of 1 meter per second on the X-axis at pos.x/pos.y of the body slowly moving it right
                // Create a body from the defintion and add it to the world
                val body = world.createBody(bodyDef)
                body.applyLinearImpulse(impulse, position, true)
                body.linearDamping = 0.5f
                // Create a polygon shape
                val groundBox = CircleShape()
                // Set the polygon shape as a box which is twice the size of our view port and 20 high
                // (setAsBox takes half-width and half-height as arguments)
                groundBox.radius = 8f

                // 2. Create a FixtureDef, as usual.
                val fd = FixtureDef()
                fd.density = 1f
                fd.friction = 2f
                //fd.restitution = 0.3f
                fd.shape = groundBox
                // Create a fixture from our polygon shape and add it to our ground body
                body.createFixture(fd)

                body.userData = et
                // Clean up after ourselves
                groundBox.dispose()

            }, i * 0.10f)
        }

    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        viewportScreen.update(width, height)
    }

    override fun resume() {
        info { "Game Resumed...Configure all systems according to the current configuration: $Config" }
        engine.getSystem(RenderSystem::class.java).setProcessing(Config.sprites)
        engine.getSystem(DebugShapeSystem::class.java).setProcessing(Config.hitbox)
        engine.getSystem(DebugPositionSystem::class.java).setProcessing(Config.position)
        engine.getSystem(DebugDirectionSystem::class.java).setProcessing(Config.direction)
        engine.getSystem(ParticleImpactSystem::class.java).enabled = Config.particles
        engine.getSystem(FreeChickenParticleSystem::class.java).setProcessing(Config.particles)
        engine.getSystem(BrickSystem::class.java).enabled = Config.box2d
        engine.getSystem(TransitionSystem::class.java).enabled = Config.shader
    }
}
