package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.get

class MoveSystem : IteratingSystem(all(Move::class.java, Position::class.java, StateComponent::class.java).get()) {
    private val move: ComponentMapper<Move> = get()
    private val position: ComponentMapper<Position> = get()
    private val state: ComponentMapper<StateComponent> = get()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (entity[move].delay > 0f) {
            entity[move].delay -= deltaTime
            if (entity[move].delay < 0) {
                entity[state].time = 0f
                move(entity)
            }
        } else {
            move(entity)
        }
    }

    private fun move(entity: Entity) {
        val percent = Math.min(entity[state].time / entity[move].duration, 1f)
        val result = entity[move].interpolation.invoke(entity[move].from.y, entity[move].target.y, percent)
        entity[position].position.y = result

        if (percent >= 1f) { // move finished
            entity.remove(Move::class.java)
        }
    }

}