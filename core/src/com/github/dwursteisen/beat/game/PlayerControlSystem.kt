package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.libgdx.ashley.get
import com.github.dwursteisen.libgdx.ashley.removeAllWith
import com.github.dwursteisen.libgdx.v2
import ktx.ashley.entity

class PlayerControlSystem(private val viewport: Viewport) : IteratingSystem(Family.all(Player::class.java, Position::class.java).get()) {

    private val player: ComponentMapper<Player> = get()
    private val position: ComponentMapper<Position> = get()
    private val size: ComponentMapper<Size> = get()

    private val origin = Vector2()
    private val current = Vector2()
    private var movingByTouch = false

    private val tmp = Vector2()

    override fun processEntity(pEntity: Entity, deltaTime: Float) {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            pEntity[player].direction.x = -128.0f * deltaTime
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            pEntity[player].direction.x = 128.0f * deltaTime
        } else if (Gdx.input.isTouched) {

            if (!movingByTouch) {
                movingByTouch = true

                origin.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                viewport.unproject(origin)
                current.set(origin)

                engine.entity {
                    entity.add(Position(position = origin))
                            .add(PlayerTouch())
                            .add(Debugable())
                            .add(Size(4f v2 4f))
                            .add(ShapeToRender(type = ShapeType.Circle, color = Color.WHITE))
                }

                engine.entity {
                    entity.add(Position(position = current))
                            .add(PlayerTouch())
                            .add(Debugable())
                            .add(Size(4f v2 4f))
                            .add(ShapeToRender(type = ShapeType.Circle, color = Color.WHITE))
                }
            } else {
                tmp.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                viewport.unproject(tmp)

                pEntity[player].direction.x = tmp.x - current.x

                current.set(tmp)
            }


        } else {
            if (movingByTouch) {
                engine.removeAllWith(PlayerTouch::class.java)
                movingByTouch = false
            }
            pEntity[player].direction.x = 0f
        }

        // sort de la zone ?
        tmp.set(pEntity[player].direction)
                .add(pEntity[position].position)

        if (tmp.x.between(0f, screenWidth - pEntity[size].size.x)) {
            pEntity[position].position.add(pEntity[player].direction)
        }
    }
}