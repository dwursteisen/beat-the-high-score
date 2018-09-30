package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.github.dwursteisen.libgdx.ashley.get

class Scene2DSystem : IteratingSystem(Family.all(StageComponent::class.java).get()) {
    private val stage: ComponentMapper<StageComponent> = get()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val stageTo = entity[stage].stage
        stageTo.act(deltaTime)
        stageTo.draw()
    }
}