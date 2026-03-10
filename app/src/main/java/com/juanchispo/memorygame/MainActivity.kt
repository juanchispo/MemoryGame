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

// ╔══════════════════════════════════════════╗
//        PARTE 1 — MODELOS DE DATOS
//   Responsable: definir Card, CardState
//   y la clase base BaseGameActivity
// ╚══════════════════════════════════════════╝

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