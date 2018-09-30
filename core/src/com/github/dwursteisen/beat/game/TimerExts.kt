package com.github.dwursteisen.beat.game

import com.badlogic.gdx.utils.Timer

private class DelegateTime(val runDelegate: () -> Unit) : Timer.Task() {
    override fun run() {
        runDelegate()
    }

}

fun runLater(run: () -> Unit, delay: Float) {
    Timer.schedule(DelegateTime(run), delay)
}