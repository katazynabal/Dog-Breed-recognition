package com.example.dogbreedrecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
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

        val batchNum = 0
        val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
        for (x in 0..223) {
            for (y in 0..223) {
                val pixel = bitmap.getPixel(x, y)
                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                // model. For example, some models might require values to be normalized
                // to the range [0.0, 1.0] instead.
                input[batchNum][x][y][0] = (((pixel shr 16 and 0xFF) - MEAN) / STD)
                input[batchNum][x][y][1] = (((pixel shr 8 and 0xFF) - MEAN) / STD)
                input[batchNum][x][y][2] = (((pixel and 0xFF) - MEAN) / STD)
            }
        }

        val inputs = FirebaseModelInputs.Builder()
            .add(input) // add() as many input arrays as your model requires
            .build()

        interpreter?.run(inputs, inputOutputOptions)?.addOnSuccessListener { result ->
            val output = result.getOutput<Array<FloatArray>>(0)
            val label = labels
                .mapIndexed { index, label ->
                    Pair(label, output[0][index])
                }.maxBy { it.second }!!

            //view?.displayDogBreed(label.first, label.second*100)
            Log.e("boom","veikia ->" + label)

            val string: String = ""
            val alertDialogBuilder = AlertDialog.Builder(imageView.context)
            alertDialogBuilder.setTitle(string)
            alertDialogBuilder.setMessage(label?:"")
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