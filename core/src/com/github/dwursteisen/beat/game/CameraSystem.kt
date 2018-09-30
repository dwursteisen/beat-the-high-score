package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.MathUtils
import com.github.dwursteisen.libgdx.ashley.*

class CameraSystem(eventBus: EventBus) : StateMachineSystem(eventBus, Family.all(CameraHolder::class.java).get()) {

    private val cameraHolder: ComponentMapper<CameraHolder> = get()
    private val position: ComponentMapper<Position> = get()
    private val state: ComponentMapper<StateComponent> = get()

    private val effectDuration = 0.3.seconds

    override fun describeMachine() {
        val IDLE = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                val pos = entity[position].position
                entity[cameraHolder].camera.position.set(pos.x, pos.y, 0f)
                entity[cameraHolder].camera.update()
            }

        }

        val SHAKING = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                entity[state].time = 0.second
                machine.eventBus.emitLater(effectDuration, EVENT_CAMERA_IDLE, entity)
                entity[cameraHolder].amplitude.set(-1f + MathUtils.random(2.0f), -1f + MathUtils.random(2.0f))
            }

            override fun update(entity: Entity, machine: StateMachineSystem, delta: Float) {
                val time = entity[state].time

                val x = entity[cameraHolder].amplitude.x * MathUtils.sin(MathUtils.PI2 * (2 * time / effectDuration))
                val y = entity[cameraHolder].amplitude.y * MathUtils.sin(MathUtils.PI2 * (2 * time / effectDuration))

                entity[cameraHolder].camera.translate(x, y, 0f)
                entity[cameraHolder].camera.update()
            }
        }

        startWith(IDLE)
        onState(IDLE).on(EVENT_CAMERA_SHAKE) { entity, event ->
            go(SHAKING, entity, event)
        }

        onState(SHAKING).on(EVENT_CAMERA_IDLE) { entity, event ->
            go(IDLE, entity, event)
        }
    }

}