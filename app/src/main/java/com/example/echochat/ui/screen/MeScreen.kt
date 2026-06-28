package com.example.echochat.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.echochat.data.local.entity.ProviderEntity
import com.example.echochat.data.local.entity.UserEntity
import com.example.echochat.ui.viewmodel.ProviderViewModel
import com.example.echochat.ui.viewmodel.UserViewModel
import com.example.echochat.util.saveUriToInternalStorage

@Composable
fun MeScreen(
    providerViewModel: ProviderViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val providers by providerViewModel.allProviders.collectAsState()
    val isSyncingMap by providerViewModel.isSyncing.collectAsState()
    val user by userViewModel.user.collectAsState()
    
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var providerToEdit by remember { mutableStateOf<ProviderEntity?>(null) }
    var showEditUserDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 用户信息区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEditUserDialog = true }
                .padding(24.dp, 32.dp, 24.dp, 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (user?.avatar != null) {
                    AsyncImage(
                        model = user!!.avatar,
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
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user?.name ?: "AI Explorer", style = MaterialTheme.typography.headlineSmall)
                Text("微信号: AI_${user?.id ?: 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        // 供应商管理标题
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("模型供应商", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showAddProviderDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(providers) { provider ->
                ProviderItem(
                    provider = provider,
                    isSyncing = isSyncingMap[provider.id] ?: false,
                    onEdit = { providerToEdit = provider },
                    onDelete = { providerViewModel.deleteProvider(provider) },
                    onSync = { providerViewModel.syncModels(provider) }
                )
            }
        }
    }

    if (showAddProviderDialog) {
        ProviderDialog(
            onDismiss = { showAddProviderDialog = false },
            onConfirm = { name, url, key ->
                providerViewModel.addProvider(name, url, key)
                showAddProviderDialog = false
            }
        )
    }

    providerToEdit?.let { provider ->
        ProviderDialog(
            provider = provider,
            onDismiss = { providerToEdit = null },
            onConfirm = { name, url, key ->
                providerViewModel.updateProvider(provider.copy(name = name, baseUrl = url, apiKey = key))
                providerToEdit = null
            }
        )
    }

    if (showEditUserDialog) {
        UserEditDialog(
            user = user,
            onDismiss = { showEditUserDialog = false },
            onConfirm = { name, avatarUri ->
                userViewModel.updateUser(name, avatarUri)
                showEditUserDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditDialog(
    user: UserEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var avatarUri by remember { mutableStateOf(user?.avatar) }
    val context = LocalContext.current
    
    // 相册选择器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            // 将选择的图片拷贝到内部存储，解决 6 小时失效问题
            val localPath = saveUriToInternalStorage(context, it, "user_avatar_${System.currentTimeMillis()}.jpg")
            avatarUri = localPath
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人资料") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 点击头像触发选择
                Surface(
                    modifier = Modifier.size(80.dp).clickable { launcher.launch("image/*") },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                }
                Text("点击更换头像", style = MaterialTheme.typography.labelSmall)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, avatarUri) }, enabled = name.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ProviderItem(
    provider: ProviderEntity,
    isSyncing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSync: () -> Unit
) {
    ListItem(
        headlineContent = { Text(provider.name) },
        supportingContent = { Text(provider.baseUrl) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onSync) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDialog(
    provider: ProviderEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(provider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider == null) "添加供应商" else "编辑供应商") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称 (例如: OpenAI)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, baseUrl, apiKey) },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
