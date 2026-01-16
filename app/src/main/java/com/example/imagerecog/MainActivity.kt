// Package name of the Android application
package com.example.imagerecog

// Importing required Android classes
import android.content.Intent              // For opening gallery intent
import android.graphics.Bitmap              // For handling image bitmap
import android.net.Uri                      // For image URI
import androidx.appcompat.app.AppCompatActivity // Base activity class
import android.os.Bundle                    // For activity lifecycle
import android.provider.MediaStore          // For converting URI to Bitmap
import android.view.View                    // For view handling
import android.widget.Button                // Button UI component
import android.widget.ImageView             // ImageView UI component
import android.widget.TextView              // TextView UI component

// Importing TensorFlow Lite generated model class
import com.example.imagerecog.ml.MobilenetV110224Quant

// Importing TensorFlow Lite helper classes
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

// Main activity class extending AppCompatActivity
class MainActivity : AppCompatActivity() {

    // Bitmap variable to store selected image
    lateinit var bitmap: Bitmap

    // ImageView to display selected image
    lateinit var imgview: ImageView

    // onCreate method is called when activity starts
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Linking XML layout file to this activity
        setContentView(R.layout.activity_main)

        // Connecting ImageView from XML
        imgview = findViewById(R.id.imageView)

        // Label file name stored in assets folder
        val fileName = "label.txt"

        // Reading labels from assets folder
        val inputString = application.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }

        // Splitting labels line by line into a list
        val townList = inputString.split("\n")

        // Connecting TextView to display result
        var tv: TextView = findViewById(R.id.textView)

        // Connecting "Select Image" button
        var select: Button = findViewById(R.id.button)

        // Click listener for selecting image
        select.setOnClickListener(View.OnClickListener {

            // Intent to open image picker
            var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)

            // Allow only image files
            intent.type = "image/*"

            // Start gallery activity
            startActivityForResult(intent, 100)
        })

        // Connecting "Predict" button
        var predict: Button = findViewById(R.id.button2)

        // Click listener for prediction
        predict.setOnClickListener(View.OnClickListener {

            // Resize image to model input size (224x224)
            var resized: Bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

            // Load TensorFlow Lite model
            val model = MobilenetV110224Quant.newInstance(this)

            // Create input tensor buffer with required shape
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)

            // Convert bitmap to TensorImage
            var tbuffer = TensorImage.fromBitmap(resized)

            // Extract ByteBuffer from TensorImage
            var byteBuffer = tbuffer.buffer

            // Load image data into input tensor
            inputFeature0.loadBuffer(byteBuffer)

            // Run model inference
            val outputs = model.process(inputFeature0)

            // Get output tensor
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            // Get index of maximum probability
            var max = getMax(outputFeature0.floatArray)

            // Display predicted label
            tv.setText(townList[max])

            // Close model to free memory
            model.close()
        })
    }

    // Called when image selection activity finishes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Display selected image in ImageView
        imgview.setImageURI(data?.data)

        // Get image URI
        var url: Uri? = data?.data

        // Convert URI to Bitmap
        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, url)
    }

    // Function to get index of maximum value from array
    fun getMax(arr: FloatArray): Int {

        // Index of max value
        var ind = 0

        // Initial minimum value
        var min = 0.0f

        // Loop through output probabilities
        for (i in 0..1000) {
            if (arr[i] > min) {
                ind = i        // Update index
                min = arr[i]   // Update max value
            }
        }

        // Return index of highest probability
        return ind
    }
}
