package com.radiantmood.anagramanimation

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// TODO: structuring this as some sort of rememberAnagramState composable function would be cool
@OptIn(ExperimentalStdlibApi::class)
class AnagramAnimationManager(val start: String, val end: String) {
    val length = start.length
    var currentStep: Int = -1
    val currentOrder = buildList { repeat(start.length) { add(it) } }.toMutableList()
    var currentJumpingCharacterIndex = -1
    var currentDuckingCharacterIndex = -1

    init {
        stepForward()
    }

    fun stepForward() {
        currentStep++
        swapCurrentStep()
        logCurrentString()
    }

    fun swapCurrentStep() {
        increaseStepUntilInvalidCharPlacementFound()
        if (currentStep < length) {
            Log.d("araiff", "swapping!")
            val targetChar = end[currentStep]
            val startingIndex = getIndexIterator()
                .drop(currentStep)
                .indexOfFirst { originalIndex -> start[originalIndex] == targetChar } + currentStep
            currentJumpingCharacterIndex = currentStep
            currentDuckingCharacterIndex = startingIndex
            val startingValue = currentOrder[startingIndex]
            val destinationValue = currentOrder[currentStep]
            // TODO: get ducking character into its final slot as well!
            currentOrder[startingIndex] = destinationValue
            currentOrder[currentStep] = startingValue
        }
    }

    fun increaseStepUntilInvalidCharPlacementFound() {
        while (currentStep < length && end[currentStep] == start[currentOrder[currentStep]]) {
            currentStep++
        }
    }

    fun logCurrentString() {
        val string = buildString {
            getIndexIterator().forEach {
                append(start[it])
            }
        }
        // TODO: configure turning on/off logging throughout anagram code
        Log.d("araiff", "logCurrentString: $string")
        Log.d(
            "araiff",
            "jumping: ${start[currentOrder[currentJumpingCharacterIndex]]}, ducking: ${start[currentOrder[currentDuckingCharacterIndex]]}"
        )
    }

    fun getIndexIterator(): Iterable<Int> = object : Iterable<Int>, Iterator<Int> {
        var currIndex = 0

        override fun iterator(): Iterator<Int> = this

        override fun hasNext(): Boolean = (currIndex < length)

        override fun next(): Int = currentOrder[currIndex].also { currIndex++ }

    }

}

/**
 * TODO
 * - take in a single string at a time
 * - rm jumpHeight
 * - rm shouldAnimate
 * - take in a AnagramState object
 * - take in a duration that decides how long a single swap animation step should take
 */
@OptIn(ExperimentalStdlibApi::class)
@Composable
fun AnimatedAnagram(
    start: String = "@radiantmood",
    end: String = "atom@android",
    jumpHeight: Dp = 30.dp,
    shouldAnimate: Boolean = true,
) {
    val durationMultiplier = 1
    // lift char to move + create spacing for char destination
    val (currentStep, updateStep) = remember { mutableStateOf(0) }
    val anagramManager by remember { mutableStateOf(AnagramAnimationManager(start, end)) }

    // TODO: put these vertical/horizontal animating values inside of the AnagramState
    // TODO: switch vertical movement to Animatable
    var targetVerticalMovement by remember(currentStep) { mutableStateOf(0f) }
    val verticalMovement by animateFloatAsState(targetValue = targetVerticalMovement, tween(250 * durationMultiplier))

    val horizontalMovementAnimatable = remember { Animatable(0f) }
    val horizontalMovement by horizontalMovementAnimatable.asState()

    if (shouldAnimate) {
        LaunchedEffect(start + end, currentStep) {
            Log.d("araiff", "launching animation with step $currentStep")
            if (currentStep == 0) {
                delay(2000)
            }
            launch {
                horizontalMovementAnimatable.animateTo(
                    1f,
                    tween(
                        durationMillis = 500 * durationMultiplier,
                        easing = CubicBezierEasing(0.4f, 0.0f, 0.4f, 1.0f)
                    )
                )
            }
            targetVerticalMovement = 1f
            delay(275 * durationMultiplier.toLong())
            targetVerticalMovement = 0f
            delay(250 * durationMultiplier.toLong())
            anagramManager.stepForward()

            if (anagramManager.currentStep < start.length - 1) {
                updateStep(currentStep + 1)
                horizontalMovementAnimatable.snapTo(0f)

            }
        }
    }
    // TODO: how do I best respect parent layouts of my custom layout?
    // TODO: how do I best respect alignment/arrangement from parent layouts?
    Layout(content = {
        start.forEach { Text(it.toString(), style = MaterialTheme.typography.h4) }
    }, modifier = Modifier.background(Color.Red)) { measurables, constraint ->
        // TODO: calc exact jump and duck heights needed to clear characters we are pathing around
        val height = -(jumpHeight * verticalMovement).roundToPx()
        val placeables = measurables.map { it.measure(constraint) }
        val placeablesSorted = anagramManager.getIndexIterator().map { placeables[it] }

        // tween space where moving characters live between the widths of the two characters
        val jumpingPlaceable = placeablesSorted[anagramManager.currentJumpingCharacterIndex]
        val duckingPlaceable = placeablesSorted[anagramManager.currentDuckingCharacterIndex]
        val jumpPlaceHolderDelta = duckingPlaceable.width - jumpingPlaceable.width
        val duckPlaceHolderDelta = -jumpPlaceHolderDelta
        val jumpPlaceholderWidth = jumpingPlaceable.width + (jumpPlaceHolderDelta * horizontalMovement)
        val duckPlaceholderWidth = duckingPlaceable.width + (duckPlaceHolderDelta * horizontalMovement)

        // calculate x,y positions for each letter
        val xPos = IntArray(measurables.size + 1) { 0 }
        val yPos = IntArray(measurables.size + 1) { 0 }
        for (i in 1 until xPos.size) {
            val prevI = i - 1
            val placeable = placeablesSorted[prevI]
            when (prevI) {
                anagramManager.currentJumpingCharacterIndex -> {
                    yPos[prevI] = height
                    xPos[i] = (xPos[prevI] + duckPlaceholderWidth).roundToInt()
                }
                anagramManager.currentDuckingCharacterIndex -> {
                    yPos[prevI] = -height
                    xPos[i] = (xPos[prevI] + jumpPlaceholderWidth).roundToInt()
                }
                else -> {
                    xPos[i] = xPos[prevI] + placeable.width
                }
            }
        }

        // character horizontal movement
        val jumpStart = xPos[anagramManager.currentDuckingCharacterIndex]
        val jumpEnd = xPos[anagramManager.currentJumpingCharacterIndex]
        val jumpTranslation = jumpEnd - jumpStart
        val jumpInterp = (jumpStart + (jumpTranslation * horizontalMovement)).roundToInt()
        val duckInterp = (jumpEnd + ((jumpStart - jumpEnd) * horizontalMovement)).roundToInt()

        layout(constraint.minWidth, constraint.minHeight) {
            // TODO: place jump and duck placeable separately from placeablez iteration
            placeablesSorted.forEachIndexed { index, placeable ->
                when (index) {
                    anagramManager.currentJumpingCharacterIndex -> {
                        placeable.placeRelative(jumpInterp, yPos[index])
                    }
                    anagramManager.currentDuckingCharacterIndex -> {
                        placeable.placeRelative(duckInterp, yPos[index])
                    }
                    else -> {
                        placeable.placeRelative(xPos[index], yPos[index])
                    }
                }
            }
        }
    }
}