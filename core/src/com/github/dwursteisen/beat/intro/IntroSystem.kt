package com.github.dwursteisen.beat.intro

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.I18NBundle
import com.github.dwursteisen.beat.BeatTheHighScore
import com.github.dwursteisen.beat.game.Animated
import com.github.dwursteisen.beat.game.EntityRender
import com.github.dwursteisen.beat.game.Position
import com.github.dwursteisen.beat.game.Size
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.get
import org.apache.commons.lang3.text.WordUtils

class IntroSystem(assetManager: AssetManager) : IteratingSystem(Family.all(Intro::class.java).get()) {

    private val spritesData: Aseprite = assetManager["sheets/intro"]

    private val animation: ComponentMapper<Animated> = get()
    private val state: ComponentMapper<StateComponent> = get()
    private val textRender: ComponentMapper<TextRender> = get()
    private val size: ComponentMapper<Size> = get()
    private val position: ComponentMapper<Position> = get()

    private var index = -1

    private val actions: List<Action>

    init {

        val i18n: I18NBundle = assetManager["i18n/messages"]

        actions = listOf(
                Action.Text(i18n["intro.fox.line1"], this),
                Action.Text(i18n["intro.fox.line2"], this),
                Action.Anim("fox", this),
                Action.Text(i18n["intro.kidnapping.line1"], this),
                Action.Anim("kidnapping", this),
                Action.Text(i18n["intro.chicken.line1"], this),
                Action.Text(i18n["intro.chicken.line2"], this),
                Action.Anim("chicken", this),
                Action.Text(i18n["intro.end.line1"], this),
                Action.Text(i18n["intro.end.line2"], this)
        )
    }

    sealed class Action(val parent: IntroSystem) {

        abstract fun isFinished(entity: Entity, deltaTime: Float): Boolean
        open fun processEntity(entity: Entity, deltaTime: Float) {

        }

        class Text(tmpTxt: String, parent: IntroSystem) : Action(parent) {

            val txt: String = WordUtils.wrap(tmpTxt, 19)
            val duration = txt.trim().length * 0.1f + 1f

            override fun isFinished(entity: Entity, deltaTime: Float): Boolean {
                return entity[parent.state].time >= duration
            }

            override fun processEntity(entity: Entity, deltaTime: Float) {
                val t = entity[parent.state].time
                val length = Math.round((t / (txt.length * 0.1f)) * txt.length)
                val nbLetters = MathUtils.clamp(length, 0, txt.length)
                entity[parent.textRender].text = txt.take(nbLetters)
            }
        }

        class Anim(val name: String, parent: IntroSystem) : Action(parent) {
            override fun isFinished(entity: Entity, deltaTime: Float): Boolean {
                return entity[parent.animation].animation.isAnimationFinished(entity[parent.state].time)
            }

        }
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        fun nextAnimation() {
            val action = actions[++index]
            when (action) {
                is Action.Text -> {
                    entity.remove(Animated::class.java)
                    entity.remove(EntityRender::class.java)
                    entity.add(TextRender("", scale = 1 / 4f))

                    entity[position].position.set(-128f * 0.3f, -56f)
                    entity[size].size.set(128f * 0.8f, 64f)
                }
                is Action.Anim -> {
                    entity.remove(TextRender::class.java)
                    entity.add(Animated())
                    entity.add(EntityRender())
                    entity[position].position.set(-128f * 0.5f, -192f * 0.5f)
                    entity[size].size.set(128f, 192f)
                    entity[animation].animation = spritesData[action.name]
                }
            }
            entity[state].time = 0f
        }

        if (index == -1) {
            nextAnimation()
        } else if (actions[index].isFinished(entity, deltaTime)) {
            if (index >= actions.size - 1) {
                BeatTheHighScore.title()
            } else {
                nextAnimation()
            }
            return
        }
        actions[index].processEntity(entity, deltaTime)
    }

}