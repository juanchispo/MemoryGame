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

class MainActivity : Activity() {

    private val EMOJIS      = listOf("🍎", "🐶", "🎸", "🚀")
    private val CARD_COUNT  = 9
    private val PAIR_EMOJIS = (EMOJIS + EMOJIS).toMutableList()

    private lateinit var cardViews:     Array<FrameLayout?>
    private lateinit var cardTextViews: Array<TextView?>
    private lateinit var cardValues:    Array<String>
    private lateinit var cardFlipped:   BooleanArray
    private lateinit var cardMatched:   BooleanArray

    private var firstFlippedIndex  = -1
    private var secondFlippedIndex = -1
    private var moves          = 0
    private var matchesFound   = 0
    private var isChecking     = false

    private lateinit var movesTextView:  TextView
    private lateinit var statusTextView: TextView
    private lateinit var gridLayout:     GridLayout

    private val colorBack        = "#1A1A2E"
    private val colorCard        = "#16213E"
    private val colorCardFlipped = "#E94560"
    private val colorCardMatched = "#0F3460"
    private val colorAccent      = "#E94560"
    private val colorText        = "#EAEAEA"
    private val colorStar        = "#FFD700"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(colorBack))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "Memory Game"
            textSize = 28f
            setTextColor(Color.parseColor(colorText))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }, lpWrap(bottomMargin = dp(8)))

        root.addView(TextView(this).apply {
            text = "Encuentra los 4 pares"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
        }, lpWrap(bottomMargin = dp(24)))

        statusTextView = TextView(this).apply {
            text = ""
            textSize = 20f
            setTextColor(Color.parseColor(colorStar))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            visibility = View.GONE
        }
        root.addView(statusTextView, lpWrap(bottomMargin = dp(12)))

        movesTextView = TextView(this).apply {
            text = "Movimientos: 0"
            textSize = 18f
            setTextColor(Color.parseColor(colorAccent))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(movesTextView, lpWrap(bottomMargin = dp(28)))

        gridLayout = GridLayout(this).apply {
            rowCount    = 3
            columnCount = 3
        }
        root.addView(gridLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.gravity     = Gravity.CENTER_HORIZONTAL
            it.bottomMargin = dp(32)
        })

        root.addView(Button(this).apply {
            text = "Reiniciar"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(colorAccent))
            setPadding(dp(48), dp(20), dp(48), dp(20))
            isAllCaps = false
            setOnClickListener { resetGame() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.gravity = Gravity.CENTER_HORIZONTAL })

        setContentView(root)
        initArrays()
        resetGame()
    }

    private fun initArrays() {
        cardViews     = arrayOfNulls(CARD_COUNT)
        cardTextViews = arrayOfNulls(CARD_COUNT)
        cardValues    = Array(CARD_COUNT) { "" }
        cardFlipped   = BooleanArray(CARD_COUNT)
        cardMatched   = BooleanArray(CARD_COUNT)
    }

    private fun resetGame() {
        moves = 0; matchesFound = 0
        firstFlippedIndex = -1; secondFlippedIndex = -1
        isChecking = false
        movesTextView.text = "Movimientos: 0"
        statusTextView.visibility = View.GONE

        val shuffled = PAIR_EMOJIS.shuffled()
        for (i in 0 until CARD_COUNT) {
            cardFlipped[i] = (i == 4)
            cardMatched[i] = (i == 4)
            cardValues[i]  = if (i == 4) "⭐" else shuffled[if (i < 4) i else i - 1]
        }
        buildGrid()
    }

    private fun buildGrid() {
        gridLayout.removeAllViews()
        val size   = dp(90)
        val margin = dp(6)

        for (i in 0 until CARD_COUNT) {
            val bgColor = when {
                cardMatched[i] -> Color.parseColor(colorStar)
                else           -> Color.parseColor(colorCard)
            }
            val cardView = FrameLayout(this).apply {
                background = roundedBg(bgColor)
            }
            val textView = TextView(this).apply {
                text     = if (cardFlipped[i]) cardValues[i] else "❓"
                textSize = if (i == 4) 34f else 30f
                gravity  = Gravity.CENTER
                setTextColor(Color.WHITE)
            }
            cardView.addView(textView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            val params = GridLayout.LayoutParams().apply {
                width = size; height = size
                setMargins(margin, margin, margin, margin)
                rowSpec    = GridLayout.spec(i / 3)
                columnSpec = GridLayout.spec(i % 3)
            }

            cardViews[i]     = cardView
            cardTextViews[i] = textView

            if (i != 4) {
                val idx = i
                cardView.setOnClickListener { onCardClick(idx) }
            }
            gridLayout.addView(cardView, params)
        }
    }

    private fun onCardClick(index: Int) {
        if (isChecking || cardFlipped[index] || cardMatched[index]) return

        flipCard(index, reveal = true)
        cardFlipped[index] = true

        if (firstFlippedIndex == -1) {
            firstFlippedIndex = index
        } else {
            secondFlippedIndex = index
            moves++
            movesTextView.text = "Movimientos: $moves"
            isChecking = true
            checkMatch()
        }
    }

    private fun checkMatch() {
        val a = firstFlippedIndex
        val b = secondFlippedIndex

        if (cardValues[a] == cardValues[b]) {
            matchesFound++
            cardMatched[a] = true; cardMatched[b] = true
            Handler(Looper.getMainLooper()).postDelayed({
                cardViews[a]?.background = roundedBg(Color.parseColor(colorCardMatched))
                cardViews[b]?.background = roundedBg(Color.parseColor(colorCardMatched))
                firstFlippedIndex = -1; secondFlippedIndex = -1; isChecking = false
                if (matchesFound == EMOJIS.size) showVictory()
            }, 400)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                flipCard(a, false); flipCard(b, false)
                cardFlipped[a] = false; cardFlipped[b] = false
                firstFlippedIndex = -1; secondFlippedIndex = -1; isChecking = false
            }, 900)
        }
    }

    private fun flipCard(index: Int, reveal: Boolean) {
        val card = cardViews[index] ?: return
        val text = cardTextViews[index] ?: return

        val out = ObjectAnimator.ofFloat(card, "scaleX", 1f, 0f).apply {
            duration = 150; interpolator = AccelerateDecelerateInterpolator()
        }
        val inn = ObjectAnimator.ofFloat(card, "scaleX", 0f, 1f).apply {
            duration = 150; interpolator = AccelerateDecelerateInterpolator()
        }
        out.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (reveal) {
                    text.text = cardValues[index]
                    card.background = roundedBg(Color.parseColor(colorCardFlipped))
                } else {
                    text.text = "❓"
                    card.background = roundedBg(Color.parseColor(colorCard))
                }
                inn.start()
            }
        })
        out.start()
    }
    private fun showVictory() {
        statusTextView.text = "🎉 ¡Ganaste en $moves movimientos!"
        statusTextView.visibility = View.VISIBLE
        val sx = ObjectAnimator.ofFloat(statusTextView, "scaleX", 0f, 1.2f, 1f).apply { duration = 500 }
        val sy = ObjectAnimator.ofFloat(statusTextView, "scaleY", 0f, 1.2f, 1f).apply { duration = 500 }
        AnimatorSet().apply { playTogether(sx, sy); start() }
    }
    private fun roundedBg(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(color)
        }

    private fun lpWrap(bottomMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = bottomMargin }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}