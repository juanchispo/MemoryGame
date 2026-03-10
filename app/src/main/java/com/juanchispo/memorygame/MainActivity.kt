package com.juanchispo.memorygame

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*

enum class CardState {
    HIDDEN,
    FLIPPED,
    MATCHED
}

data class Card(
    val id: Int,
    val emoji: String,
    var state: CardState
) {
    fun isClickable(): Boolean = state == CardState.HIDDEN

    fun display(): String = when (state) {
        CardState.HIDDEN  -> "\uD83D\uDC40"
        CardState.FLIPPED -> emoji
        CardState.MATCHED -> emoji
    }
}

abstract class BaseGameActivity : Activity() {

    protected var moves: Int = 0
    protected var matchesFound: Int = 0

    protected val availableEmojis: List<String> = listOf("🍎", "🐶", "🎸", "🚀")
    protected val emojiPairs: MutableList<String> = (availableEmojis + availableEmojis).toMutableList()

    abstract fun buildUI(): View
    abstract fun onGameWon()

    open fun resetCounters() {
        moves = 0
        matchesFound = 0
    }

    fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}
class MainActivity : BaseGameActivity() {

    private val CARD_COUNT: Int = 9
    internal lateinit var cards: Array<Card>

    internal var firstFlippedIndex: Int  = -1
    internal var secondFlippedIndex: Int = -1
    internal var isChecking: Boolean     = false

    internal lateinit var movesTextView:  TextView
    internal lateinit var statusTextView: TextView
    internal lateinit var gridLayout:     GridLayout

    private val colorBack        = "#1A1A2E"
    private val colorCard        = "#16213E"
    internal val colorCardFlipped = "#E94560"
    internal val colorCardMatched = "#0F3460"
    private val colorAccent      = "#E94560"
    private val colorText        = "#EAEAEA"
    private val colorStar        = "#FFD700"

    internal val cardViewMap = mutableMapOf<Int, FrameLayout>()
    internal val cardTextMap = mutableMapOf<Int, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = buildUI()
        setContentView(ui)
        resetGame()
    }
    override fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(colorBack))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.toPx(), 48.toPx(), 24.toPx(), 24.toPx())
        }

        root.addView(TextView(this).apply {
            text = "🧠 Memory Game"
            textSize = 28f
            setTextColor(Color.parseColor(colorText))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }, wrapLp(bottom = 8.toPx()))

        root.addView(TextView(this).apply {
            text = "Encuentra los ${availableEmojis.size} pares"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
        }, wrapLp(bottom = 24.toPx()))

        statusTextView = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.parseColor(colorStar))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            visibility = View.GONE
        }
        root.addView(statusTextView, wrapLp(bottom = 12.toPx()))

        movesTextView = TextView(this).apply {
            text = "Movimientos: 0"
            textSize = 18f
            setTextColor(Color.parseColor(colorAccent))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(movesTextView, wrapLp(bottom = 28.toPx()))

        gridLayout = GridLayout(this).apply {
            rowCount    = 3
            columnCount = 3
        }
        root.addView(gridLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.gravity      = Gravity.CENTER_HORIZONTAL
            it.bottomMargin = 32.toPx()
        })
        root.addView(Button(this).apply {
            text = "🔄  Reiniciar"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(colorAccent))
            setPadding(48.toPx(), 20.toPx(), 48.toPx(), 20.toPx())
            isAllCaps = false
            setOnClickListener { resetGame() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.gravity = Gravity.CENTER_HORIZONTAL })

        return root
    }

    override fun onGameWon() {
        statusTextView.text = "🎉 ¡Ganaste en $moves movimientos!"
        statusTextView.visibility = View.VISIBLE
        val sx = ObjectAnimator.ofFloat(statusTextView, "scaleX", 0f, 1.2f, 1f).apply { duration = 500 }
        val sy = ObjectAnimator.ofFloat(statusTextView, "scaleY", 0f, 1.2f, 1f).apply { duration = 500 }
        AnimatorSet().apply { playTogether(sx, sy); start() }
    }

    override fun resetCounters() {
        super.resetCounters()
        firstFlippedIndex  = -1
        secondFlippedIndex = -1
        isChecking         = false
    }

    internal fun roundedBg(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = 12.toPx().toFloat()
            setColor(color)
        }

    private fun wrapLp(bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = bottom }
}

fun MainActivity.resetGame() {
    resetCounters()
    movesTextView.text = "Movimientos: 0"
    statusTextView.visibility = android.view.View.GONE

    val shuffled: List<String> = emojiPairs.shuffled()

    cards = Array(9) { i ->
        when (i) {
            4    -> Card(id = i, emoji = "⭐", state = CardState.MATCHED)
            else -> {
                val emojiIndex = if (i < 4) i else i - 1
                Card(id = i, emoji = shuffled[emojiIndex], state = CardState.HIDDEN)
            }
        }
    }
    buildGrid()
}

fun MainActivity.buildGrid() {
    gridLayout.removeAllViews()
    cardViewMap.clear()
    cardTextMap.clear()

    val size   = 90.toPx()
    val margin = 6.toPx()

    for (i in 0 until 9) {
        val card = cards[i]

        val bgColor = when (card.state) {
            CardState.MATCHED -> android.graphics.Color.parseColor("#FFD700")
            CardState.FLIPPED -> android.graphics.Color.parseColor(colorCardFlipped)
            CardState.HIDDEN  -> android.graphics.Color.parseColor("#16213E")
        }

        val cardView = android.widget.FrameLayout(this).apply {
            background = roundedBg(bgColor)
        }

        val textView = android.widget.TextView(this).apply {
            text     = card.display()
            textSize = if (i == 4) 34f else 30f
            gravity  = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
        }

        cardView.addView(textView, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val params = android.widget.GridLayout.LayoutParams().apply {
            width = size; height = size
            setMargins(margin, margin, margin, margin)
            rowSpec    = android.widget.GridLayout.spec(i / 3)
            columnSpec = android.widget.GridLayout.spec(i % 3)
        }

        cardViewMap[i] = cardView
        cardTextMap[i] = textView

        if (card.isClickable()) {
            val idx = i
            cardView.setOnClickListener { onCardClick(idx) }
        }

        gridLayout.addView(cardView, params)
    }
}



