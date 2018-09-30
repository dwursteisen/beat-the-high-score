package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.dwursteisen.libgdx.ashley.get

class DebugPositionSystem(private val batch: SpriteBatch) : IteratingSystem(Family.all(Debugable::class.java, Position::class.java).get()) {

    private val font = BitmapFont().apply {
        this.data.setScale(0.35f)
    }
    private val position: ComponentMapper<Position> = get()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val x = entity[position].position.x
        val y = entity[position].position.y
        font.draw(batch, "%.0f|%.0f".format(x, y), x, y)
    }

    override fun update(deltaTime: Float) {
        batch.begin()
        super.update(deltaTime)
        batch.end()
    }
}