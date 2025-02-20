package com.valdea.assistantwriter

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import com.valdea.assistantwriter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var lastClickTime: Long = 0
    private val DOUBLE_CLICK_TIME_DELTA: Long = 300

    private var isTitleVisible = true
    private var titleContainerHeight = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupScrollView()
        setupMinimapScrollView()
        setupContentEditText()
        setupFocusListeners()

        populateTestContent()

        binding.titleContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                titleContainerHeight = binding.titleContainer.height
                binding.titleContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun populateTestContent() {
        var t = ""
        for (i in 1..100) {
            t += "$i\n"
        }
        binding.contentEditText.setText(t)
        // 초기 스크롤 정보 업데이트
        binding.scrollView.post {
            updateScrollInfo(binding.scrollView, 0)
        }
    }

    private fun setupScrollView() {
        binding.scrollView.setOnScrollChangeListener(object : OnScrollChangeListener {
            override fun onScrollChange(
                v: NestedScrollView,
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                updateScrollInfo(v, scrollY)
            }
        })
    }
    private fun updateScrollInfo(v : NestedScrollView, scrollY : Int) {
        val contentHeight = v.getChildAt(0).height
        val viewportHeight = v.height
        val scrollRange = contentHeight - viewportHeight
        val scrollProgress = if(scrollRange > 0) scrollY.toFloat() / scrollRange else 0f
        binding.minimapScrollView.setScrollInfo(scrollProgress, contentHeight, viewportHeight,
            binding.contentEditText.text.toString())
    }

    private fun setupMinimapScrollView() {
        binding.minimapScrollView.setOnScrollListener { progress ->
            val scrollY = (progress * (binding.scrollView.getChildAt(0).height
                    - binding.scrollView.height)).toInt()
            binding.scrollView.scrollTo(0, scrollY)
        }
    }

    private fun setupFocusListeners() {
        binding.contentEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isTitleVisible) {
                hideTitleContainer()
            }
        }

        binding.titleEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isTitleVisible) {
                showTitleContainer()
            }
        }
    }
    private fun showTitleContainer() {
        isTitleVisible = true
        binding.titleContainer.visibility = View.VISIBLE
        binding.titleContainer.translationY = -titleContainerHeight.toFloat()

        binding.titleContainer.animate()
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                binding.titleEditText.requestFocus() // Focus를 titleEditText로 이동
            }
            .start()

        val params = binding.scrollView.layoutParams as FrameLayout.LayoutParams
        params.topMargin = titleContainerHeight
        binding.scrollView.layoutParams = params

        binding.scrollView.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }
    private fun hideTitleContainer() {
        isTitleVisible = false
        binding.titleContainer.animate()
            .translationY(-titleContainerHeight.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.titleContainer.visibility = View.GONE
                val params = binding.scrollView.layoutParams as FrameLayout.LayoutParams
                params.topMargin = 0
                binding.scrollView.layoutParams = params
            }
            .start()

        binding.scrollView.animate()
            .translationY(-titleContainerHeight.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.scrollView.translationY = 0f
            }
            .start()
    }

    private fun setupContentEditText() {
        binding.contentEditText.setOnClickListener {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                showKeyboard()
            }
            lastClickTime = clickTime
        }
        binding.contentEditText.showSoftInputOnFocus = false
    }

    private fun showKeyboard() {
        binding.contentEditText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.contentEditText, InputMethodManager.SHOW_IMPLICIT)
    }


    override fun onBackPressed() {
        if (!isTitleVisible) {
            showTitleContainer()
        } else {
            super.onBackPressed()
        }
    }
}