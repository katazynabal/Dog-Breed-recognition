package com.example.dogbreedrecognition

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import com.google.firebase.ml.custom.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DogDetector(private val context: Context) {
    private val labels = mutableListOf<String>()
    private lateinit var dataOptions: FirebaseModelInputOutputOptions

    companion object {
        private const val IMG_SIZE = 224
        private const val ASSET = "asset"
        private const val MODEL_NAME = "dog-breed-detector"
        private const val MEAN = 128
        private const val STD = 128.0f
    }

    init {
        initializeLabels()
    }

    fun doStuff(bitmap: Bitmap, imageView: ImageView) {
        val localModel = FirebaseCustomLocalModel.Builder()
            .setAssetFilePath("dog-breed-detector.tflite")
            .build()

        val options = FirebaseModelInterpreterOptions.Builder(localModel).build()
        val interpreter = FirebaseModelInterpreter.getInstance(options)
        FirebaseModelInterpreterOptions.Builder(localModel).build()

        val inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 224, 224, 3))
            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, labels.size))
            .build()

        val imgData = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val pixels = IntArray(IMG_SIZE * IMG_SIZE)
        Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, false).apply {
            getPixels(pixels, 0, width, 0, 0, width, height)
        }

        pixels.forEach {
            imgData.putFloat(((it shr 16 and 0xFF) - MEAN) / STD)
            imgData.putFloat(((it shr 8 and 0xFF) - MEAN) / STD)
            imgData.putFloat(((it and 0xFF) - MEAN) / STD)
        }

        val inputs = FirebaseModelInputs.Builder()
            .add(imgData) // add() as many input arrays as your model requires
            .build()

        interpreter?.run(inputs, inputOutputOptions)?.addOnSuccessListener { result ->
            val output = result.getOutput<Array<FloatArray>>(0)
            val label = labels
                .mapIndexed { index, label ->
                    Pair(label, output[0][index])
                }.maxBy { it.second }!!

            //view?.displayDogBreed(label.first, label.second*100)
            Log.e("boom","veikia ->" + label)

            val string: String = "Your dog is: "
            val alertDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(imageView.context)
            alertDialogBuilder.setTitle(string)
            alertDialogBuilder.setMessage(label.toString())
            alertDialogBuilder.setPositiveButton("OK") { arg0, arg1 -> }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            }?.addOnFailureListener { e ->
                Log.e("boom","neveikia Q_Q")
            }
    }

    private fun initializeLabels() {
        labels.addAll(context.assets.open("labels.txt").bufferedReader().readLines())
    }
}