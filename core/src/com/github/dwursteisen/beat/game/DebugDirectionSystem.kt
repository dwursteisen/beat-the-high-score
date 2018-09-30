package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.github.dwursteisen.libgdx.ashley.Direction
import com.github.dwursteisen.libgdx.ashley.get
import com.github.dwursteisen.libgdx.ashley.getNullable

class DebugDirectionSystem(val batch: ShapeRenderer) : IteratingSystem(Family.all(Debugable::class.java, Position::class.java).get()) {

    private val ball: ComponentMapper<Ball> = get()
    private val player: ComponentMapper<Player> = get()
    private val position: ComponentMapper<Position> = get()
    private val size: ComponentMapper<Size> = get()
    private val direction: ComponentMapper<Direction> = get()

    private val tmp = Vector2()
    private val tmp2 = Vector2()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val b = entity.getNullable(ball)
        val p = entity.getNullable(player)
        val d = entity.getNullable(direction)

        val position = entity[position].position
        if (b != null) {
            draw(position, b.direction)
        } else if (p != null) {
            tmp2.set(entity[size].size)
                    .scl(0.5f)
                    .add(position)
            draw(tmp2, p.direction)
        } else if (d != null) {
            draw(position, d.value)
        }
    }

    private fun draw(position: Vector2, direction: Vector2) {
        val arrow = Vector2()

        tmp.set(direction).nor().scl(10f).add(position)
        batch.color = Color.RED
        batch.line(position, tmp)

        arrow.set(tmp)
                .sub(position)
                .nor()
                .scl(5f)
                .rotate(45f + 90f)
                .add(tmp)
        batch.color = Color.GREEN
        batch.line(tmp, arrow)

        arrow.sub(tmp)
                .rotate(+90f)
                .add(tmp)
        batch.color = Color.BLUE
        batch.line(tmp, arrow)
    }

    override fun update(deltaTime: Float) {
        batch.begin(ShapeRenderer.ShapeType.Line)
        super.update(deltaTime)
        batch.end()
    }
}