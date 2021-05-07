/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.wear.tiles.fitness

import androidx.core.content.ContextCompat
import androidx.wear.tiles.TileProviderService
import androidx.wear.tiles.builders.ColorBuilders.argb
import androidx.wear.tiles.builders.DimensionBuilders.*
import androidx.wear.tiles.builders.LayoutElementBuilders.*
import androidx.wear.tiles.builders.ModifiersBuilders.*
import androidx.wear.tiles.builders.ResourceBuilders.*
import androidx.wear.tiles.builders.TileBuilders.Tile
import androidx.wear.tiles.builders.TimelineBuilders.Timeline
import androidx.wear.tiles.builders.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.readers.RequestReaders.ResourcesRequest
import androidx.wear.tiles.readers.RequestReaders.TileRequest
import com.example.wear.tiles.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

// Updating this version triggers a new call to onResourcesRequest(). This is useful for dynamic
// resources, the contents of which change even though their id stays the same (e.g. a graph).
// In this sample, our resources are all fixed, so we use a constant value.
private const val RESOURCES_VERSION = "1"

private const val ID_IC_RANK_BG = "ic_rank_bg"

// dimensions
private val PROGRESS_BAR_THICKNESS = dp(6f)

/**
 * Creates a Fitness Tile, showing your progress towards a daily goal. The progress is defined
 * randomly, for demo purposes only.
 *
 * The main function, [onTileRequest], is triggered when the system calls for a tile and implements
 * ListenableFuture which allows the Tile to be returned asynchronously.
 *
 * Resources are provided with the [onResourcesRequest] method, which is triggered when the tile
 * uses an Image. This sample tile does not include any images, so the method has only a minimal
 * implementation.
 */
class FitnessTileService : TileProviderService() {
    // For coroutines, use a custom scope we can cancel when the service is destroyed
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onTileRequest(
        requestParams: TileRequest
    ): ListenableFuture<Tile> = serviceScope.future {
        val goalProgress = FitnessRepo.getGoalProgress()
        val fontStyles = FontStyles.withDeviceParameters(requestParams.deviceParameters)
        Tile.builder()
            .setResourcesVersion(RESOURCES_VERSION)
            // Creates a timeline to hold one or more tile entries for a specific time periods.
            .setTimeline(
                Timeline.builder().addTimelineEntry(
                    TimelineEntry.builder().setLayout(
                        Layout.builder().setRoot(
                            layout(goalProgress, fontStyles)
                        )
                    )
                )
            ).build()
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
            Futures.immediateFuture(
                    Resources.builder().setVersion(RESOURCES_VERSION)
                            .addIdToImageMapping(ID_IC_RANK_BG, ImageResource.builder()
                                    .setAndroidResourceByResid(AndroidImageResourceByResId.builder()
                                            .setResourceId(R.drawable.ic_rank_bg_gold)))
                            .build()
            )

    override fun onDestroy() {
        super.onDestroy()
        // Cleans up the coroutine
        serviceJob.cancel()
    }

    private fun layout(goalProgress: GoalProgress, fontStyles: FontStyles) =
        Box.builder()
            .addContent(
                Arc.builder()
                    .addContent(
                        ArcLine.builder()
                            .setLength(degrees(goalProgress.percentage * 360f))
                            .setColor(
                                argb(ContextCompat.getColor(baseContext, R.color.primary))
                            )
                            .setThickness(PROGRESS_BAR_THICKNESS)
                    )
                    .setAnchorType(ARC_ANCHOR_START)
            )
            .addContent(
                    Column.builder()
                            .addContent(
                                    Text.builder()
                                            .setText(goalProgress.current.toString())
                                            .setFontStyle(
                                                    FontStyle.builder().setSize(sp(44f)).build()
                                            )
                                            .setFontStyle(fontStyles.display2())
                            )
                            .addContent(
                                    Box.builder()
                                            .setModifiers(Modifiers.builder()
                                                    .setBackground(Background.builder()
                                                            .setColor(argb(0x4400FF00))))
                                            .setHeight(wrap())
                                            .setWidth(wrap())
                                            .addContent(
                                                    Image.builder()
                                                            .setContentScaleMode(CONTENT_SCALE_MODE_FILL_BOUNDS)
                                                            .setHeight(expand())
                                                            .setWidth(expand())
                                                            .setResourceId(ID_IC_RANK_BG)

                                            )
                                            .addContent(
                                                    Text.builder()
                                                            .setModifiers(Modifiers.builder()
                                                                    .setPadding(Padding.builder()
                                                                            .setTop(dp(8f))
                                                                            .setBottom(dp(8f))
                                                                            .setStart(dp(8f))
                                                                            .setEnd(dp(8f))))
                                                            .setText(resources.getString(R.string.goal, goalProgress.goal))
                                                            .setFontStyle(fontStyles.title3()))

                            )
            )
}
