package kr.sweetapps.alcoholictimer.ui.common

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.stringResource
import kr.sweetapps.alcoholictimer.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
/**
 * Reusable full-screen gallery + camera composable.
 * Provides images from MediaStore and a camera capture button.
 */
@Composable
fun CustomGalleryScreen(
    onImageSelected: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imagesState = remember { androidx.compose.runtime.mutableStateOf<List<Uri>>(emptyList()) }
    val loadingState = remember { androidx.compose.runtime.mutableStateOf(true) }

    // Load images from MediaStore asynchronously
    androidx.compose.runtime.LaunchedEffect(Unit) {
        loadingState.value = true
        imagesState.value = loadImagesFromMediaStore(context)
        loadingState.value = false
    }

    // Camera capture: create temp file Uri via MediaStore (preferred) or FileProvider fallback
    val cameraOutputUriState = remember { androidx.compose.runtime.mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success && cameraOutputUriState.value != null) {
            onImageSelected(cameraOutputUriState.value!!)
        } else {
            // nothing selected / cancelled
        }
    }

    // Prepare an output Uri for camera using MediaStore (recommended)
    val createCameraOutput: () -> Uri? = {
        try {
            val displayName = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AlcoholicTimer")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            cameraOutputUriState.value = uri
            uri
        } catch (e: Exception) {
            Log.w("CustomGallery", "Failed to create MediaStore entry: ${e.message}")
            cameraOutputUriState.value = null
            null
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text(text = stringResource(R.string.photo_selection_title)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onClose) { androidx.compose.material3.Icon(
                        Icons.Default.Close, contentDescription = "닫기") }
                },
                actions = {
                    androidx.compose.material3.IconButton(onClick = {
                        var out = createCameraOutput()
                        if (out == null) {
                            // fallback: create temp file in cache and expose via FileProvider
                            try {
                                val cacheFile = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                                if (!cacheFile.exists()) cacheFile.createNewFile()
                                out = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                cameraOutputUriState.value = out
                            } catch (e: Exception) {
                                Log.w("CustomGallery", "fallback fileprovider creation failed: ${e.message}")
                                out = null
                            }
                        }

                        if (out != null) {
                            takePictureLauncher.launch(out)
                        } else {
                            Log.w("CustomGallery", "camera output uri null, camera unavailable")
                        }
                    }) {
                        androidx.compose.material3.Icon(Icons.Default.CameraAlt, contentDescription = "카메라")
                    }
                }
            )
        }
    ) { inner ->
        val modifier = Modifier.padding(inner)
        if (loadingState.value) {
            androidx.compose.foundation.layout.Box(modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { androidx.compose.material3.Text("로딩 중...") }
        } else if (imagesState.value.isEmpty()) {
            androidx.compose.foundation.layout.Box(modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { androidx.compose.material3.Text("사진이 없습니다") }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                modifier = modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(1.dp)
            ) {
                items(imagesState.value) { uri ->
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .background(androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f)) // [NEW] 로딩 중 배경색 (2025-12-26)
                            .clickable {
                                // [NEW] Uri 권한 로그 추가 (2025-12-26)
                                Log.d("CustomGallery", "Image selected: $uri")
                                try {
                                    // Uri 영구 권한을 획득하려면 호출자 측에서 takePersistableUriPermission() 처리 필요
                                    onImageSelected(uri)
                                } catch (e: Exception) {
                                    Log.w("CustomGallery", "Failed to process selected image: ${e.message}")
                                }
                            }
                    ) {
                        // [MODIFIED] ContentScale.Crop 추가로 정사각형 격자에 이미지 꽉 차게 표시 (2025-12-26)
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop, // [NEW] 이미지 크롭 (2025-12-26)
                            error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.BrokenImage)
                        )
                    }
                }
            }
        }
    }
}

/**
 * MediaStore에서 이미지 목록을 로드합니다.
 *
 * [MODIFIED] MediaStore 쿼리 개선 (2025-12-26)
 * - projection에 DATA, DISPLAY_NAME 추가로 데이터 무결성 향상
 * - 시스템 전체 미디어를 포괄적으로 탐색
 * - 에뮬레이터 호환성 강화
 */
private suspend fun loadImagesFromMediaStore(context: Context): List<Uri> = withContext(Dispatchers.IO) {
    val list = mutableListOf<Uri>()
    try {
        // [MODIFIED] projection 확장 - DATA, DISPLAY_NAME 추가 (2025-12-26)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        // [NEW] 미디어 스캔 로그 추가 (2025-12-26)
        Log.d("CustomGallery", "Starting MediaStore query...")

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sort
        )
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dataIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val displayName = if (displayNameIndex >= 0) it.getString(displayNameIndex) else null
                val data = if (dataIndex >= 0) it.getString(dataIndex) else null

                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString())
                    .build()
                list.add(uri)

                // [NEW] 첫 5개 이미지만 상세 로그 출력 (디버깅용) (2025-12-26)
                if (list.size <= 5) {
                    Log.d("CustomGallery", "Loaded image: id=$id, name=$displayName, path=$data")
                }
            }
        }

        Log.d("CustomGallery", "MediaStore query completed: ${list.size} images found")

        // [NEW] 에뮬레이터 환경에서 이미지가 없을 경우 미디어 스캔 유도 (2025-12-26)
        if (list.isEmpty()) {
            Log.w("CustomGallery", "No images found. Media scan may be required.")
            // 참고: 개별 파일 스캔은 호출자 측에서 처리하는 것이 적절함
            // context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        }

    } catch (se: SecurityException) {
        Log.w("CustomGallery", "MediaStore query denied: ${se.message}")
    } catch (e: Exception) {
        Log.e("CustomGallery", "Failed to load images", e)
    }
    list
}
