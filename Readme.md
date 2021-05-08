# MapCompose

MapCompose is a fast, memory efficient Jetpack compose library to display tiled maps with minimal effort.
It shows the visible part of a tiled map with support of markers and paths, and various gestures
(flinging, dragging, scaling, and rotating).

An example of setting up:

```kotlin
/* Inside your view-model */
val tileStreamProvider = object : TileStreamProvider {
     override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
         return FileInputStream(File("path/{zoomLvl}/{row}/{col}.jpg")) // or it can be a remote HTTP fetch
     }
}

val state: MapState by mutableStateOf(
    MapState(4, 4096, 4096, tileStreamProvider).apply {
        enableRotation()
        scrollToAndCenter(0.5, 0.5)
    }
)

/* Inside a composable */
@Composable
fun MapContainer(
    modifier: Modifier = Modifier, viewModel: YourViewModel
) {
    MapUI(modifier, state = viewModel.state)
}
```

Inspired from [MapView](https://github.com/peterLaurence/MapView), every aspects of the library have
been revisited. MapCompose brings the same level of performance as MapView, with a simplified API.

This project holds the source code of this library, plus a demo app (which is useful to get started).
To test the demo, just clone the repo and launch the demo app from Android Studio Canary (for now).

**🚧 Almost done 🚧**

The core library is done, and all features of MapView are now implemented in MapCompose. I'll continue
to add more demo for various scenario.

## Roadmap / TODO

* Core library
  * [x] Implement the equivalent of ZoomPanLayout in compose
  * [x] Implement a first minimal version (tiles loading only)
  * [x] Markers support
  * [x] Paths support
  * [x] Custom drawings support
  * [ ] Callouts support
  * [ ] Hotspots support

* Demo app
  * [x] Simple map view
  * [x] More advanced rotation APIs use
  * [x] Map with markers
  * [x] Center on marker with animation
  * [x] Map with paths
  * [x] Custom drawings
  * [ ] Callouts demo
  * [ ] Hotspots demo

* Publication
  * [ ] Publish on maven central, under `ovh.plrapps.mapcompose`, artifact id `mapcompose`

## Design changes and differences with MapView

* In MapView, you had to define bounds before you could add markers. There's no more such concept
in MapCompose. Now, coordinates are normalized. For example, (x=0.5, y=0.5) is a point located at
the center of the map. Normalized coordinates are easier to reason about, and application code can
still translate this coordinate system to a custom one.

* The `TileStreamProvider` is now an interface with a suspending function:
```kotlin
interface TileStreamProvider {
    suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}
```

* In MapView, you had to build a configuration and use that configuration to create a `MapView`
instance. There's no such thing in MapCompose. Now, you create a `MapState` object with required
parameters.

* A lot of things which couldn't change after MapView configuration can now be changed dynamically
in MapCompose. For example, the `zIndex` of a marker, or the minimum scale mode can be changed at
runtime.

