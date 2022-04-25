package com.example.pract8

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord.createMime
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.pract8.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), NfcAdapter.CreateNdefMessageCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageUri: Uri
    private lateinit var nfcAdapter: NfcAdapter

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result!!.data!!.data
            imageUri = Uri.parse(data.toString())
            val cursor = contentResolver.query(imageUri, null, null, null, null)
            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            val text = "Имя файла: ${cursor?.getString(nameIndex!!).toString()}\nТип: ${contentResolver.getType(imageUri)}\nРазмер: ${cursor?.getLong(sizeIndex!!).toString()} байт"
            binding.imageData.text = text
            binding.gifView.setImageURI(Uri.parse(data.toString()))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Share text to another app
        binding.textShareButton.setOnClickListener {
            if (binding.editTextTextPersonName.text.isNotEmpty()) {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, binding.editTextTextPersonName.text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            } else
                Toast.makeText(applicationContext, "Empty text", Toast.LENGTH_LONG).show()
        }

        // Select image from storage
        binding.selectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }

        // Share image to another app
        binding.shareImage.setOnClickListener {
            if (this::imageUri.isInitialized) {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    type = "image/jpeg"
                }
                startActivity(Intent.createChooser(shareIntent, null))
            } else
                Toast.makeText(applicationContext, "Please select image", Toast.LENGTH_LONG).show()
        }

        // Check for NFC feature
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            nfcAdapter?.setNdefPushMessageCallback(this, this)
        }
    }

    override fun createNdefMessage(event: NfcEvent): NdefMessage {
        val text = "hi"
        return NdefMessage(
            arrayOf(
                createMime("application/vnd.com.example.android.beam", text.toByteArray())
            )
        )
    }

    override fun onResume() {
        super.onResume()
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Parses the NDEF Message from the intent and prints to the EditText
     */
    private fun processIntent(intent: Intent) {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMsgs ->
            (rawMsgs[0] as NdefMessage).apply {
                binding.editTextTextPersonName.setText(String(records[0].payload))
            }
        }
    }
}