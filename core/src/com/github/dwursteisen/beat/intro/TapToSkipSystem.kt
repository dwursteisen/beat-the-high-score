package com.github.dwursteisen.beat.intro

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.get

class TapToSkipSystem : IteratingSystem(Family.all(TapToSkip::class.java).get()) {

    private val state: ComponentMapper<StateComponent> = get()
    private val text: ComponentMapper<TextRender> = get()
    private val tapToSkip: ComponentMapper<TapToSkip> = get()
    private val cycleDuration = 1f

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val currentCycle = entity[state].time - MathUtils.floor(entity[state].time / cycleDuration) * cycleDuration
        if (currentCycle < cycleDuration * 0.5f) {
            entity[text].text = entity[tapToSkip].txt
        } else {
            entity[text].text = ""
        }
    }

}