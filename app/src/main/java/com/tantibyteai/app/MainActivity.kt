package com.tantibyteai.app

import android.os.Bundle
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

data class ChatMessage(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    private val persona = PersonaProfile()
    private val dispatcher by lazy { CommandDispatcher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TantiByteAITheme {
                ChatScreen(persona = persona, dispatcher = dispatcher)
            }
        }
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
fun ChatScreen(persona: PersonaProfile, dispatcher: CommandDispatcher) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        messages.add(ChatMessage(
            "Salut ${persona.defaultAddress}! Sunt ${persona.name}. Spune-mi ce vrei sa fac!",
            isUser = false
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingAvatar()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    val userText = inputText.trim()
                    if (userText.isNotEmpty()) {
                        messages.add(ChatMessage(userText, isUser = true))
                        inputText = ""
                        dispatcher.handleFreeText(userText)
                        val reply = getAIReply(userText)
                        messages.add(ChatMessage(reply, isUser = false))
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
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
fun PulsingAvatar() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarScale"
    )
    Box(
        modifier = Modifier
            .size(110.dp)
            .scale(scale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF42A5F5), Color(0xFF0D47A1))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Tanti Byte AI",
            tint = Color.White,
            modifier = Modifier.size(52.dp)
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
            "Salut fraiere! Gata sa te ajut, zi ce vrei!"
        t.contains("cum esti") || t.contains("ce mai faci") ->
            "In forma maxima fraiere! Ai nevoie de ceva?"
        t.contains("multumesc") || t.contains("mersi") ->
            "Cu placere fraiere! Altceva?"
        t.contains("maps") || t.contains("navigatie") || t.contains("drum") ->
            "Pornesc Google Maps acum fraiere!"
        t.contains("youtube") || t.contains("video") ->
            "Deschid YouTube fraiere, bucura-te!"
        t.contains("suna") || t.contains("apel") || t.contains("telefon") ->
            "Deschid telefonul fraiere, formeaza numarul!"
        t.contains("whatsapp") || t.contains("wa") ->
            "Deschid WhatsApp fraiere!"
        t.contains("browser") || t.contains("internet") || t.contains("google") ->
            "Deschid browserul fraiere!"
        t.contains("ajutor") || t.contains("help") || t.contains("comenzi") ->
            "Pot face: maps, youtube, suna, whatsapp, browser. Incearca fraiere!"
        t.isEmpty() -> "Scrie ceva fraiere, te ascult!"
        else -> "Nu am prins '${t.take(20)}...' dar incearca: maps, youtube, suna, whatsapp fraiere!"
    }
}
