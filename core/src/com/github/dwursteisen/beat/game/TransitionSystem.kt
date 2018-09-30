package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.get

class TransitionSystem(assets: AssetManager,
                       private val viewport: Viewport,
                       private val batch: SpriteBatch,
                       private val callback: (Entity) -> Unit) : IteratingSystem(Family.all(Transition::class.java, StateComponent::class.java).get()) {

    private val shader: ShaderProgram = assets["shaders/transition.frag"]
    private val texture: TextureRegion


    init {
        val ase: Aseprite = assets[Config.transitions]
        texture = ase.frame(0)
    }


    private val state: ComponentMapper<StateComponent> = get()
    private val transition: ComponentMapper<Transition> = get()

    private val tmp = Vector3()
    private val tmp2 = Vector3()

    var enabled = true


    override fun processEntity(entity: Entity, deltaTime: Float) {

        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined

        val time = entity[state].time
        val duration = entity[transition].duration
        val wayIn = entity[transition].wayIn
        // add a little offset so the screen will be black
        if (!entity[transition].done && wayIn && time > duration + 0.3f) {
            entity[transition].done = true
            callback(entity)
        } else if (!entity[transition].done && !wayIn && time > duration) {
            entity[transition].done = true
            callback(entity)
        }

        if (enabled) {
            val percent = if (wayIn) {
                Math.min(1f, time / duration)
            } else {
                Math.max(0f, 1 - time / duration)
            }

            shader.begin()
            shader.setUniformf("alpha", percent)
            shader.end()
            batch.begin()

            val w = viewport.worldWidth
            val h = viewport.worldHeight

            tmp2.sub(tmp)

            batch.shader = shader
            batch.draw(texture, -w * 0.5f, -h * 0.5f, w, h)
            batch.end()
            batch.shader = null
        }
    }
}
