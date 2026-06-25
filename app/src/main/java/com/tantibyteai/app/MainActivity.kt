package com.tantibyteai.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tantibyteai.app.commands.CommandDispatcher
import com.tantibyteai.app.model.PersonaProfile
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    private val persona = PersonaProfile()
    private val dispatcher by lazy { CommandDispatcher(this) }
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setContent {
            TantiByteAITheme {
                ChatScreen(
                    persona = persona,
                    dispatcher = dispatcher,
                    speechRecognizer = speechRecognizer
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

@Composable
fun TantiByteAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF1E88E5),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun ChatScreen(
    persona: PersonaProfile,
    dispatcher: CommandDispatcher,
    speechRecognizer: SpeechRecognizer?
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        messages.add(ChatMessage(
            "Salut ${persona.defaultAddress}! Sunt ${persona.name}. Scrie sau vorbeste!",
            isUser = false
        ))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(text, isUser = true))
        dispatcher.handleFreeText(text)
        val reply = getAIReply(text)
        messages.add(ChatMessage(reply, isUser = false))
        scope.launch {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun startListening() {
        if (speechRecognizer == null) return
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull() ?: return
                sendMessage(spoken)
            }
        })
        speechRecognizer.startListening(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = persona.name,
                color = Color(0xFF42A5F5),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Avatar animat central - palpaie cand asculta
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingAvatar(isListening = isListening)
        }

        // Mesaje
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg -> MessageBubble(msg) }
        }

        // Bara de input cu microfon langa text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Buton microfon - stanga sau dreapta textului
            IconButton(
                onClick = { startListening() },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isListening) Color(0xFF43A047) else Color(0xFF333333),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microfon",
                    tint = if (isListening) Color.White else Color(0xFF42A5F5)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Scrie o comanda...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = Color(0xFF1E88E5)
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotEmpty()) {
                        inputText = ""
                        sendMessage(text)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1E88E5), CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Trimite", tint = Color.White)
            }
        }
    }
}

@Composable
fun PulsingAvatar(isListening: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val normalScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "normalScale"
    )
    val listeningScale by transition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listeningScale"
    )
    val scale = if (isListening) listeningScale else normalScale
    val colors = if (isListening)
        listOf(Color(0xFF43A047), Color(0xFF1B5E20))
    else
        listOf(Color(0xFF42A5F5), Color(0xFF0D47A1))

    Box(
        modifier = Modifier
            .size(110.dp)
            .scale(scale)
            .background(
                brush = Brush.radialGradient(colors = colors),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "AI Avatar",
            tint = Color.White,
            modifier = Modifier.size(52.dp)
        )
    }
    if (isListening) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ascult...",
            color = Color(0xFF43A047),
            fontSize = 13.sp
        )
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val bubbleColor = if (msg.isUser) Color(0xFF1E88E5) else Color(0xFF2A2A2A)
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = msg.text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

fun getAIReply(input: String): String {
    val t = input.lowercase().trim()
    return when {
        t.contains("salut") || t.contains("buna") || t.contains("hey") ->
            "Salut fraiere! Gata sa te ajut!"
        t.contains("cum esti") || t.contains("ce mai faci") ->
            "In forma maxima fraiere!"
        t.contains("multumesc") || t.contains("mersi") ->
            "Cu placere fraiere!"
        t.contains("maps") || t.contains("navigatie") || t.contains("drum") ->
            "Pornesc Google Maps fraiere!"
        t.contains("youtube") || t.contains("video") ->
            "Deschid YouTube fraiere!"
        t.contains("suna") || t.contains("apel") || t.contains("telefon") ->
            "Deschid telefonul fraiere!"
        t.contains("whatsapp") ->
            "Deschid WhatsApp fraiere!"
        t.contains("browser") || t.contains("internet") || t.contains("google") ->
            "Deschid browserul fraiere!"
        t.contains("ajutor") || t.contains("help") || t.contains("comenzi") ->
            "Pot face: maps, youtube, suna, whatsapp, browser fraiere!"
        t.isEmpty() -> "Spune ceva fraiere!"
        else -> "Nu am inteles '${t.take(20)}', incearca: maps, youtube, suna fraiere!"
    }
}
