package com.wbrawner.skerge

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.wbrawner.skerge.ui.theme.SkergeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val darkMode = isSystemInDarkTheme()
            LaunchedEffect(darkMode) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightNavigationBars = !darkMode
            }
            val pages by viewModel.pages.collectAsState(initial = emptyList())
            val scannerUrl by viewModel.scannerUrl.collectAsState(initial = "")
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            val (loading, setLoading) = remember { mutableStateOf(false) }
            var pdfShareJob: Job? by remember { mutableStateOf(null) }
            SkergeTheme {
                ScanScreen(
                    loading = loading,
                    setLoading = { loading ->
                        if (!loading) {
                            pdfShareJob?.cancel()
                        }
                        setLoading(loading)
                    },
                    scannerUrl = scannerUrl,
                    setScannerUrl = viewModel::setScannerUrl,
                    addButtonClicked = viewModel::requestScan,
                    shareButtonClicked = {
                        pdfShareJob = coroutineScope.launch {
                            delay(10_000)
                            val file = if (pages.size == 1) {
                                pages.first().file ?: return@launch
                            } else {
                                pages.merge()
                            }
                            startActivity(file.buildShareIntent(context))
                        }
                    },
                    removePage = viewModel::removePage,
                    pages = pages
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    loading: Boolean,
    setLoading: (Boolean) -> Unit,
    scannerUrl: String,
    setScannerUrl: (String) -> Unit,
    addButtonClicked: () -> Unit,
    shareButtonClicked: () -> Unit,
    removePage: (Page) -> Unit,
    pages: List<Page>
) {
    var showScannerUrlInput by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Skerge") },
                actions = {
                    IconButton(onClick = shareButtonClicked) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showScannerUrlInput = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (pages.none { it.file == null }) {
                FloatingActionButton(onClick = addButtonClicked) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        if (pages.isEmpty()) {
            EmptyDocumentView(modifier = Modifier.padding(paddingValues))
        } else {
            PageList(
                modifier = Modifier.padding(paddingValues),
                pages = pages,
                removePage = removePage
            )
        }
        if (loading) {
            AlertDialog(
                onDismissRequest = { setLoading(false) },
                confirmButton = {
                    TextButton(onClick = { setLoading(false) }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Merging PDF...")
                        CircularProgressIndicator()
                    }
                }
            )
        } else if (showScannerUrlInput) {
            val (scannerInput, setScannerInput) = remember { mutableStateOf(scannerUrl) }
            AlertDialog(
                onDismissRequest = { showScannerUrlInput = false },
                dismissButton = {
                    TextButton(onClick = { showScannerUrlInput = false }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            setScannerUrl(scannerInput)
                            showScannerUrlInput = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                text = {
                    OutlinedTextField(
                        value = scannerInput,
                        onValueChange = setScannerInput,
                        label = {
                            Text("Scanner URL")
                        },
                        keyboardActions = KeyboardActions {
                            setScannerUrl(scannerInput)
                            showScannerUrlInput = false
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            autoCorrect = false,
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Uri
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun EmptyDocumentView(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Place your document on the scanner, then press the add button to scan. Repeat " +
                    "the process for each page you want to scan then press the Share button to " +
                    "combine the pages into a single PDF to send to another app.",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PageList(pages: List<Page>, removePage: (Page) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(items = pages) { index, page ->
            val topPadding = if (index == 0) 16.dp else 8.dp
            val bottomPadding = if (index == pages.size - 1) 16.dp else 8.dp
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = bottomPadding)
                    .aspectRatio(8.5f / 11),
            ) {
                PagePreview(page = page, removePage)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun PagePreview(page: Page, removePage: (Page) -> Unit) {
    val (pageBitmap, setPageBitmap) = remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page.file) {
        setPageBitmap(page.loadBitmap().first)
    }
    pageBitmap?.let {
        var showMenu by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            showMenu = true
                        }
                    ),
                bitmap = it.asImageBitmap(),
                contentDescription = null
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Remove") }, onClick = { removePage(page) })
            }
        }
    }
        ?: page.error?.let {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(it.localizedMessage ?: "An unknown error has occurred")
                TextButton(
                    onClick = { removePage(page) }
                ) {
                    Text("Remove page")
                }
            }
        } ?: LoadingPage()
}

@Composable
fun LoadingPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SkergeTheme {
        ScanScreen(false, {}, "", {}, {}, {}, {}, emptyList())
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingPagePreview() {
    SkergeTheme {
        PagePreview(Page()) {}
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorPagePreview() {
    SkergeTheme {
        PagePreview(Page(error = Exception("Error message here"))) {}
    }
}