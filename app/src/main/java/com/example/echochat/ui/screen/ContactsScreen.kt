package com.example.echochat.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.echochat.R
import com.example.echochat.data.local.entity.AgentEntity
import com.example.echochat.data.local.entity.MemoryEntity
import com.example.echochat.data.local.entity.ProviderEntity
import com.example.echochat.ui.viewmodel.AgentViewModel
import com.example.echochat.util.saveUriToInternalStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onAgentClick: (Long) -> Unit,
    viewModel: AgentViewModel = hiltViewModel()
) {
    val agents by viewModel.allAgents.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showMemoryDialogByAgentId by remember { mutableStateOf<Long?>(null) }
    var agentToEdit by remember { mutableStateOf<AgentEntity?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 3.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                TopAppBar(
                    modifier = Modifier.height(48.dp),
                    windowInsets = WindowInsets(0),
                    title = { 
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                            Text("联系人", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium) 
                        }
                    },
                    actions = {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showGroupDialog = true }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = "发起群聊", modifier = Modifier.size(24.dp))
                                }
                                IconButton(onClick = {
                                    agentToEdit = null
                                    showEditDialog = true
                                }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "添加助手", modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(agents) { agent ->
                    AgentItem(
                        agent = agent,
                        onClick = { onAgentClick(agent.id) },
                        onEdit = { 
                            agentToEdit = agent
                            showEditDialog = true 
                        },
                        onDelete = { viewModel.deleteAgent(agent) },
                        onManageMemory = {
                            showMemoryDialogByAgentId = agent.id
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AgentEditDialog(
            agent = agentToEdit,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false }
        )
    }

    if (showGroupDialog) {
        GroupCreateDialog(
            agents = agents,
            onCreate = { name, selectedIds ->
                viewModel.createGroup(name, selectedIds)
                showGroupDialog = false
            },
            onDismiss = { showGroupDialog = false }
        )
    }

    showMemoryDialogByAgentId?.let { agentId ->
        val agent = agents.find { it.id == agentId }
        if (agent != null) {
            MemoryManagementDialog(
                agent = agent,
                viewModel = viewModel,
                onDismiss = { showMemoryDialogByAgentId = null }
            )
        }
    }
}

@Composable
fun AgentItem(
    agent: AgentEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageMemory: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(agent.name) },
        supportingContent = { Text(agent.description, maxLines = 1) },
        leadingContent = {
            AsyncImage(
                model = agent.avatar ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${agent.name}",
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("管理记忆") }, onClick = { showMenu = false; onManageMemory() }, leadingIcon = { Icon(Icons.Default.Psychology, null) })
                    DropdownMenuItem(text = { Text("编辑信息") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("删除助手") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryManagementDialog(
    agent: AgentEntity,
    viewModel: AgentViewModel,
    onDismiss: () -> Unit
) {
    val memories by viewModel.getMemories(agent.id).collectAsState(initial = emptyList())
    var editingMemory by remember { mutableStateOf<MemoryEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${agent.name} 的长期记忆") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (memories.isEmpty()) {
                    Text("暂无记忆", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn {
                        items(memories) { memory ->
                            ListItem(
                                headlineContent = { Text(memory.content, style = MaterialTheme.typography.bodyMedium) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { editingMemory = memory }) { Icon(Icons.Default.Edit, "编辑", modifier = Modifier.size(20.dp)) }
                                        IconButton(onClick = { viewModel.deleteMemory(memory) }) { Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(20.dp)) }
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    editingMemory?.let { memory ->
        var content by remember { mutableStateOf(memory.content) }
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("修改记忆") },
            text = {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateMemory(memory.copy(content = content))
                    editingMemory = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingMemory = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentEditDialog(
    agent: AgentEntity? = null,
    viewModel: AgentViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(agent?.name ?: "") }
    var avatar by remember { mutableStateOf(agent?.avatar ?: "") }
    var chatBackground by remember { mutableStateOf(agent?.chatBackground ?: "") }
    var description by remember { mutableStateOf(agent?.description ?: "") }
    var systemPrompt by remember { mutableStateOf(agent?.systemPrompt ?: "") }

    val providers by viewModel.allProviders.collectAsState()
    var selectedProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var modelName by remember { mutableStateOf("") }
    var providerExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            val localPath = saveUriToInternalStorage(context, it, "agent_avatar_${System.currentTimeMillis()}.jpg")
            avatar = localPath ?: it.toString()
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            val localPath = saveUriToInternalStorage(context, it, "chat_bg_${System.currentTimeMillis()}.jpg")
            chatBackground = localPath ?: it.toString()
        }
    }

    LaunchedEffect(agent) {
        if (agent != null) {
            viewModel.getModelInfo(agent.modelId)?.let { (provider, mName) ->
                selectedProvider = provider
                modelName = mName
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (agent == null) "创建助手" else "编辑助手") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { avatarLauncher.launch("image/*") }
                ) {
                    AsyncImage(
                        model = if (avatar.isBlank()) "https://api.dicebear.com/7.x/bottts/svg?seed=$name" else avatar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("助手头像", style = MaterialTheme.typography.titleSmall)
                        Text("点击从相册选择", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedCard(
                    onClick = { backgroundLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                        if (chatBackground.isNotBlank()) {
                            AsyncImage(
                                model = chatBackground,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("点击设置聊天背景图", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名字") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("一句话介绍") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = systemPrompt, onValueChange = { systemPrompt = it }, label = { Text("人格设定 (System Prompt)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Box {
                    OutlinedTextField(
                        value = selectedProvider?.name ?: "选择供应商",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("供应商") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { providerExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                        providers.forEach { provider ->
                            DropdownMenuItem(text = { Text(provider.name) }, onClick = { selectedProvider = provider; providerExpanded = false })
                        }
                    }
                }

                OutlinedTextField(value = modelName, onValueChange = { modelName = it }, label = { Text("模型标识 (如: gpt-4o)") }, modifier = Modifier.fillMaxWidth(), enabled = selectedProvider != null)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProvider?.let { provider ->
                        viewModel.saveAgent(
                            id = agent?.id ?: 0L, 
                            name = name, 
                            avatar = if (avatar.isBlank()) null else avatar, 
                            chatBackground = if (chatBackground.isBlank()) null else chatBackground,
                            description = description, 
                            systemPrompt = systemPrompt, 
                            providerId = provider.id, 
                            modelName = modelName
                        )
                    }
                    onDismiss()
                },
                enabled = name.isNotBlank() && selectedProvider != null && modelName.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreateDialog(
    agents: List<AgentEntity>,
    onCreate: (String, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发起群聊") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("群聊名称") }, modifier = Modifier.fillMaxWidth())
                Text("选择成员", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.heightIn(max = 240.dp)) {
                    LazyColumn {
                        items(agents) { agent ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (selectedIds.contains(agent.id)) selectedIds.remove(agent.id) else selectedIds.add(agent.id)
                                }.padding(vertical = 4.dp)
                            ) {
                                Checkbox(checked = selectedIds.contains(agent.id), onCheckedChange = { if (it) selectedIds.add(agent.id) else selectedIds.remove(agent.id) })
                                Text(agent.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(groupName, selectedIds.toList()) }, enabled = groupName.isNotBlank() && selectedIds.isNotEmpty()) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
