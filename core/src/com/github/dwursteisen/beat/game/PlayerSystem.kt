package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.math.MathUtils
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.EntityState
import com.github.dwursteisen.libgdx.ashley.EventBus
import com.github.dwursteisen.libgdx.ashley.EventData
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.StateMachineSystem
import com.github.dwursteisen.libgdx.ashley.get

fun <T> List<T>.pickOne(): T {
    val index = MathUtils.random(this.size - 1)
    return this.elementAt(index)
}

class PlayerSystem(eventBus: EventBus, val assets: AssetManager) : StateMachineSystem(eventBus, all(Player::class.java).get()) {
    private val animation: ComponentMapper<Animated> = get()
    private val state: ComponentMapper<StateComponent> = get()

    private val idles = listOf("idle", "idle3")

    override fun describeMachine() {
        val IDLE = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                val chicken: Aseprite = assets["sheets/chicken"]
                val anim = if (eventData.event == EVENT_PLAYER_TOUCH) {
                    chicken["bounce"]
                } else {
                    // chose an random animation
                    chicken[idles.pickOne()]
                }

                entity[animation].animation = anim
                entity[state].time = 0f
            }

            override fun update(entity: Entity, machine: StateMachineSystem, delta: Float) {
                if (entity[animation].animation.isAnimationFinished(entity[state].time)) {
                    machine.eventBus.emit(EVENT_PLAYER_IDLE, entity)
                }
            }
        }

        val MOVE = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                // chose an random animation
                val chicken: Aseprite = assets["sheets/chicken"]

                val anim = if (eventData.event == EVENT_PLAYER_TOUCH) {
                    chicken["bounce"]
                } else {
                    chicken["idle2"]
                }

                entity[animation].animation = anim
                entity[state].time = 0f
            }

            override fun update(entity: Entity, machine: StateMachineSystem, delta: Float) {
                if (entity[animation].animation.isAnimationFinished(entity[state].time)) {
                    machine.eventBus.emit(EVENT_PLAYER_IDLE, entity)
                }
            }
        }

        startWith(IDLE)
        onState(IDLE).on(EVENT_PLAYER_IDLE, EVENT_PLAYER_TOUCH) { entity, event ->
            go(IDLE, entity, event)
        }

        onState(IDLE).on(EVENT_KEY) { entity, event ->
            go(MOVE, entity, event)
        }

        onState(MOVE).on(EVENT_KEY_UP) { entity, event ->
            go(IDLE, entity, event)
        }

        onState(IDLE).on(EVENT_TOUCHED) { entity, event ->
            go(MOVE, entity, event)
        }

        onState(MOVE).on(EVENT_SLIDE) { entity, event ->
            go(IDLE, entity, event)
        }

        onState(MOVE).on(EVENT_PLAYER_TOUCH) { entity, event ->
            go(MOVE, entity, event)
        }
    }
}
