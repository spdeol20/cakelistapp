package com.example.cakelistapp.ui.animation

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Shared tuning values so entrance animations feel the same across screens. */
object EntranceAnimationDefaults {
    val FallDistance: Dp = 32.dp
    val Easing: Easing = FastOutSlowInEasing
    const val DurationMillis: Int = 420
    const val StaggerMillis: Long = 65L

    /**
     * Length of the stagger wave in items. Items past this share the last slot, so anything the
     * user scrolls to later animates immediately rather than waiting out a delay that would read
     * as slow loading.
     */
    const val StaggeredItemCount: Int = 8
}

/**
 * Records which items have already played their entrance, so it is not replayed when an item is
 * recomposed, scrolled back into view, or restored after a configuration change.
 */
@Stable
class EntranceAnimationState internal constructor(
    playedKeys: Set<String> = emptySet(),
) {
    private val playedKeys: MutableSet<String> = playedKeys.toMutableSet()
    private var waveStart: Long = NO_WAVE

    internal fun hasPlayed(key: String): Boolean = key in playedKeys

    internal fun markPlayed(key: String) {
        playedKeys += key
    }

    /**
     * Timestamp the current wave began, established by whichever item animates first. Every item
     * offsets from this shared instant so the sequence holds even when they compose in different
     * frames.
     */
    internal fun waveStartMillis(): Long {
        if (waveStart == NO_WAVE) {
            waveStart = SystemClock.uptimeMillis()
        }
        return waveStart
    }

    /** Lets the entrance play again, for example after the underlying data set is replaced. */
    fun reset() {
        playedKeys.clear()
        waveStart = NO_WAVE
    }

    internal companion object {
        private const val NO_WAVE = 0L

        val Saver = listSaver<EntranceAnimationState, String>(
            save = { state -> state.playedKeys.toList() },
            restore = { saved -> EntranceAnimationState(saved.toSet()) },
        )
    }
}

@Composable
fun rememberEntranceAnimationState(): EntranceAnimationState {
    return rememberSaveable(saver = EntranceAnimationState.Saver) { EntranceAnimationState() }
}

/**
 * Fades content in and drops it into place the first time [itemKey] is shown.
 *
 * The animated value is read inside the [graphicsLayer] lambda rather than in composition, which
 * confines each frame to the draw phase. Reading it in composition (for example via
 * `Modifier.alpha` or `Modifier.offset`) would recompose and re-layout every visible item on every
 * frame, which is what usually makes list entrances stutter.
 *
 * @param itemKey stable identity of the item; use the same key the list is keyed by.
 * @param index position used to stagger the first [staggeredItemCount] items. Leave at 0 for
 *   content that should animate immediately.
 */
@Composable
fun Modifier.entranceAnimation(
    itemKey: String,
    state: EntranceAnimationState,
    index: Int = 0,
    durationMillis: Int = EntranceAnimationDefaults.DurationMillis,
    staggerMillis: Long = EntranceAnimationDefaults.StaggerMillis,
    staggeredItemCount: Int = EntranceAnimationDefaults.StaggeredItemCount,
    fallDistance: Dp = EntranceAnimationDefaults.FallDistance,
    easing: Easing = EntranceAnimationDefaults.Easing,
): Modifier {
    val isFirstAppearance = remember(itemKey) { !state.hasPlayed(itemKey) }
    val progress = remember(itemKey) { Animatable(if (isFirstAppearance) 0f else 1f) }

    LaunchedEffect(itemKey) {
        state.markPlayed(itemKey)
        if (!isFirstAppearance) return@LaunchedEffect
        // Offset from when the wave started rather than from this item's own composition. Items
        // do not all compose in the same frame, so a per-item delay falls out of sequence as soon
        // as the main thread is busy. Capping the index also means an item composed long after
        // the wave (because the user scrolled to it) has a start time in the past and animates
        // immediately instead of waiting.
        val startAt = state.waveStartMillis() +
            index.coerceAtMost(staggeredItemCount) * staggerMillis
        val wait = startAt - SystemClock.uptimeMillis()
        if (wait > 0) {
            delay(wait)
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = easing),
        )
    }

    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * fallDistance.toPx()
    }
}
