package com.wbrawner.skerge

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wbrawner.skerge.ui.theme.SkergeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val scansDirectory: File by lazy { File(cacheDir, "scans") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkergeTheme {
                ScanScreen(
                    addButtonClicked = { viewModel.requestScan(scansDirectory) },
                    shareButtonClicked = { sharePdf(viewModel.pages.replayCache.first()) },
                    removePage = viewModel::removePage,
                    pagesFlow = viewModel.pages
                )
            }
        }
    }

    private fun sharePdf(pages: List<Page>) {
        // TODO: Show loading dialog for this
        lifecycleScope.launch {
            val file = if (pages.size == 1) {
                pages.first().file ?: return@launch
            } else {
                pages.merge()
            }
            startActivity(file.buildShareIntent(this@MainActivity))
        }
    }
}

@Composable
fun ScanScreen(
    addButtonClicked: () -> Unit,
    shareButtonClicked: () -> Unit,
    removePage: (Page) -> Unit,
    pagesFlow: SharedFlow<List<Page>>
) {
    val pages = pagesFlow.collectAsState(initial = emptyList())
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Skerge") },
                actions = {
                    IconButton(onClick = shareButtonClicked) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        floatingActionButton = {
            if (pages.value.none { it.file == null }) {
                FloatingActionButton(onClick = addButtonClicked) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) {
        if (pages.value.isEmpty()) {
            EmptyDocumentView()
        } else {
            PageList(pages.value, removePage)
        }
    }
}

@Composable
fun EmptyDocumentView() {
    Column(
        modifier = Modifier
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
fun PageList(pages: List<Page>, removePage: (Page) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun PagePreview(page: Page, removePage: (Page) -> Unit) {
    val (pageBitmap, setPageBitmap) = remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page.file) {
        setPageBitmap(page.loadBitmap().first)
    }
    pageBitmap?.let {
        Image(
            modifier = Modifier.fillMaxSize(),
            bitmap = it.asImageBitmap(),
            contentDescription = null
        )
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
        ScanScreen({}, {}, {}, MutableSharedFlow())
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