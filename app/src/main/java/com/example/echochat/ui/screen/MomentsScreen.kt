package com.example.echochat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
                title = { Text("朋友圈") },
                actions = {
                    IconButton(onClick = { showPostDialog = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            item {
                MomentsHeader(user)
            }

            items(moments) { moment ->
                MomentItem(moment, user)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
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
        // 背景封面 (暂用灰色)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF333333))
        )
        
        // 用户信息 (右下角)
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
                color = Color.LightGray,
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

@Composable
fun MomentItem(moment: MomentWithAgent, currentUser: UserEntity?) {
    val isUser = moment.agentId == null
    val displayName = if (isUser) (currentUser?.name ?: "我") else (moment.agentName ?: "未知")
    val avatar = if (isUser) currentUser?.avatar else moment.agentAvatar
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatMomentTime(moment.createdAt),
                fontSize = 12.sp,
                color = Color.Gray
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
