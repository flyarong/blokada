package core

import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.text.Html
import android.util.AttributeSet
import gs.presentation.doAfter
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class AInfoView(
        private val ctx: Context,
        private val attributeSet: AttributeSet
): AppCompatTextView(ctx, attributeSet) {

    var waitMillis: Int? = null

    var message = ""
        set(value) {
            field = value
            if (shown) {
                shown = false
                slideOut {
                    text = Html.fromHtml(value)
                    shown = true
                    slideIn()
                }
            } else {
                launch(UI) {
                    text = Html.fromHtml(value)
                }
                shown = true
                slideIn()
            }
            Thread.sleep(calculateWait())
        }

    var shown = true
        set(value) { when(value) {
            field -> Unit
            else -> field = value
        }}

    init {
//        movementMethod = LinkMovementMethod()
    }

    private fun slideIn(after: () -> Unit = {}) {
        if (!shown) return
        launch(UI) {
            animate().alpha(1f).doAfter { after() }
        }
    }

    private fun slideOut(after: () -> Unit = {}) {
        if (shown) return
        launch(UI) {
            animate().alpha(0f).doAfter { after() }
        }
    }

    private fun calculateWait(): Long {
        return if (waitMillis != null) waitMillis!!.toLong()
        else 2000L * text.length / 50
    }
}
