package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.github.dwursteisen.libgdx.ashley.get
import com.github.dwursteisen.libgdx.ashley.getNullable
import ktx.ashley.has
import ktx.graphics.circle
import ktx.graphics.rect

class DebugShapeSystem(private val batch: ShapeRenderer) : IteratingSystem(Family.all(Debugable::class.java, Position::class.java, ShapeToRender::class.java, Size::class.java).get()) {

    private val position: ComponentMapper<Position> = get()
    private val size: ComponentMapper<Size> = get()
    private val shape: ComponentMapper<ShapeToRender> = get()
    private val collision: ComponentMapper<DebugCollision> = get()
    private val player: ComponentMapper<Player> = get()
    private val hitbox: ComponentMapper<Hitbox> = get()

    private val tmp = Vector2()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val shape = entity[shape]
        batch.color = if (entity.getNullable(collision)?.hit ?: 0f > 0f) {
            Color.RED
        } else {
            shape.color
        }

        if (entity.has(player)) {
            tmp.set(entity[position].position)
                    .add(entity[player].offsetHitbox)

            draw(shape, tmp, entity[player].hitbox)
        }

        if (entity.has(hitbox)) {

            val (x, y) = entity[position].position
            val (offx, offy) = entity[hitbox].offset

            tmp.set(x + offx, y + offy)
            if (entity[hitbox].size.x <= 0.1f) {
                batch.color = Color.WHITE
            }
            draw(shape, tmp, entity[hitbox].size)
        }
        draw(shape, entity[position].position, entity[size].size)
    }

    private fun draw(shape: ShapeToRender, pos: Vector2, siz: Vector2) {
        when (shape.type) {
            is ShapeType.FilledCircle -> batch.circle(pos, siz.x)
            is ShapeType.Circle -> batch.circle(pos, siz.x)
            is ShapeType.FilledRectangle -> batch.rect(pos, siz)
            is ShapeType.Rectangle -> batch.rect(pos, siz)
        }
    }

    override fun update(deltaTime: Float) {

        val groupBy = super.getEntities().groupBy { it[shape].type.filed }
        batch.begin(ShapeRenderer.ShapeType.Filled)
        groupBy[true]?.forEach { processEntity(it, deltaTime) }
        batch.end()
        batch.begin(ShapeRenderer.ShapeType.Line)
        groupBy[false]?.forEach { processEntity(it, deltaTime) }
        batch.end()
    }
}