[![Maven Central](https://img.shields.io/maven-central/v/ovh.plrapps/mapcompose)](https://central.sonatype.com/artifact/ovh.plrapps/mapcompose)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![](https://img.shields.io/badge/ComposeBOM-2025.10.01-brightgreen)](https://developer.android.com/jetpack/compose/bom/bom)

🎉 News:
- `3.1.0` now supports infinite scroll (#119).
- `3.0.0` is released. The library is now capable of handling much bigger maps, such as world-wide
OpenStreetMap at zoom level 17 with a maximum scale of 8. See [Migrate from 2.x.x](#migrate-from-2xx).
- Memory footprint has been dramatically reduced on Android 10 and above, by leveraging [Hardware Bitmaps](https://bumptech.github.io/glide/doc/hardwarebitmaps.html).
- MapCompose Multiplatform is officially released: https://github.com/p-lr/MapComposeMP \
  Works on iOS, MacOS, Windows, Linux, and Android.

# MapCompose

MapCompose is a fast, memory efficient Jetpack compose library to display tiled maps with minimal effort.
It shows the visible part of a tiled map with support of markers and paths, and various gestures
(flinging, dragging, scaling, and rotating).

An example of setting up:

```kotlin
/* Inside your view-model */
val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
    FileInputStream(File("path/{$zoomLvl}/{$row}/{$col}.jpg")) // or it can be a remote HTTP fetch
}

val state = MapState(4, 4096, 4096).apply {
    addLayer(tileStreamProvider)
    enableRotation()
}

/* Inside a composable */
@Composable
fun MapContainer(
    modifier: Modifier = Modifier, viewModel: YourViewModel
) {
    MapUI(modifier, state = viewModel.state)
}
```

This project holds the source code of this library, plus a demo app - which is useful to get started.
To test the demo, just clone the repo and launch the demo app from Android Studio.

## Clustering

Marker clustering regroups markers of close proximity into clusters. The video below shows how it works.

https://github.com/p-lr/MapCompose/assets/15638794/de48cb1b-396b-44d3-b47a-e3d719e8f38a

The sample below shows the relevant part of the code. We can still add regular markers (not managed by a clusterer), such as the red marker in the video.
See the [full code](demo/src/main/java/ovh/plrapps/mapcompose/demo/viewmodels/MarkersClusteringVM.kt).

```kotlin
/* Add clusterer */
state.addClusterer("default") { ids ->
   { Cluster(size = ids.size) }
}

/* Add marker managed by the clusterer */
state.addMarker(
    id = "marker",
    x = 0.2,
    y = 0.3,
    renderingStrategy = RenderingStrategy.Clustering("default"),
) {
    Marker()
}
```

There's an example in the demo app.


## Installation

Add this to your module's build.gradle
```groovy
implementation 'ovh.plrapps:mapcompose:3.2.3'
```

Starting with v.2.4.1, the library is using the 
[compose BOM](https://developer.android.com/jetpack/compose/bom/bom). The version of the BOM is
specified in the release notes. The demo app shows an example of how to use it.

## Basics

MapCompose is optimized to display maps that have several levels, like this:

<p align="center">
<img src="doc/readme-files/pyramid.png" width="400">
</p>

Each next level is twice bigger than the former, and provides more details. Overall, this looks like
 a pyramid. Another common name is "deep-zoom" map.
This library comes with a demo app featuring various use-cases such as using markers, paths,
map rotation, etc. All examples use the same map stored in the assets, which is a great example of
deep-zoom map.

MapCompose can also be used with single level maps.

### Usage

In a typical application, you create a `MapState` instance inside a `ViewModel` (or whatever
component which survives device rotation). Your `MapState` should then be passed to the `MapUI`
composable. The code sample at the top of this readme shows an example. Then, whenever you need to
update the map (add a marker, a path, change the scale, etc.), you invoke APIs on your `MapState`
instance. As its name suggests, `MapState` also _owns_ the state. Therefore, composables will always
render consistently - even after a device rotation.

All public APIs are located under the [api](mapcompose/src/main/java/ovh/plrapps/mapcompose/api) 
package. The following sections provide details on the `MapState` class, and give examples of how to
add markers, callouts, and paths. 

All apis should be invoked from the main thread.

### MapState

The `MapState` class expects three parameters for its construction:
* `levelCount`: The number of levels of the map,
* `fullWidth`: The width of the map at scale 1.0, which is the width of last level,
* `fullHeight`: The height of the map at scale 1.0, which is the height of last level

### Layers

MapCompose supports layers - e.g it's possible to add several tile pyramids. Each level is made of
the superposition of tiles from all pyramids at the given level. For example, at the second level
(starting from the lowest scale), tiles would look like the image below when three layers are added.

<p align="center">
<img src="doc/readme-files/layer.png" width="200">
</p>

Your implementation of the `TileStreamProvider` interface (see below) is what defines a tile
pyramid. It provides `InputStream`s of image files (png, jpg). MapCompose will request tiles using
the convention that the origin is at the top-left corner. For example, the tile requested with
`row` = 0, and `col = 0` will be positioned at the top-left corner.

```kotlin
fun interface TileStreamProvider {
    suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}
```

Depending on your configuration, your `TileStreamProvider` implementation might fetch local files,
as well as performing remote HTTP requests - it's up to you. You don't have to worry about threading,
MapCompose takes care of that (the main thread isn't blocked by `getTileStream` calls). However, in
case of HTTP requests, it's advised to create a `MapState` with a higher than default `workerCount`.
That optional parameter defines the size of the dedicated thread pool for fetching tiles, and defaults
to the number of cores minus one. Typically, you would want to set `workerCount` to 16 when performing
HTTP requests. Otherwise, you can safely leave it to its default.

To add a layer, use the `addLayer` on your `MapState` instance. There are others APIs for reordering,
removing, setting alpha - all dynamically.

### Markers

To add a marker, use the [addMarker](https://github.com/p-lr/MapCompose/blob/982caf29ab5e86b58c56812735f60bfe405638ea/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/MarkerApi.kt#L30)
API, like so:

```kotlin
/* Add a marker at the center of the map */
mapState.addMarker("id", x = 0.5, y = 0.5) {
    Icon(
        painter = painterResource(id = R.drawable.map_marker),
        contentDescription = null,
        modifier = Modifier.size(50.dp),
        tint = Color(0xCC2196F3)
    )
}
```

<p align="center">
<img src="doc/readme-files/marker.png">
</p>

A marker is a composable that you supply (in the example above, it's an `Icon`). It can be
whatever composable you like. A marker does not scale, but it's position updates as the map scales,
so it's always attached to the original position. A marker has an anchor point defined - the point
which is fixed relatively to the map. This anchor point is defined using relative offsets, which are
applied to the width and height of the marker. For example, to have a marker centered horizontally 
and aligned at the bottom edge (like a typical map pin would do), you'd pass -0.5f and -1.0f as
relative offsets (left position is offset by half the width, and top is offset by the full height).
If necessary, an absolute offset expressed in pixels can be applied, in addition to the
relative offset.

Markers can be moved, removed, and be draggable. See the following APIs: [moveMarker](https://github.com/p-lr/MapCompose/blob/2fbf0967290ffe01d63a6c65a3022568ef48b9dd/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/MarkerApi.kt#L72),
[removeMarker](https://github.com/p-lr/MapCompose/blob/2fbf0967290ffe01d63a6c65a3022568ef48b9dd/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/MarkerApi.kt#L61),
[enableMarkerDrag](https://github.com/p-lr/MapCompose/blob/2fbf0967290ffe01d63a6c65a3022568ef48b9dd/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/MarkerApi.kt#L89).

### Callouts

Callouts are typically message popups which are, like markers, attached to a specific position.
However, they automatically dismiss on touch down. This default behavior can be changed. 
To add a callout, use [addCallout](https://github.com/p-lr/MapCompose/blob/2fbf0967290ffe01d63a6c65a3022568ef48b9dd/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/MarkerApi.kt#L220).

<p align="center">
<img src="doc/readme-files/callout.png">
</p>

Callouts can be programmatically removed (if automatic dismiss was disabled).

### Paths

To add a path, use the `addPath` api:

```kotlin
mapState.addPath("pathId", color = Color(0xFF448AFF)) {
  addPoints(points)
}
```

The demo app shows a complete example.

<p align="center">
<img src="doc/readme-files/path.png">
</p>

## Animate state change

It's pretty common to programmatically animate the scroll and/or the scale, or even the rotation of
the map.

*scroll and/or scale animation*

When animating the scale, we generally do so while maintaining the center of the screen at
a specific position. Likewise, when animating the scroll position, we can do so with or without 
animating the scale altogether, using [scrollTo](https://github.com/p-lr/MapCompose/blob/08c0f68f654c1ce27a295f3fb6c25e9cf4274de9/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/LayoutApi.kt#L188)
and [snapScrollTo](https://github.com/p-lr/MapCompose/blob/08c0f68f654c1ce27a295f3fb6c25e9cf4274de9/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/LayoutApi.kt#L161).

*rotation animation*

For animating the rotation while keeping the current scale and scroll, use the
[rotateTo](https://github.com/p-lr/MapCompose/blob/08c0f68f654c1ce27a295f3fb6c25e9cf4274de9/mapcompose/src/main/java/ovh/plrapps/mapcompose/api/LayoutApi.kt#L149) API.

Both `scrollTo` and `rotateTo` are suspending functions. Therefore, you know exactly when
an animation finishes, and you can easily chain animations inside a coroutine.

```kotlin
// Inside a ViewModel
viewModelScope.launch {
    mapState.scrollTo(0.8, 0.8, destScale = 2f)
    mapState.rotateTo(180f, TweenSpec(2000, easing = FastOutSlowInEasing))
}
```

For a detailed example, see the "AnimationDemo".

## Design changes and differences with MapView

* In MapView, you had to define bounds before you could add markers. There's no such concept
in MapCompose anymore. Now, coordinates are normalized. For example, (x=0.5, y=0.5) is a point located at
the center of the map. Normalized coordinates are easier to reason about, and application code can
still translate this coordinate system to a custom one.

* In MapView, you had to build a configuration and use that configuration to create a `MapView`
instance. There's no such thing in MapCompose. Now, you create a `MapState` object with required
parameters.

* A lot of things which couldn't change after MapView configuration can now be changed dynamically
in MapCompose. For example, the `zIndex` of a marker, or the minimum scale mode can be changed at
runtime.

## Migrate from `2.x.x`

* All apis taking the scale as parameter now require `Double` values instead of `Float`.

* A few low-level apis taking the scroll in pixels now require `Double` values instead of `Float`.

* The `addMarker` api now longer has the parameter `clipShape`.

## Contributors

Marcin (@Nohus) has contributed and fixed some issues. He also thoroughly tested the new layers 
feature – which made `MapCompose` better.

