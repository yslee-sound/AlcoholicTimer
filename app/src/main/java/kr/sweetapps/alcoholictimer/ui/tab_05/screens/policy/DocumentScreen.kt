package kr.sweetapps.alcoholictimer.ui.tab_05.screens.policy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar

// 간단한 블록 표현
private sealed class MDBlock {
    data class Heading(val level: Int, val text: String) : MDBlock()
    data class Paragraph(val text: String) : MDBlock()
    data class ListBlock(val items: List<String>, val ordered: Boolean) : MDBlock()
    data class CodeBlock(val code: String) : MDBlock()
}

// 인라인 마크다운 -> AnnotatedString (링크에 "URL" 태그 추가)
private fun inlineMarkdownToAnnotated(text: String): AnnotatedString {
    val builder = buildAnnotatedString {
        var remaining = text
        // 링크 먼저 처리: [label](url)
        val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
        var indexOffset = 0
        var lastIndex = 0
        val matches = linkRegex.findAll(remaining).toList()
        if (matches.isEmpty()) {
            // 단순 처리: bold/italic/code
            appendInlineStyles(this, remaining)
        } else {
            var cursor = 0
            for (m in matches) {
                val before = remaining.substring(cursor, m.range.first)
                appendInlineStyles(this, before)
                val label = m.groups[1]?.value ?: ""
                val url = m.groups[2]?.value ?: ""
                val start = this.length
                appendInlineStyles(this, label)
                val end = this.length
                addStringAnnotation(tag = "URL", annotation = url, start = start, end = end)
                // apply link style
                addStyle(style = SpanStyle(color = Color(0xFF1E88E5).toArgbColor(), textDecoration = TextDecoration.Underline), start = start, end = end)
                cursor = m.range.last + 1
            }
            // remainder
            if (cursor < remaining.length) {
                appendInlineStyles(this, remaining.substring(cursor))
            }
        }
    }
    return builder
}

// helper to append with bold/italic/inline code styles
private fun appendInlineStyles(builder: AnnotatedString.Builder, text: String) {
    // naive processing for **bold**, *italic*, `code`
    var t = text
    val codeRegex = Regex("`([^`]+)`")
    var cursor = 0
    val codeMatches = codeRegex.findAll(t).toList()
    if (codeMatches.isEmpty()) {
        // no code, handle bold/italic
        var s = t
        // bold
        val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
        var last = 0
        val bmatches = boldRegex.findAll(s).toList()
        if (bmatches.isEmpty()) {
            // italic
            val italicRegex = Regex("\\*([^*]+)\\*")
            var cur = 0
            val imatches = italicRegex.findAll(s).toList()
            if (imatches.isEmpty()) {
                builder.append(s)
            } else {
                for (m in imatches) {
                    val before = s.substring(cur, m.range.first)
                    builder.append(before)
                    val content = m.groups[1]?.value ?: ""
                    builder.withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(content)
                    }
                    cur = m.range.last + 1
                }
                if (cur < s.length) builder.append(s.substring(cur))
            }
        } else {
            var cur = 0
            for (m in bmatches) {
                val before = s.substring(cur, m.range.first)
                builder.append(before)
                val content = m.groups[1]?.value ?: ""
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(content)
                }
                cur = m.range.last + 1
            }
            if (cur < s.length) builder.append(s.substring(cur))
        }
    } else {
        var cur = 0
        for (m in codeMatches) {
            val before = t.substring(cur, m.range.first)
            appendInlineStyles(builder, before) // recursive for bold/italic in before
            val codeContent = m.groups[1]?.value ?: ""
            val start = builder.length
            builder.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFEFEFEF))) {
                append(codeContent)
            }
            cur = m.range.last + 1
        }
        if (cur < t.length) appendInlineStyles(builder, t.substring(cur))
    }
}

// 확장 함수: convert compose Color to ARGB int style helper
private fun Color.toArgbColor(): androidx.compose.ui.graphics.Color = this

// Markdown 블록 파서 (간단)
private fun parseMarkdownBlocks(text: String): List<MDBlock> {
    val lines = text.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MDBlock>()
    var i = 0
    var codeMode = false
    val codeBuf = StringBuilder()
    val listBuf = mutableListOf<String>()
    var listOrdered = false
    val paraBuf = StringBuilder()

    fun flushParagraph() {
        val p = paraBuf.toString().trimEnd()
        if (p.isNotEmpty()) blocks.add(MDBlock.Paragraph(p))
        paraBuf.clear()
    }

    fun flushList() {
        if (listBuf.isNotEmpty()) {
            blocks.add(MDBlock.ListBlock(listBuf.toList(), listOrdered))
            listBuf.clear()
            listOrdered = false
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trim().startsWith("```") ) {
            if (!codeMode) {
                // start code
                flushParagraph()
                flushList()
                codeMode = true
                codeBuf.clear()
            } else {
                // end code
                blocks.add(MDBlock.CodeBlock(codeBuf.toString()))
                codeMode = false
                codeBuf.clear()
            }
            i++
            continue
        }
        if (codeMode) {
            codeBuf.append(line).append('\n')
            i++
            continue
        }

        // headings
        val headingMatch = Regex("^(#{1,6})\\s+(.*)").find(line)
        if (headingMatch != null) {
            flushParagraph()
            flushList()
            val level = headingMatch.groups[1]!!.value.length
            val textH = headingMatch.groups[2]!!.value
            blocks.add(MDBlock.Heading(level, textH))
            i++
            continue
        }

        // list items
        val ulMatch = Regex("^\\s*[-*]\\s+(.*)").find(line)
        val olMatch = Regex("^\\s*([0-9]+)\\.\\s+(.*)").find(line)
        if (ulMatch != null || olMatch != null) {
            if (paraBuf.isNotEmpty()) {
                flushParagraph()
            }
            listOrdered = olMatch != null
            val itemText = ulMatch?.groups?.get(1)?.value ?: olMatch?.groups?.get(2)?.value ?: ""
            listBuf.add(itemText)
            // consume following lines that are indented as continuation
            i++
            while (i < lines.size && lines[i].startsWith("    ")) {
                listBuf[listBuf.size -1] = listBuf.last() + "\n" + lines[i].trimStart()
                i++
            }
            // if next line isn't list, flush? we'll flush when next non-list element encountered
            // lookahead: if next line isn't a list item, flush
            if (i >= lines.size || !(Regex("^\\s*[-*]\\s+.*").containsMatchIn(lines[i]) || Regex("^\\s*[0-9]+\\.\\s+.*").containsMatchIn(lines[i]))) {
                flushList()
            }
            continue
        }

        if (line.isBlank()) {
            flushParagraph()
            flushList()
            i++
            continue
        }

        // accumulate paragraph
        paraBuf.append(line).append('\n')
        i++
    }
    // end
    if (codeMode) {
        // unclosed code block: flush as code
        blocks.add(MDBlock.CodeBlock(codeBuf.toString()))
    }
    flushParagraph()
    flushList()
    return blocks
}

@Composable
private fun RenderMarkdownContent(content: String) {
    val context = LocalContext.current
    val blocks = remember(content) { parseMarkdownBlocks(content) }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
        .verticalScroll(scrollState)
        .padding(16.dp)) {
        for (block in blocks) {
            when (block) {
                is MDBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> 22.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        else -> 16.sp
                    }
                    Text(
                        text = block.text,
                        fontSize = size,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                is MDBlock.Paragraph -> {
                    val annotated = inlineMarkdownToAnnotated(block.text.trim())
                    ClickableText(
                        text = annotated,
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { span ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(span.item))
                                    context.startActivity(intent)
                                }
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 14.sp)
                    )
                }
                is MDBlock.ListBlock -> {
                    for ((idx, item) in block.items.withIndex()) {
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(text = if (block.ordered) "${idx+1}." else "•", modifier = Modifier.width(24.dp))
                            val annotated = inlineMarkdownToAnnotated(item.trim())
                            ClickableText(
                                text = annotated,
                                onClick = { offset ->
                                    annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { span ->
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(span.item))
                                            context.startActivity(intent)
                                        }
                                },
                                style = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 14.sp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(bottom = 6.dp))
                }
                is MDBlock.CodeBlock -> {
                    Text(
                        text = block.code.trimEnd(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
fun DocumentScreen(
    resName: String,
    viewModel: DocumentViewModel = viewModel(),
    onBack: () -> Unit = {},
    titleResId: Int? = null,
    title: String? = null
) {
    val context = LocalContext.current
    LaunchedEffect(resName) { viewModel.load(context, resName) }
    val content = viewModel.content

    Column(modifier = Modifier.fillMaxSize()) {
        // titleResId가 주어지면 리소스 우선 사용, 아니면 전달된 문자열 또는 기본값 사용
        val titleText = when {
            titleResId != null -> stringResource(titleResId)
            title != null -> title
            else -> stringResource(R.string.dialog_view_details) // 안전한 기본값
        }
        BackTopBar(title = titleText, onBack = onBack)

        if (content == null) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
        } else {
            RenderMarkdownContent(content)
        }
    }
}
