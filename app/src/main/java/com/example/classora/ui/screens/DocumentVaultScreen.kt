package com.example.classora.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.CollegeDocument
import com.example.classora.data.DocumentCategories
import com.example.classora.data.DocumentPreferences
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentVaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val docPreferences = remember { DocumentPreferences(context) }
    val allDocuments by docPreferences.documentsFlow.collectAsState(initial = emptyList())
    val pinnedCategories by docPreferences.pinnedCategoriesFlow.collectAsState(initial = emptySet())
    
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var docTitle by remember { mutableStateOf("") }
    var docDescription by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<CollegeDocument?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    fun saveDocument() {
        val uri = selectedUri
        val category = selectedCategory
        if (uri == null || category == null || docTitle.isBlank()) {
            Toast.makeText(context, "Please fill all fields and select a file", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "*/*"
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                val fileName = "doc_${System.currentTimeMillis()}.${extension ?: "bin"}"
                
                val inputStream = contentResolver.openInputStream(uri)
                val outputFile = File(context.filesDir, fileName)
                val outputStream = FileOutputStream(outputFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                val newDoc = CollegeDocument(
                    type = category,
                    title = docTitle,
                    description = docDescription,
                    uri = Uri.fromFile(outputFile).toString(),
                    fileName = fileName,
                    mimeType = mimeType
                )
                
                docPreferences.saveDocument(newDoc)
                showUploadDialog = false
                docTitle = ""
                docDescription = ""
                selectedUri = null
                Toast.makeText(context, "Document saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sortedCategories = remember(pinnedCategories) {
        val pinned = DocumentCategories.categories.filter { it in pinnedCategories }
        val others = DocumentCategories.categories.filter { it !in pinnedCategories }
        pinned + others
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedCategory = "Other"
                    showUploadDialog = true 
                },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 8.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1724))
                }
                Text(
                    text = "College Document Vault",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1724),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Securely store and manage your college documents", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))

                sortedCategories.forEach { category ->
                    val docsInCategory = allDocuments.filter { it.type == category }
                    val isPinned = category in pinnedCategories
                    
                    DocumentCategorySection(
                        category = category,
                        documents = docsInCategory,
                        isPinned = isPinned,
                        onAddClick = {
                            selectedCategory = category
                            showUploadDialog = true
                        },
                        onDocClick = { doc ->
                            openDocument(context, doc)
                        },
                        onShareClick = { doc ->
                            shareDocument(context, doc)
                        },
                        onDeleteClick = { doc ->
                            showDeleteConfirmation = doc
                        },
                        onTogglePin = {
                            val newPinned = pinnedCategories.toMutableSet()
                            if (isPinned) {
                                newPinned.remove(category)
                            } else {
                                if (newPinned.size < 3) {
                                    newPinned.add(category)
                                } else {
                                    Toast.makeText(context, "You can only pin up to 3 categories", Toast.LENGTH_SHORT).show()
                                }
                            }
                            scope.launch { docPreferences.savePinnedCategories(newPinned) }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Upload Document") },
            text = {
                Column {
                    Text("Category: ${selectedCategory}", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = docDescription,
                        onValueChange = { docDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F7F9), contentColor = Color(0xFF0D1724)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedUri == null) "Select File/Photo" else "File Selected")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { saveDocument() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showDeleteConfirmation?.let { doc ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete '${doc.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            docPreferences.deleteDocument(doc.id)
                            val file = File(context.filesDir, doc.fileName)
                            if (file.exists()) file.delete()
                            showDeleteConfirmation = null
                            Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCategorySection(
    category: String,
    documents: List<CollegeDocument>,
    isPinned: Boolean,
    onAddClick: () -> Unit,
    onDocClick: (CollegeDocument) -> Unit,
    onShareClick: (CollegeDocument) -> Unit,
    onDeleteClick: (CollegeDocument) -> Unit,
    onTogglePin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { /* Do nothing or expand/collapse */ },
                    onLongClick = onTogglePin
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1724))
                if (isPinned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                }
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add", tint = Color(0xFF2196F3))
            }
        }

        if (documents.isEmpty()) {
            Text("No documents uploaded", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
        } else {
            documents.forEach { doc ->
                DocumentItemCard(
                    doc = doc,
                    onClick = { onDocClick(doc) },
                    onShare = { onShareClick(doc) },
                    onDelete = { onDeleteClick(doc) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DocumentItemCard(
    doc: CollegeDocument,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    doc.mimeType.contains("image") -> Icons.Default.Image
                    doc.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.Description
                }
                Icon(icon, contentDescription = null, tint = Color(0xFF2196F3))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D1724))
                if (doc.description.isNotEmpty()) {
                    Text(doc.description, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                }
            }
            Row {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp), tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

fun openDocument(context: android.content.Context, doc: CollegeDocument) {
    try {
        val file = File(context.filesDir, doc.fileName)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, doc.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareDocument(context: android.content.Context, doc: CollegeDocument) {
    try {
        val file = File(context.filesDir, doc.fileName)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = doc.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
