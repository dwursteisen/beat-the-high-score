package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.*
import com.github.dwursteisen.libgdx.v2
import ktx.ashley.entity
import ktx.ashley.hasNot

class BrickSystem(eventBus: EventBus, private val world: World, val assets: AssetManager, val feather: ParticleEffectPool, var enabled: Boolean = true) : StateMachineSystem(eventBus,
        Family.all(Brick::class.java, Animated::class.java)
                .exclude(Gate::class.java)
                .get()
) {

    private val position: ComponentMapper<Position> = get()
    private val brick: ComponentMapper<Brick> = get()
    private val collision: ComponentMapper<DebugCollision> = get()
    private val state: ComponentMapper<StateComponent> = get()
    private val animation: ComponentMapper<Animated> = get()

    private val tmp = Vector2()

    private val wreckageOrigin = 4 v2 4

    override fun describeMachine() {
        val IDLE = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {

                entity[state].time = MathUtils.random(0.5f)
                if (entity.hasNot(animation)) {
                    return
                }

                val spriteData: Aseprite = assets["sheets/brick"]
                val anim = if (entity[brick].hit != 1) {
                    spriteData["idle"]
                } else {
                    spriteData["idle2"]
                }
                entity[animation].animation = anim
            }
        }

        val TOUCHED = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                entity[collision].hit = hitTime
                entity[brick].hit--

                val cpy = machine.eventBus.createEventData()
                cpy.body = eventData.body
                if (entity[brick].hit == 0) {
                    machine.eventBus.emit(EVENT_BRICK_EXPLODED, entity, cpy)
                } else {
                    machine.eventBus.emitLater(hitTime, EVENT_BRICK_IDLE, entity)
                }

                entity[state].time = 0f
                if (entity.hasNot(animation)) {
                    return
                }
                val spriteData: Aseprite = assets["sheets/brick"]
                entity[animation].animation = spriteData["explode_fill"]

            }
        }

        val EXPLODED = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                val direction = eventData.body as Vector2

                val radius = 1.5f + MathUtils.random(2.5f)
                world.destroyBody(entity[brick].body)
                addWreckage(radius,
                        entity[position].position,
                        direction
                )

                addFreeChicken(entity[position].position)

                entity.remove(Brick::class.java)

                val spriteData: Aseprite = assets["sheets/brick"]
                entity[animation].animation = spriteData["explode_nr"]
                entity[state].time = 0f

                val remainingBricks = engine.getEntitiesFor(family).count()
                if (remainingBricks == 0) {
                    machine.eventBus.emit(EVENT_WIN)
                }
            }
        }

        startWith(IDLE)
        onState(IDLE).on(EVENT_BRICK_TOUCHED) { entity, event ->
            go(TOUCHED, entity, event)
        }

        onState(TOUCHED).on(EVENT_BRICK_IDLE) { entity, _ ->
            go(IDLE, entity)
        }
        onState(TOUCHED).on(EVENT_BRICK_EXPLODED) { entity, event ->
            go(EXPLODED, entity, event)
        }
    }

    private val toLeft = -128 * 0.5f v2 0
    private val toRight = 128 * 0.5f v2 0
    private val chickenSize = 16 v2 16
    private val wreckageSize = 8 v2 8

    private fun addFreeChicken(position: Vector2) {
        val direction = if (position.x < 128 * 0.5f) {
            toLeft to false
        } else {
            toRight to true
        }

        val spriteData: Aseprite = assets["sheets/free_chicken"]
        val chickenAnimation = spriteData["fly"]
        engine.entity {
            val dir = direction.first.cpy()
                    .nor()
                    .scl(0.5f)
                    .rotateRad(-1.0f + MathUtils.random(2.0f))

            val particle = feather.obtain()
            if (direction.second) {
                particle.emitters.first().velocity.highMax *= -1
                particle.emitters.first().velocity.highMin *= -1
            }

            entity.add(FreeChicken(position.cpy(), particle))
                    .add(Position(position = position.cpy()))
                    .add(Size(chickenSize))
                    .add(StateComponent())
                    .add(EntityRender(hFlip = direction.second))
                    .add(Debugable())
                    .add(Direction(value = dir))
                    .add(Animated(animation = chickenAnimation))
        }
    }

    private fun addWreckage(radius: Float, position: Vector2, impulse: Vector2) {

        if (!enabled) {
            return
        }

        tmp.set(impulse)
                .rotate(MathUtils.random(-20f, 20f))
                .nor()
                .scl(40f)

        // -- box2d entities -- //
        // Create our body definition
        val bodyDef = BodyDef()
        // Set its world position
        bodyDef.position.set(position.x, position.y)
        bodyDef.type = BodyDef.BodyType.DynamicBody
        bodyDef.angle = tmp.angle()
        //  bodyDef.gravityScale = gravityScale
        // Apply a force of 1 meter per second on the X-axis at pos.x/pos.y of the body slowly moving it right
        // Create a body from the defintion and add it to the world
        val body = world.createBody(bodyDef)
        body.applyLinearImpulse(tmp, position, true)
        body.linearDamping = 0.5f
        // Create a polygon shape
        val groundBox = CircleShape()
        // Set the polygon shape as a box which is twice the size of our view port and 20 high
        // (setAsBox takes half-width and half-height as arguments)
        groundBox.radius = radius

        // 2. Create a FixtureDef, as usual.
        val fd = FixtureDef()
        fd.density = 1f
        fd.friction = 2f
        //fd.restitution = 0.3f
        fd.shape = groundBox
        // Create a fixture from our polygon shape and add it to our ground body
        body.createFixture(fd)

        val sprData: Aseprite = assets["sheets/wreckage"]
        val nbFrame = sprData.json.frames.size
        val randomFrame = sprData.frame(MathUtils.random(nbFrame - 1))

        val et = engine.entity {
            EngineEntity@ entity.add(Position(position.cpy()))
                    .add(Size(wreckageSize))
                    .add(StateComponent())
                    .add(EntityRender(texture = randomFrame))
                    .add(Rotation(origin = wreckageOrigin))
        }
        body.userData = et
        // Clean up after ourselves
        groundBox.dispose()
    }
}