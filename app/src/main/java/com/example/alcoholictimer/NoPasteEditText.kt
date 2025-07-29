package com.example.alcoholictimer

import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText

/**
 * 붙여넣기 기능이 비활성화된 커스텀 EditText
 */
class NoPasteEditText : EditText {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        // 길게 누르기 비활성화
        isLongClickable = false

        // 텍스트 선택 비활성화
        setTextIsSelectable(false)

        // 컨텍스트 메뉴 비활성화
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        // 모든 컨텍스트 메뉴 항목(붙여넣기, 복사 등) 차단
        return false
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val connection = super.onCreateInputConnection(outAttrs)

        // IME 옵션 설정
        outAttrs.imeOptions = outAttrs.imeOptions or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN

        return connection
    }

    // 붙여넣기 방지를 위해 모든 입력 시도를 추가로 검사
    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)

        // 텍스트가 한 번에 여러 자리 추가되는 경우 (붙여넣기로 추정)
        if (lengthAfter > 1 && lengthBefore == 0) {
            val currentText = this.text.toString()
            if (start == 0 && currentText.length > 1) {
                // 마지막 숫자만 유지
                setText(currentText.substring(currentText.length - 1))
            }
        }
    }
}
