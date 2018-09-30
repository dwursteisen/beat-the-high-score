package com.github.dwursteisen.beat.game

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IntervalSystem
import com.badlogic.gdx.assets.AssetManager
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.ashley.Rotation
import com.github.dwursteisen.libgdx.ashley.StateComponent
import com.github.dwursteisen.libgdx.ashley.get
import com.github.dwursteisen.libgdx.v2
import ktx.ashley.entity

class BallCopySystem(private val assets: AssetManager) : IntervalSystem(0.1f) {

    override fun updateInterval() {
        engine.entity {

            val ballSpr: Aseprite = assets["sheets/egg"]
            entity.add(BallCopy(alpha = 0.25f))
                    .add(Position(0 v2 0))
                    .add(Size(8 v2 9))
                    .add(EntityRender(ballSpr.frame(0), zLevel = -1, alpha = 0.25f))
                    .add(StateComponent())
                    .add(Rotation(origin = 4 v2 4))

            entity[ballCopy].alpha = 1f
            val ball = engine.getEntitiesFor(BALL_FAMILY).firstOrNull() ?: return
            entity[position].position.set(ball[position].position)
            entity[rotation].degree = ball[rotation].degree
        }
    }

    private val ballCopy: ComponentMapper<BallCopy> = get()
    private val position: ComponentMapper<Position> = get()
    private val rotation: ComponentMapper<Rotation> = get()


    companion object {
        private val BALL_FAMILY = Family.all(Ball::class.java).get()
    }


}