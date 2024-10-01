package com.tamersarioglu.speechrecognizer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tamersarioglu.speechrecognizer.ui.theme.SpeechRecognizerTheme
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeechRecognizerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpeechToTextScreen()
                }
            }
        }
    }
}

@Composable
fun SpeechToTextScreen() {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isManuallyStopped by remember { mutableStateOf(false) }

    // Permission launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val microphonePermission = android.Manifest.permission.RECORD_AUDIO
    if (ContextCompat.checkSelfPermission(context, microphonePermission) != PackageManager.PERMISSION_GRANTED) {
        LaunchedEffect(Unit) {
            speechRecognizerLauncher.launch(microphonePermission)
        }
    }

    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
    }

    val recognitionListener = remember {
        object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (!isManuallyStopped) {
                    speechRecognizer.startListening(speechRecognizerIntent)
                } else {
                    isListening = false
                }
            }

            override fun onError(error: Int) {
                if (!isManuallyStopped) {
                    speechRecognizer.startListening(speechRecognizerIntent)
                } else {
                    isListening = false
                    Toast.makeText(context, "Error occurred: $error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    spokenText += matches.firstOrNull() ?: ""
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    speechRecognizer.setRecognitionListener(recognitionListener)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (!isListening) {
                    isManuallyStopped = false
                    speechRecognizer.startListening(speechRecognizerIntent)
                    isListening = true
                } else {
                    isManuallyStopped = true
                    speechRecognizer.stopListening()
                    isListening = false
                }
            },
            enabled = true
        ) {
            Text(if (isListening) "Stop Listening" else "Start Listening")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "You said: $spokenText")
    }
}