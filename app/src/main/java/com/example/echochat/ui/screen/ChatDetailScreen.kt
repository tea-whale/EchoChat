package com.example.echochat.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.echochat.R
import com.example.echochat.data.local.entity.MessageEntity
import com.example.echochat.ui.viewmodel.ChatViewModel
import com.example.echochat.ui.viewmodel.AgentViewModel
import com.example.echochat.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    agentId: Long? = null,
    groupId: Long? = null,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    agentViewModel: AgentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val agents by agentViewModel.allAgents.collectAsState()
    val user by userViewModel.user.collectAsState()
    val agent = remember(agents, agentId) { agents.find { it.id == agentId } }
    
    val title = remember(agents, agentId, groupId) {
        if (groupId != null) "群聊"
        else agent?.name ?: "聊天"
    }

    val convId by chatViewModel.currentConversationId.collectAsState()
    val messages by if (convId != null) {
        chatViewModel.getMessages(convId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<MessageEntity>()) }
    }
    
    val contextLimit by chatViewModel.contextLimit.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(agentId, groupId) {
        chatViewModel.initConversation(agentId, groupId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("开启新对话") },
                                onClick = {
                                    chatViewModel.startNewConversation(agentId, groupId)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("删除此对话记录") },
                                onClick = {
                                    chatViewModel.deleteCurrentConversation()
                                    showMenu = false
                                    onBack()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                            HorizontalDivider()
                            Text("上下文限制", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                            listOf(10, 20, 100, -1).forEach { limit ->
                                DropdownMenuItem(
                                    text = { Text(if (limit == -1) "不限制" else "${limit}条") },
                                    onClick = {
                                        chatViewModel.setContextLimit(limit)
                                        showMenu = false
                                    },
                                    trailingIcon = {
                                        if (contextLimit == limit) {
                                            RadioButton(selected = true, onClick = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().navigationBarsPadding().imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...") },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(agentId, groupId, inputText)
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // 背景图处理
            if (agent?.chatBackground != null) {
                AsyncImage(
                    model = agent.chatBackground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f 
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val sender = agents.find { it.id == message.senderId }
                    MessageBubble(
                        message = message, 
                        senderName = sender?.name, 
                        senderAvatar = sender?.avatar,
                        userAvatar = user?.avatar,
                        onDelete = { chatViewModel.deleteMessage(message) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity, 
    senderName: String?, 
    senderAvatar: String?,
    userAvatar: String?,
    onDelete: () -> Unit
) {
    val isUser = message.role == "user"
    val isTool = message.role == "tool"
    var showDeleteMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser && !isTool) {
            AsyncImage(
                model = senderAvatar ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${senderName ?: "Agent"}",
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (!isUser && !isTool && senderName != null) {
                Text(
                    senderName, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            
            if (isTool) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.combinedClickable(
                        onLongClick = { showDeleteMenu = true },
                        onClick = {}
                    )
                ) {
                    Text(
                        "系统动作: ${message.content}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            } else {
                Surface(
                    color = if (isUser) Color(0xFF95EC69) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isUser) 8.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 8.dp
                    ),
                    tonalElevation = 1.dp,
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.combinedClickable(
                        onLongClick = { showDeleteMenu = true },
                        onClick = {}
                    )
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Text(
                            message.content, 
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            DropdownMenu(expanded = showDeleteMenu, onDismissRequest = { showDeleteMenu = false }) {
                DropdownMenuItem(
                    text = { Text("删除此消息") },
                    onClick = {
                        onDelete()
                        showDeleteMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            if (userAvatar != null) {
                AsyncImage(
                    model = userAvatar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), 
                    contentAlignment = Alignment.Center
                ) {
                    Text("我", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}
