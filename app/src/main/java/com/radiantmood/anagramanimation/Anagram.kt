package com.radiantmood.anagramanimation

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AnagramTransitionData(
    hopFraction: State<Float>,
    strafeFraction: State<Float>,
) {
    val hopFraction by hopFraction
    val strafeFraction by strafeFraction
}

@Composable
fun updateAnagramTransition(anagramManager: AnagramAnimationManager): AnagramTransitionData {
    val durationMultiplier = 1
    val stepDuration = 500L * durationMultiplier
    val strafeDuration = (1f * stepDuration).roundToInt()
    val hopDuration = (.5f * stepDuration).roundToInt()
    val fastInFastOut = CubicBezierEasing(0.4f, 0.0f, 0.4f, 1.0f)

//    val anagramManager by remember { mutableStateOf(AnagramAnimationManagerV2()) }
    var string by remember { mutableStateOf(anagramManager.currentString) }
    val strafeFraction = remember(string) { Animatable(0f) }
    val hopFraction = remember(string) { Animatable(0f) }

    LaunchedEffect(string) {
        launch {
            strafeFraction.animateTo(1f, tween(strafeDuration, easing = fastInFastOut))
        }
        launch {
            hopFraction.animateTo(1f, tween(hopDuration, easing = fastInFastOut))
        }
        launch {
            delay(hopDuration.toLong())
            hopFraction.animateTo(0f, tween(hopDuration, easing = fastInFastOut))
        }
        delay(stepDuration)
        if (!anagramManager.hasReachedEnd) {
            anagramManager.stepForward()
            string = anagramManager.currentString
        }
    }
    // TODO: why does this remember make all the difference?
    return remember(string) { AnagramTransitionData(hopFraction.asState(), strafeFraction.asState()) }
}

@OptIn(ExperimentalStdlibApi::class)
class AnagramAnimationManager(
    val start: String = "@radiantmood",
    val end: String = "atom@android",
) {
    val length = start.length
    var currentStep: Int = -1
    val currentOrder = buildList { repeat(start.length) { add(it) } }.toMutableList()
    var currentJumpingCharacterIndex = -1
    var currentDuckingCharacterIndex = -1
    var currentString = start
    var hasReachedEnd = false

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
        hasReachedEnd = currentStep + 1 >= length
        if (!hasReachedEnd) {
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

            currentString = buildString {
                getIndexIterator().forEach {
                    append(start[it])
                }
            }
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
) {
    // lift char to move + create spacing for char destination
    val anagramManager by remember { mutableStateOf(AnagramAnimationManager(start, end)) }
    val anagramTransitionData = updateAnagramTransition(anagramManager)

    Layout(content = {
        start.forEach { Text(it.toString(), style = MaterialTheme.typography.h4) }
    }, modifier = Modifier.background(Color.Red)) { measurables, constraint ->
        // TODO: calc exact jump and duck heights needed to clear characters we are pathing around
        val height = -(jumpHeight * anagramTransitionData.hopFraction).roundToPx()
        val placeables = measurables.map { it.measure(constraint) }
        val placeablesSorted = anagramManager.getIndexIterator().map { placeables[it] }

        // lerp space where moving characters live between the widths of the two characters
        val jumpingPlaceable = placeablesSorted[anagramManager.currentJumpingCharacterIndex]
        val duckingPlaceable = placeablesSorted[anagramManager.currentDuckingCharacterIndex]
        val jumpPlaceholderWidth =
            lerp(duckingPlaceable.width, jumpingPlaceable.width, anagramTransitionData.strafeFraction)
        val duckPlaceholderWidth =
            lerp(jumpingPlaceable.width, duckingPlaceable.width, anagramTransitionData.strafeFraction)

        // calculate x,y positions for each letter
        val xPos = IntArray(measurables.size + 1) { 0 } // TODO: get rid of + 1
        val yPos = IntArray(measurables.size + 1) { 0 }
        for (i in 1 until xPos.size) {
            val prevI = i - 1
            val placeable = placeablesSorted[prevI]
            when (prevI) {
                anagramManager.currentJumpingCharacterIndex -> {
                    yPos[prevI] = height
                    xPos[i] = (xPos[prevI] + jumpPlaceholderWidth)
                }
                anagramManager.currentDuckingCharacterIndex -> {
                    yPos[prevI] = -height
                    xPos[i] = (xPos[prevI] + duckPlaceholderWidth)
                }
                else -> {
                    xPos[i] = xPos[prevI] + placeable.width
                }
            }
        }

        val tallestChar: Int = placeables.maxOf { it.height } // TODO: find in above for loop
        val compWidth = xPos[xPos.lastIndex]
        val compHeight = tallestChar + ((jumpHeight.roundToPx()) * 2)
        val midHeight = compHeight / 2

        // character horizontal movement
        val jumpStart = xPos[anagramManager.currentDuckingCharacterIndex]
        val jumpEnd = xPos[anagramManager.currentJumpingCharacterIndex]
        val jumpLerp = lerp(jumpStart, jumpEnd, anagramTransitionData.strafeFraction)
        val duckLerp = lerp(jumpEnd, jumpStart, anagramTransitionData.strafeFraction)

        layout(compWidth, compHeight) {
            // TODO: place jump and duck placeable separately from placeables iteration
            placeablesSorted.forEachIndexed { index, placeable ->
                // TODO: simplify y logic to mention relative placement to be easier to read
                val y = midHeight - (placeable.height / 2)
                when (index) {
                    anagramManager.currentJumpingCharacterIndex -> placeable.placeRelative(jumpLerp, y + yPos[index])
                    anagramManager.currentDuckingCharacterIndex -> placeable.placeRelative(duckLerp, y + yPos[index])
                    else -> placeable.placeRelative(xPos[index], y + yPos[index])
                }
            }
        }
    }
}