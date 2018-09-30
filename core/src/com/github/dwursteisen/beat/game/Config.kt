package com.github.dwursteisen.beat.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import kotlin.reflect.KProperty

object Config {
    private val preferences by lazy(LazyThreadSafetyMode.NONE) { Gdx.app.getPreferences("beat") }

    private inline fun <reified T> pref(default: T) = ByPreference(preferences, T::class.java, default)

    var interpolation by pref("CURRENT")
    var shader by pref(true)
    var hitbox by pref(false)
    var sprites by pref(true)
    var box2d by pref(true)
    var position by pref(false)
    var direction by pref(false)
    var particles by pref(true)
    var viewport by pref("FitViewport")
    var customFont by pref(true)
    var transitions by pref("sheets/transition2")
    var scene2d by pref(false)
    var music by pref(true)
    var sfx by pref(true)
    var level by pref("level0.tmx")
    var credit by pref("pixel")

    class ByPreference<T>(private val preferences: Preferences, private val clazz: Class<T>, private val default: T) {

        @SuppressWarnings("unchecked")
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val result: Any = when (clazz) {
                String::class.javaObjectType -> preferences.getString(property.name, default as String)
                Boolean::class.javaObjectType -> preferences.getBoolean(property.name, default as Boolean)
                Int::class.javaObjectType -> preferences.getInteger(property.name, default as Int)
                else -> TODO("$clazz is not a managed type")
            }
            return result as T
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            when (clazz) {
                String::class.javaObjectType -> preferences.putString(property.name, value as String)
                Boolean::class.javaObjectType -> preferences.putBoolean(property.name, value as Boolean)
                Int::class.javaObjectType -> preferences.putInteger(property.name, value as Int)
                else -> TODO("$clazz is not a managed type")
            }
            preferences.flush()
        }
    }

    override fun toString(): String {
        return """
            {
                interpolation: $interpolation,
                shader: $shader,
                hitbox: $hitbox,
                sprites: $sprites,
                box2d: $box2d,
                position: $position,
                direction: $direction,
                particles: $particles,
                viewport: $viewport,
                customFont: $customFont,
                scene2d: $scene2d,
                music: $music,
                sfx: $sfx
             }
        """.trimIndent()
    }
}
