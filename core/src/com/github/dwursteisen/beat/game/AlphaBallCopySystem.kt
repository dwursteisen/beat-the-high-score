package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.github.dwursteisen.libgdx.ashley.get

class AlphaBallCopySystem : IteratingSystem(Family.all(BallCopy::class.java).get()) {
    private val ballCopy: ComponentMapper<BallCopy> = get()
    private val render: ComponentMapper<EntityRender> = get()
    private val size: ComponentMapper<Size> = get()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[ballCopy].alpha -= deltaTime
        entity[render].alpha = entity[ballCopy].alpha
        if (entity[ballCopy].alpha < 0.0) {
            engine.removeEntity(entity)
        }

        entity[size].size.scl(0.95f)

    }

}