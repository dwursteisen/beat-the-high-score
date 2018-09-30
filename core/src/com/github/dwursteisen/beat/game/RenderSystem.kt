package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.dwursteisen.libgdx.ashley.Rotation
import com.github.dwursteisen.libgdx.ashley.get
import com.github.dwursteisen.libgdx.ashley.getNullable

class RenderSystem(val batch: SpriteBatch) : IteratingSystem(all(EntityRender::class.java).get()) {

    var toRender = mutableListOf<Entity>()

    private val renderMapper: ComponentMapper<EntityRender> = get()
    private val position: ComponentMapper<Position> = get()
    private val size: ComponentMapper<Size> = get()
    private val rotation: ComponentMapper<Rotation> = get()


    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (entity[renderMapper].enabled) {
            toRender.add(entity)
        }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        toRender.sortWith(compareBy({ it[renderMapper].zLevel }, { -it[position].position.y }))
        draw(toRender)
        toRender.clear()
    }

    private fun draw(entities: List<Entity>) {
        batch.begin()
        entities.forEach {
            val entityRender = it[renderMapper]
            if (entityRender.texture != NO_TEXTURE) {

                val position = it[position].position
                val size = it[size].size
                val rotation = it.getNullable(rotation)
                val offset = entityRender.offset

                val (originX, originY, degree) = if (rotation != null) {
                    listOf(rotation.origin.x, rotation.origin.y, rotation.degree)
                } else {
                    listOf(0f, 0f, 0f)
                }

                val (x, sizeX) = if (entityRender.hFlip) {
                    (position.x + offset.x + size.x) to (-size.x)
                } else {
                    (position.x + offset.x) to (size.x)
                }
                batch.setColor(1f, 1f, 1f, entityRender.alpha)
                batch.draw(entityRender.texture,
                        x, position.y + offset.y,
                        originX, originY,
                        sizeX, size.y,
                        1f, 1f,
                        degree)
            }
        }
        batch.end()
    }

}