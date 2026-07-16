package com.sinus.pinmap.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinEditScreen(
    pinId: Long,
    lat: Double = 0.0,
    lng: Double = 0.0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val categoryRepository = remember { CategoryRepository(database.categoryStore()) }
    val templateRepo = remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val valueRepo = remember { FieldValueRepository(database.fieldValueStore()) }

    val isCreate = pinId == 0L

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf<List<FieldTemplate>>(emptyList()) }
    var fieldValues by remember { mutableStateOf<Map<Long, List<FieldValue>>>(emptyMap()) }
    var editingValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var editingImages by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }
    var playingVideo by remember { mutableStateOf<String?>(null) }
    val thumbnailCache = remember { mutableStateMapOf<String, androidx.compose.ui.graphics.ImageBitmap>() }
    var hasChanges by remember { mutableStateOf(false) }
    var pinId by remember { mutableStateOf(pinId) }

    fun markDirty() { hasChanges = true }

    LaunchedEffect(Unit) {
        categories = categoryRepository.getAllCategories().first()
        if (!isCreate) {
            pinRepository.getPinById(pinId)?.let { pin ->
                title = pin.title
                selectedCategory = categories.find { it.id == pin.categoryId }
                selectedCategory?.let { cat ->
                    templates = templateRepo.getFieldTemplatesByCategory(cat.id).first()
                }
                val values = valueRepo.getFieldValuesByPin(pinId).first()
                fieldValues = values.groupBy { it.fieldTemplateId ?: it.id }
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { cat ->
            templates = templateRepo.getFieldTemplatesByCategory(cat.id).first()
            if (isCreate) {
                fieldValues = emptyMap()
                editingValues = emptyMap()
                editingImages = emptyMap()
            }
        }
    }

    var hasImagePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
                else Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasImagePermission = it }

    var pendingImageTemplateId by remember { mutableStateOf<Long?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            pendingImageTemplateId?.let { tid ->
                scope.launch {
                    try {
                        val timeStamp = System.currentTimeMillis()
                        val ext = try {
                            context.contentResolver.getType(selectedUri)?.split("/")?.lastOrNull()?.let {
                                if (it == "*" || it.length > 4) null else ".$it"
                            } ?: ".jpg"
                        } catch (_: Exception) { ".jpg" }
                        val fileName = "IMG_${timeStamp}$ext"
                        val imagesDir = File(context.filesDir, "images")
                        if (!imagesDir.exists()) imagesDir.mkdirs()
                        val imageFile = File(imagesDir, fileName)
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            FileOutputStream(imageFile).use { output -> input.copyTo(output) }
                        }
                        val uriStr = Uri.fromFile(imageFile).toString()
                        val existing = fieldValues[tid]?.map { it.value ?: "" } ?: emptyList()
                        val current = editingImages[tid] ?: existing
                        if (uriStr !in current) {
                            editingImages = editingImages + (tid to (current + uriStr)); markDirty()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            pendingImageTemplateId = null
        }
    }

    fun getImages(template: FieldTemplate): List<String> {
        val edited = editingImages[template.id]
        if (edited != null) return edited
        return fieldValues[template.id]?.map { it.value ?: "" } ?: emptyList()
    }

    @Composable
    fun VideoThumbnail(videoUri: String, modifier: Modifier = Modifier) {
        val cached = thumbnailCache[videoUri]
        if (cached != null) {
            Image(bitmap = cached, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
        } else {
            LaunchedEffect(videoUri) {
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, Uri.parse(videoUri))
                        val frame = retriever.frameAtTime
                        retriever.release()
                        frame?.let { thumbnailCache[videoUri] = it.asImageBitmap() }
                    } catch (_: Exception) { }
                }
            }
            Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreate) "新建标记" else "编辑标记") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(value = title, onValueChange = { title = it; markDirty() }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }

                item {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("分类") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(modifier = Modifier.size(24.dp).background(Color(category.color), CircleShape))
                                        Text(category.name)
                                    }},
                                    onClick = { selectedCategory = category; expanded = false; markDirty() },
                                    leadingIcon = if (selectedCategory?.id == category.id) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                                )
                            }
                        }
                    }
                }

                if (isCreate && selectedCategory == null) {
                    item { Text("请选择一个分类", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }

                if (templates.isNotEmpty()) {
                    item { Text("字段", style = MaterialTheme.typography.titleSmall) }
                }

                itemsIndexed(templates) { index, template ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = template.fieldName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        when (template.fieldType) {
                            FieldType.TEXT -> {
                                val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                OutlinedTextField(value = v, onValueChange = { editingValues = editingValues + (template.id to it); markDirty() }, modifier = Modifier.fillMaxWidth(), minLines = 1)
                            }
                            FieldType.NUMBER -> {
                                val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                OutlinedTextField(value = v, onValueChange = { editingValues = editingValues + (template.id to it); markDirty() }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            }
                            FieldType.DATE -> {
                                val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                OutlinedTextField(value = v, onValueChange = { editingValues = editingValues + (template.id to it); markDirty() }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("YYYY-MM-DD") })
                            }
                            FieldType.IMAGE -> {
                                val images = getImages(template)
                                if (images.isNotEmpty()) {
                                    val rowState = rememberLazyListState()
                                    LaunchedEffect(images.size) {
                                        if (images.isNotEmpty()) rowState.animateScrollToItem(images.size - 1)
                                    }
                                    LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        itemsIndexed(images) { index, img ->
                                            Box(modifier = Modifier.size(120.dp).clickable {
                                                viewerImages = images; viewerStartIndex = index
                                            }) {
                                                Image(painter = coil.compose.rememberAsyncImagePainter(img), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clickable {
                                                    editingImages = editingImages + (template.id to (images - img)); markDirty()
                                                }) { Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(4.dp)) }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (hasImagePermission) {
                                            pendingImageTemplateId = template.id; imagePickerLauncher.launch("image/*")
                                        } else {
                                            imagePermissionLauncher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (images.isEmpty()) "选择图片" else "添加图片") }
                            }
                            FieldType.VIDEO -> {
                                val videos = editingImages[template.id] ?: fieldValues[template.id]?.map { it.value ?: "" } ?: emptyList()
                                if (videos.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        itemsIndexed(videos) { index, video ->
                                            Box(modifier = Modifier.size(120.dp).clickable { playingVideo = video }) {
                                                VideoThumbnail(videoUri = video, modifier = Modifier.fillMaxSize())
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.align(Alignment.Center).size(48.dp))
                                                Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clickable {
                                                    editingImages = editingImages + (template.id to (videos - video)); markDirty()
                                                }) { Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(4.dp)) }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (hasImagePermission) {
                                            pendingImageTemplateId = template.id; imagePickerLauncher.launch("video/*")
                                        } else {
                                            imagePermissionLauncher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (videos.isEmpty()) "选择视频" else "添加视频") }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(onClick = { showAddFieldDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("添加字段") }
                }
            }

            Button(
                    onClick = {
                        hasChanges = false
                        scope.launch {
                            val categoryId = selectedCategory?.id ?: return@launch
                            val id = when {
                                pinId == 0L -> {
                                    val newId = pinRepository.insertPin(Pin(latitude = lat, longitude = lng, title = title, categoryId = categoryId))
                                    pinId = newId
                                    newId
                                }
                                else -> {
                                    pinRepository.getPinById(pinId)?.let { pin ->
                                        pinRepository.updatePin(pin.copy(title = title, categoryId = categoryId))
                                    }
                                    pinId
                                }
                            }
                        val existingValues = valueRepo.getFieldValuesByPin(id).first()
                        existingValues.forEach { fv ->
                            if (fv.fieldTemplateId !in templates.map { it.id }) {
                                valueRepo.deleteFieldValueById(fv.id)
                            }
                        }
                        templates.forEach { template ->
                            when (template.fieldType) {
                                FieldType.IMAGE, FieldType.VIDEO -> {
                                    val newMedia = editingImages[template.id]
                                    if (newMedia != null) {
                                        existingValues.filter { it.fieldTemplateId == template.id }.forEach { valueRepo.deleteFieldValueById(it.id) }
                                        newMedia.forEach { uri ->
                                            valueRepo.insertFieldValue(FieldValue(pinId = id, fieldTemplateId = template.id, value = uri))
                                        }
                                    }
                                }
                                else -> {
                                    val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                    val existing = existingValues.find { it.fieldTemplateId == template.id }
                                    if (existing != null) {
                                        valueRepo.updateFieldValue(existing.copy(value = v))
                                    } else {
                                        valueRepo.insertFieldValue(FieldValue(pinId = id, fieldTemplateId = template.id, value = v))
                                    }
                                }
                            }
                        }
                        hasChanges = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                enabled = title.isNotBlank() && selectedCategory != null && hasChanges
            ) { Text("保存") }
        }
    }

    if (showAddFieldDialog) {
        CreateFieldTemplateDialog(
            onConfirm = { name, type ->
                scope.launch {
                    val catId = selectedCategory?.id ?: return@launch
                    val nextOrder = templateRepo.nextSortOrder(catId)
                    templateRepo.insertFieldTemplate(FieldTemplate(categoryId = catId, fieldName = name, fieldType = type, sortOrder = nextOrder))
                    templates = templateRepo.getFieldTemplatesByCategory(catId).first()
                }
                showAddFieldDialog = false
            },
            onDismiss = { showAddFieldDialog = false }
        )
    }

    playingVideo?.let { videoUrl ->
        Dialog(onDismissRequest = { playingVideo = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { playingVideo = null }) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(videoUrl))
                            setOnPreparedListener { it.isLooping = true; start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (viewerImages.isNotEmpty()) {
        val images = viewerImages
        val startIndex = viewerStartIndex.coerceIn(0, images.size - 1)
        val totalPages = images.size * 2001
        val initialOffset = images.size * 1000
        val pagerState = rememberPagerState(
            initialPage = startIndex + initialOffset,
            pageCount = { totalPages }
        )
        Dialog(
            onDismissRequest = { viewerImages = emptyList() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageUrl = images[page % images.size]
                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var multi = false
                                var moved = false
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        if (!multi) {
                                            multi = true
                                            event.changes.forEach { it.consume() }
                                        }
                                        val ch = event.changes
                                        val currDist = (ch[0].position - ch[1].position).getDistance()
                                        val prevDist = (ch[0].previousPosition - ch[1].previousPosition).getDistance()
                                        val zoom = if (prevDist > 0f) currDist / prevDist else 1f
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        val cx = ch.fold(0f) { a, c -> a + c.position.x } / ch.size
                                        val cy = ch.fold(0f) { a, c -> a + c.position.y } / ch.size
                                        val pcx = ch.fold(0f) { a, c -> a + c.previousPosition.x } / ch.size
                                        val pcy = ch.fold(0f) { a, c -> a + c.previousPosition.y } / ch.size
                                        offsetX = (offsetX + cx - pcx).coerceIn(-(size.width * (scale - 1)) / 2f, (size.width * (scale - 1)) / 2f)
                                        offsetY = (offsetY + cy - pcy).coerceIn(-(size.height * (scale - 1)) / 2f, (size.height * (scale - 1)) / 2f)
                                    } else if (event.type == PointerEventType.Move) {
                                        val dx = event.changes[0].position.x - event.changes[0].previousPosition.x
                                        val dy = event.changes[0].position.y - event.changes[0].previousPosition.y
                                        if (dx * dx + dy * dy > 50f) moved = true
                                    }
                                } while (event.changes.any { it.pressed })
                                if (!multi && !moved) viewerImages = emptyList()
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offsetX; translationY = offsetY
                        }
                ) {
                    Image(
                        painter = coil.compose.rememberAsyncImagePainter(imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
