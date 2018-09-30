package com.github.dwursteisen.beat.intro

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.dwursteisen.beat.game.Position
import com.github.dwursteisen.beat.game.Size
import com.github.dwursteisen.libgdx.ashley.get

class TextRenderSystem(private val batch: SpriteBatch, private val font: BitmapFont) : IteratingSystem(Family.all(TextRender::class.java).get()) {

    private val text: ComponentMapper<TextRender> = get()
    private val position: ComponentMapper<Position> = get()
    private val size: ComponentMapper<Size> = get()


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val text = entity[text]
        val position = entity[position]
        val size = entity[size]

        font.data.setScale(text.scale)
        font.color = text.color
        font.draw(batch, text.text, position.position.x, position.position.y + size.size.y, size.size.x, text.halign, true)
    }

    override fun update(deltaTime: Float) {
        batch.begin()
        super.update(deltaTime)
        batch.end()
    }
}