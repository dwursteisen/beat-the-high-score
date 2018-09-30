package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.gdx.assets.AssetManager
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.*
import ktx.ashley.has

class GateSystem(eventBus: EventBus, assets: AssetManager) : StateMachineSystem(eventBus, all(Gate::class.java).get()) {

    private val gate: ComponentMapper<Gate> = get()
    private val animation: ComponentMapper<Animated> = get()
    private val animatedHitbox: ComponentMapper<AnimatedHitbox> = get()
    private val state: ComponentMapper<StateComponent> = get()

    private val sprData: Aseprite = assets["sheets/gate"]

    override fun describeMachine() {

        val OPEN = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                entity[state].time = 0f
                entity[animation].animation = sprData["open_nr"]
                entity[animatedHitbox].slices = sprData.animatedSlices("gate")["open_nr"]
                machine.eventBus.emitLater(entity[gate].openTime, EVENT_UPDATE_GATE, entity)
            }
        }

        val CLOSED = object : EntityState() {
            override fun enter(entity: Entity, machine: StateMachineSystem, eventData: EventData) {
                entity[state].time = 0f
                entity[animation].animation = sprData["close_nr"]
                entity[animatedHitbox].slices = sprData.animatedSlices("gate")["close_nr"]

                machine.eventBus.emitLater(entity[gate].closeTime, EVENT_UPDATE_GATE, entity)
            }
        }

        startWith { entity, event ->
            if (!entity.has(gate)) {
                return@startWith
            }
            if (entity[gate].open) {
                go(OPEN, entity, event)
            } else {
                go(CLOSED, entity, event)
            }
        }

        onState(OPEN).on(EVENT_UPDATE_GATE) { entity, event ->
            go(CLOSED, entity, event)
        }


        onState(CLOSED).on(EVENT_UPDATE_GATE) { entity, event ->
            go(OPEN, entity, event)
        }

    }

}