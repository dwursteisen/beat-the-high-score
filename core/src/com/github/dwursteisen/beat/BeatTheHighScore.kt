package com.github.dwursteisen.beat

import com.badlogic.gdx.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.Logger
import com.github.dwursteisen.beat.credits.CreditsScreen
import com.github.dwursteisen.beat.game.Config
import com.github.dwursteisen.beat.game.GameScreen
import com.github.dwursteisen.beat.intro.IntroScreen
import com.github.dwursteisen.beat.options.OptionsScreen
import com.github.dwursteisen.beat.title.TitleScreen
import com.github.dwursteisen.libgdx.aseprite.Aseprite
import com.github.dwursteisen.libgdx.aseprite.AsepriteJson
import com.github.dwursteisen.libgdx.aseprite.AsepriteJsonLoader
import com.github.dwursteisen.libgdx.aseprite.AsepriteLoader
import ktx.log.info
import ktx.scene2d.Scene2DSkin

open class BeatTheHighScore : Game() {

    private val assetsManager = object : AssetManager() {
        override fun <T : Any?> get(fileName: String?, type: Class<T>?): T {
            return try {
                super.get(fileName, type)
            } catch (ex: GdxRuntimeException) {
                ktx.log.error(ex) {
                    "Asset $fileName not yet loaded! You should load it before accessing to it! " +
                            "Will force the loading of it. (Bad performance expected!)"
                }

                finishLoadingAsset(fileName)
                super.get(fileName, type)
            }
        }
    }

    private var isLoaded = false

    private var screenOnNextLoop: Screen? = null

    override fun create() {


        info { "Application start loading assets..." }
        assetsManager.logger.level = Logger.ERROR // enable logging on assets manager
        assetsManager.setLoader(Aseprite::class.java, AsepriteLoader(InternalFileHandleResolver()))
        assetsManager.setLoader(AsepriteJson::class.java, AsepriteJsonLoader(InternalFileHandleResolver()))
        assetsManager.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()))

        info { "Loading sprite sheets" }
        assetsManager.load("sheets/intro", Aseprite::class.java)

        info { "Loading i18n bundles" }
        assetsManager.load("i18n/messages", I18NBundle::class.java)

        info { "Loading fonts" }
        assetsManager.load("krungthep.fnt", BitmapFont::class.java)
        assetsManager.load("krungthep2.fnt", BitmapFont::class.java)

        info { "Loading scene2D skins" }
        Scene2DSkin.defaultSkin = Skin(Gdx.files.internal("skin/skin2d.json"))
        Scene2DSkin.defaultSkin.getFont("button").setUseIntegerPositions(false)
        Scene2DSkin.defaultSkin.getFont("button-reduit").setUseIntegerPositions(false)

        info { "Loading Music" }
        assetsManager.load("sfx/beat_intro.ogg", Music::class.java)

        info { "Setting shader" }
        ShaderProgram.pedantic = false

        BeatTheHighScore.game = this
    }

    override fun render() {
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                // used for hot reload
                Scene2DSkin.defaultSkin = Skin(Gdx.files.internal("skin/skin2d.json"))
                screenOnNextLoop = screen
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                credits()
            }
        }

        if (!isLoaded) {
            isLoaded = assetsManager.update()
            if (isLoaded) {
                info { "Assets for introduction fully loaded!" }
                BeatTheHighScore.intro()
            }
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        } else {
            screenOnNextLoop?.let {
                val checkUpdate = screenOnNextLoop
                setScreen(screenOnNextLoop)
                if (checkUpdate == screenOnNextLoop) {
                    screenOnNextLoop = null
                }
            }
            super.render()
        }
    }

    override fun dispose() {
    }

    companion object {

        private lateinit var game: BeatTheHighScore

        private val optionsScreen by lazy { OptionsScreen(game.assetsManager) }
        private val gameScreen by lazy { GameScreen(game.assetsManager) }
        private val titleScreen by lazy { TitleScreen(game.assetsManager) }
        private val introScreen by lazy { IntroScreen(game.assetsManager) }
        private val credits by lazy { CreditsScreen(game.assetsManager) }

        private val levels = mapOf(
                "level0.tmx" to "level1.tmx",
                "level1.tmx" to "level2.tmx",
                "level2.tmx" to "level3.tmx",
                "level3.tmx" to "level4.tmx",
                "level4.tmx" to "level5.tmx",
                "level5.tmx" to "level6.tmx",
                "level6.tmx" to "level7.tmx",
                "level7.tmx" to "level8.tmx"
        )

        fun title() {
            game.screenOnNextLoop = titleScreen
        }

        fun options() {
            game.screenOnNextLoop = optionsScreen
        }

        fun credits() {
            game.screenOnNextLoop = credits
        }

        fun play() {
            game.screenOnNextLoop = gameScreen
        }

        fun intro() {
            game.screenOnNextLoop = introScreen
        }

        fun nextLevel() {
            val currentLevel = gameScreen.levelName
            val nextLevel = levels[currentLevel]
            if (nextLevel == null) {
                gameScreen.levelName = Config.level
                credits()
            } else {
                gameScreen.levelName = nextLevel
                play()
            }
        }

        fun updateLevel(levelName: String) {
            gameScreen.levelName = levelName
        }
    }
}
