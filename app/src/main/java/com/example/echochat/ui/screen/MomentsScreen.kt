package com.example.echochat.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.echochat.R
import com.example.echochat.data.local.dao.MomentWithAgent
import com.example.echochat.data.local.entity.UserEntity
import com.example.echochat.ui.viewmodel.MomentViewModel
import com.example.echochat.ui.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    momentViewModel: MomentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val moments by momentViewModel.allMoments.collectAsState()
    val user by userViewModel.user.collectAsState()
    var showPostDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding().height(48.dp),
                windowInsets = WindowInsets(0),
                title = { 
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        Text("朋友圈", fontSize = 18.sp) 
                    }
                },
                actions = {
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showPostDialog = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Post", modifier = Modifier.size(24.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                MomentsHeader(user)
            }

            items(moments) { moment ->
                MomentItem(
                    moment = moment, 
                    currentUser = user,
                    onDelete = { momentViewModel.deleteMoment(moment.id) },
                    onLike = { momentViewModel.likeMoment(moment.id, user?.name ?: "User") },
                    onComment = { content -> momentViewModel.commentMoment(moment.id, user?.name ?: "User", content) },
                    momentViewModel = momentViewModel
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showPostDialog) {
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text("发布动态") },
            text = {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("这一刻的想法...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        momentViewModel.postMoment(content)
                        showPostDialog = false
                    },
                    enabled = content.isNotBlank()
                ) {
                    Text("发表")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MomentsHeader(user: UserEntity?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(bottom = 40.dp)
    ) {
        // 封面图 (使用 moment 图片作为 Header 背景)
        Image(
            painter = painterResource(id = R.drawable.moment),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentScale = ContentScale.Crop
        )
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user?.name ?: "AI Explorer",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 20.dp, end = 12.dp)
            )
            Surface(
                modifier = Modifier.size(70.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                if (user?.avatar != null) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user?.name?.take(1) ?: "U", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentItem(
    moment: MomentWithAgent, 
    currentUser: UserEntity?,
    onDelete: () -> Unit,
    onLike: () -> Unit,
    onComment: (String) -> Unit,
    momentViewModel: MomentViewModel
) {
    val isUser = moment.agentId == null
    val displayName = if (isUser) (currentUser?.name ?: "我") else (moment.agentName ?: "未知")
    val avatar = if (isUser) currentUser?.avatar else moment.agentAvatar
    
    val likes by momentViewModel.getLikes(moment.id).collectAsState(initial = emptyList())
    val comments by momentViewModel.getComments(moment.id).collectAsState(initial = emptyList())
    
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var showDeleteMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onLongClick = { showDeleteMenu = true },
                onClick = {}
            )
    ) {
        // 头像
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.small,
            color = if (isUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
        ) {
            if (avatar != null) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(displayName.take(1))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = Color(0xFF576B95),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = moment.content,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMomentTime(moment.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Row {
                    IconButton(onClick = onLike, modifier = Modifier.size(24.dp)) {
                        val isLiked = likes.any { it.senderId == null }
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { showCommentInput = !showCommentInput }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Comment", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 点赞列表
            if (likes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color(0xFF576B95), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = likes.joinToString(", ") { it.senderName },
                            color = Color(0xFF576B95),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 评论列表
            if (comments.isNotEmpty()) {
                val topPadding = if (likes.isEmpty()) 8.dp else 1.dp
                Spacer(modifier = Modifier.height(topPadding))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    shape = if (likes.isEmpty()) RoundedCornerShape(4.dp) else RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        comments.forEach { comment ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "${comment.senderName}: ",
                                    color = Color(0xFF576B95),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.content,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (showCommentInput) {
                Row(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("评论", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    TextButton(onClick = {
                        if (commentText.isNotBlank()) {
                            onComment(commentText)
                            commentText = ""
                            showCommentInput = false
                        }
                    }) {
                        Text("发送")
                    }
                }
            }
        }
        
        DropdownMenu(expanded = showDeleteMenu, onDismissRequest = { showDeleteMenu = false }) {
            DropdownMenuItem(
                text = { Text("删除动态") },
                onClick = {
                    onDelete()
                    showDeleteMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }
    }
}

fun formatMomentTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000} 分钟前"
        diff < 86400_000 -> "${diff / 3600_000} 小时前"
        else -> SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
