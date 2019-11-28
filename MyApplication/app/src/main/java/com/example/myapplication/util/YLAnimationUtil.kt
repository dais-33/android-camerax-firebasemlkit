package com.example.myapplication.util

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.R

object YLAnimationUtil {

    /**
     * 画面遷移のアニメーション
     *
     * @param animationBackground アニメーションを行うためのView
     * @param originalView 開始時のアニメーション対象のView
     * @param listener アニメーションのリスナー
     */
    fun transitionListDetail(animationBackground: ViewGroup, originalView: View, listener: Animator.AnimatorListener? = null) {

        // アニメーションを行うBackgroundを初期化する
        animationBackground.removeAllViews()

        val location = IntArray(2)
        originalView.getLocationInWindow(location)
        // アニメーションを行うViewを初期化する
        val startView = makeAnimationView(animationBackground, originalView).apply {
            setBackgroundResource(R.drawable.anim_frame_style)
        }
        val sceneRootLoc = IntArray(2)
        animationBackground.getLocationInWindow(sceneRootLoc)

        // scalingアニメーション
        val scaleXAnimator = ObjectAnimator.ofFloat(startView, "scaleX", 1.0f, 2.0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(startView, "scaleY", 1.0f, 2.0f)
        val alphaAnimator = ObjectAnimator.ofFloat(startView, "alpha", 1.0f, 0.0f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                    listener?.onAnimationRepeat(animation)
                }

                override fun onAnimationEnd(animation: Animator?) {
                    listener?.onAnimationEnd(animation)
                    animationBackground.removeAllViews()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    listener?.onAnimationCancel(animation)
                    animationBackground.removeAllViews()
                }

                override fun onAnimationStart(animation: Animator?) {
                    listener?.onAnimationStart(animation)
                }

            })
        }.start()
    }

    private fun makeAnimationView(root: ViewGroup, originalView: View): View {
        val animView = addViewToOverlay(root, originalView.width, originalView.height)

        val originalLoc = IntArray(2)
        originalView.getLocationInWindow(originalLoc)
        val rootLoc = IntArray(2)
        root.getLocationInWindow(rootLoc)

        val startTranslationX = (originalLoc[0] - rootLoc[0]).toFloat()
        val startTranslationY = (originalLoc[1] - rootLoc[1]).toFloat()
        animView.apply {
            translationX = startTranslationX
            translationY = startTranslationY
        }
        return animView
    }

    /**
     * アニメーションを行うViewをアニメーションを表示するViewの上に置いて作成する
     *
     * @param root アニメーションを表示するView
     * @param width アニメーションを行うViewの幅
     * @param height アニメーションを行うViewの高さ
     */
    private fun addViewToOverlay(root: ViewGroup, width: Int, height: Int) =
        NoOverlapView(root.context).also {
            it.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            it.layout(0, 0, width, height)
            root.overlay.add(it)
        }

    /**
     * アニメーションを行うためのView
     *
     * @param context context
     */
    class NoOverlapView(context: Context) : View(context) {
        override fun hasOverlappingRendering() = false
    }
}