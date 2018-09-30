package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.github.dwursteisen.libgdx.ashley.Direction
import com.github.dwursteisen.libgdx.ashley.get

class FreeChicken(var origin: Vector2, val particle: ParticleEffectPool.PooledEffect) : Component

fun ParticleEffectPool.PooledEffect.setPosition(pos: Vector2) {
    this.setPosition(pos.x, pos.y)
}

class FreeChickenParticleSystem(private val batch: SpriteBatch) : IteratingSystem(all(FreeChicken::class.java).get()) {
    private val chicken: ComponentMapper<FreeChicken> = get()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[chicken].particle.draw(batch, deltaTime)
    }

    override fun update(deltaTime: Float) {
        batch.begin()
        super.update(deltaTime)
        batch.end()
    }
}

class FreeChickenSystem : IteratingSystem(all(FreeChicken::class.java).get()) {

    private val position: ComponentMapper<Position> = get()
    private val chicken: ComponentMapper<FreeChicken> = get()
    private val direction: ComponentMapper<Direction> = get()

    override fun processEntity(entity: Entity, deltaTime: Float) {

        val direction = entity[direction].value
        entity[position].position.add(direction)
        entity[chicken].particle.setPosition(entity[position].position)

        // out of the screen
        if (entity[position].position.dst(entity[chicken].origin) >= 72f) {
            entity[chicken].particle.free()
            engine.removeEntity(entity)
        }
    }

}