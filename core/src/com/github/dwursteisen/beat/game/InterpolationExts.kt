package com.github.dwursteisen.beat.game

import com.badlogic.gdx.math.Interpolation

fun Interpolation.invoke(value: Float): Float {
    return when (Config.interpolation) {
        "DISABLED" -> this.apply(1f)
        "LINEAR" -> Interpolation.linear.apply(value)
        "ELASTIC" -> Interpolation.elastic.apply(value)
        "POW2" -> Interpolation.pow2.apply(value)
        "BOUNCE" -> Interpolation.bounce.apply(value)
        else -> this.apply(value)
    }
}


fun Interpolation.invoke(start: Float, stop: Float, value: Float): Float {
    return when (Config.interpolation) {
        "DISABLED" -> this.apply(start, value, 1f)
        "LINEAR" -> Interpolation.linear.apply(start, stop, value)
        "ELASTIC" -> Interpolation.elastic.apply(start, stop, value)
        "POW2" -> Interpolation.pow2.apply(start, stop, value)
        "BOUNCE" -> Interpolation.bounce.apply(start, stop, value)
        else -> this.apply(start, stop, value)
    }
}
