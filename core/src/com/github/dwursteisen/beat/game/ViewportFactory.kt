package com.github.dwursteisen.beat.game

import com.badlogic.gdx.utils.viewport.*

object ViewportFactory {
    fun create(width: Float, height: Float): Viewport {
        return when (Config.viewport) {
            "FillViewport" -> FillViewport(width, height)
            "ExtendViewport" -> ExtendViewport(width, height)
            "StretchViewport" -> StretchViewport(width, height)
            "ScreenViewport" -> ScreenViewport()
            "FitViewport" -> FitViewport(width, height)
            else -> TODO("${Config.viewport} is not a supported value for viewport")
        }
    }
}