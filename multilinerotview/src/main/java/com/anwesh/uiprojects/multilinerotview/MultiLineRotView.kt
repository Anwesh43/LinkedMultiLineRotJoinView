package com.anwesh.uiprojects.multilinerotview

/**
 * Created by anweshmishra on 06/07/19.
 */


import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas

val nodes : Int = 5
val lines : Int = 6
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#311B92")
val backColor : Int = Color.parseColor("#BDBDBD")
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val totalDeg : Float = 360f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawRotLine(deg : Float, size : Float, paint : Paint) {
    save()
    rotate(deg)
    drawLine(0f, 0f, 0f, -size, paint)
    restore()
}

fun Canvas.drawJointLine(i : Int, sc : Float, size : Float, paint : Paint) {
    val sweepDeg : Float = totalDeg / lines
    val sci : Float = sc.divideScale(i, lines)
    val x1 : Float = size * Math.cos(-90 + sweepDeg * i * Math.PI / 180).toFloat()
    val y1 : Float = size * Math.sin(-90 + sweepDeg * i * Math.PI / 180).toFloat()
    val x2 : Float = size * Math.cos(-90 + sweepDeg * (i + 1) * Math.PI / 180).toFloat()
    val y2 : Float = size * Math.sin(-90 + sweepDeg * (i + 1) * Math.PI / 180).toFloat()
    save()
    drawLine(x1, y1, x1 + (x2 - x1) * sci, y1 + (y2 - y1) * sci, paint)
    restore()
}

fun Canvas.drawJointLines(sc : Float, size : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        drawJointLine(j, sc, size, paint)
    }
}

fun Canvas.drawMultiRotLine(sc : Float, size : Float, paint : Paint) {
    val sweepDeg : Float = (totalDeg) / lines
    var deg : Float = 0f
    for (j in 0..(lines - 1)) {
        deg += sweepDeg * sc.divideScale(j, lines)
        drawRotLine(deg, size, paint)
    }
}

fun Canvas.drawMLRNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    save()
    translate(w / 2, gap * (i + 1))
    drawMultiRotLine(sc2, size, paint)
    drawJointLines(sc1, size, paint)
    restore()
}

class MultiLineRotView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, lines)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class MLRNode(var i : Int, val state : State = State()) {

        private var next : MLRNode? = null
        private var prev : MLRNode? = null

        init {

        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = MLRNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawMLRNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : MLRNode {
            var curr : MLRNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class MultiLineRot(var i : Int) {

        private val root : MLRNode = MLRNode(0)
        private var curr : MLRNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }



    data class Renderer(var view : MultiLineRotView) {

        private val animator : Animator = Animator(view)
        private val mlr : MultiLineRot = MultiLineRot(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            mlr.draw(canvas, paint)
            animator.animate {
                mlr.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            mlr.startUpdating {
                animator.start()
            }
        }
    }
}
