package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.github.dwursteisen.libgdx.ashley.get

class ParticleImpactSystem(private val pool: ParticleEffectPool, private val batch: SpriteBatch, var enabled: Boolean = true) : IteratingSystem(Family.all(ParticleEntity::class.java).get()) {

    private val particle: ComponentMapper<ParticleEntity> = get()
    private val position: ComponentMapper<Position> = get()

    fun ParticleEffect.setPosition(v: Vector2) {
        this.setPosition(v.x, v.y)
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {

        val p = entity[particle].particle ?: pool.obtain()

        entity[particle].particle = p

        p.setPosition(entity[position].position)

        if (enabled) {
            p.draw(batch, deltaTime)
        }

        if (p.isComplete) {
            pool.free(p)
            entity[particle].particle = null
            engine.removeEntity(entity)
        }
    }

    override fun update(deltaTime: Float) {
        batch.begin()
        super.update(deltaTime)
        batch.end()
    }
}