package com.cantrip.focusalarm

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.sqrt

// Interface to notify when the drag gesture is confirmed
interface OnConfirmListener {
    fun onConfirm(view: View)
    fun onDragStart(view: View) // Optional: Callback when drag starts
    fun onDragEnd(view: View, confirmed: Boolean) // Optional: Callback when drag ends
}

class DragToConfirmListener(
    context: Context,
    private val requiredDistanceDp: Float, // Minimum distance in DP to confirm
    private val listener: OnConfirmListener
) : View.OnTouchListener {

    // Convert DP to pixels once
    private val requiredDistancePx: Float = requiredDistanceDp * context.resources.displayMetrics.density
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop // System defined min drag distance

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var isDragging: Boolean = false
    private var initialViewX: Float = 0f // Store original view position if needed (relative to parent)
    private var initialViewY: Float = 0f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX // Use rawX/Y for screen coordinates
                startY = event.rawY
                initialViewX = view.x // View position relative to parent
                initialViewY = view.y
                isDragging = false // Reset dragging state
                // Don't consume event yet, allow parent to handle if needed (e.g., scrolling)
                // Let's consume it to prevent standard button click behavior
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val currentX = event.rawX
                val currentY = event.rawY
                val deltaX = currentX - startX
                val deltaY = currentY - startY
                val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                if (!isDragging && distance > touchSlop) {
                    // Start dragging only if moved beyond system touch slop
                    isDragging = true
                    // Apply haptic feedback or visual cue for drag start
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) // Example feedback
                    listener.onDragStart(view) // Notify listener drag started
                }

                if (isDragging) {
                    // Update the view's position using translation
                    view.translationX = deltaX
                    view.translationY = deltaY
                    // Ensure the view stays interactive by returning true
                    return true
                }
                // If not dragging yet (still within slop), don't consume
                // return false // Let's consume to prevent clicks during small moves
                return true // Consume to prevent accidental clicks while pressing down
            }

            MotionEvent.ACTION_UP -> {
                var confirmed = false
                if (isDragging) {
                    val finalX = event.rawX
                    val finalY = event.rawY
                    val totalDeltaX = finalX - startX
                    val totalDeltaY = finalY - startY
                    val totalDistance = sqrt(totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY)

                    // Check if required distance met
                    if (totalDistance >= requiredDistancePx) {
                        listener.onConfirm(view) // Action confirmed!
                        confirmed = true
                    }

                    // Reset view position smoothly (optional animation)
                    view.animate().translationX(0f).translationY(0f).setDuration(150).start()
                    listener.onDragEnd(view, confirmed) // Notify listener drag ended

                } else {
                    // If no drag occurred (just a tap), do nothing or handle as tap?
                    // For now, do nothing on simple tap. Reset just in case.
                    view.translationX = 0f
                    view.translationY = 0f
                    // listener.onDragEnd(view, false) // Optional: Notify tap ended without confirm
                }
                isDragging = false
                return true // Event handled
            }

            MotionEvent.ACTION_CANCEL -> {
                // Handle cancellation (e.g., if touch moves outside view bounds or system interruption)
                if(isDragging) {
                    view.animate().translationX(0f).translationY(0f).setDuration(150).start()
                    listener.onDragEnd(view, false) // Drag ended without confirm
                }
                isDragging = false
                return true
            }
        }
        return false // Should not happen if ACTION_DOWN returned true
    }
}