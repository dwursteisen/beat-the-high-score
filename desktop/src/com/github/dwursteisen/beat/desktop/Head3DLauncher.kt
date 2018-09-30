package com.github.dwursteisen.beat.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import io.anuke.gif.GifRecorder


object Head3DLauncher {


    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        LwjglApplication(object : ApplicationAdapter() {

            private lateinit var modelBatch: ModelBatch
            private lateinit var environment: Environment
            private lateinit var cam: PerspectiveCamera
            private lateinit var instance: ModelInstance
            private lateinit var camController: CameraInputController
            private lateinit var model: Model

            lateinit var gifBatch: SpriteBatch
            lateinit var recorder: GifRecorder
            override fun create() {

                gifBatch = SpriteBatch()
                recorder = GifRecorder(gifBatch)

                modelBatch = ModelBatch()
                environment = Environment()
                environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
                environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

                cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
                cam.position.set(1f, 1f, 1f)
                cam.lookAt(0f, 0f, 0f)
                cam.near = 1f
                cam.far = 300f
                cam.update()

                val loader = ObjLoader()
                model = loader.loadModel(Gdx.files.internal("head.obj"))
                instance = ModelInstance(model)

                camController = CameraInputController(cam)
                Gdx.input.inputProcessor = camController
            }

            override fun render() {

                camController.update()

                Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
                Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT or GL30.GL_DEPTH_BUFFER_BIT)

                modelBatch.begin(cam)
                modelBatch.render(instance, environment)
                modelBatch.end()

                recorder.update()
            }


            override fun dispose() {
                modelBatch.dispose()
                model.dispose()
            }
        }, config)
    }
}