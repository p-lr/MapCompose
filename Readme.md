# MapCompose

MapCompose is a Fast, memory efficient Jetpack compose library to display tiled maps with minimal effort.
Inspired from [MapView](https://github.com/peterLaurence/MapView), every aspects of the library have
been revisited. MapCompose brings the same level of performance as MapView, with a simplified API.

An example of setting up:

```kotlin
/* Inside a YourViewModel */
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

MapView shows only the visible part of a tiled map, and supports flinging, dragging, scaling, and
rotating. It's also possible to add markers and paths.

This project holds the source code of this library, plus a demo app (which is useful to get started).
To test the demo, just clone the repo and launch the demo app from Android Studio Canary (for now).

**🚧 Work in progress 🚧**

The core library is done, and all features of MapView are now implemented in MapCompose. I'll continue
to add more demo to test various scenario.

## Roadmap / TODO

* Core library
  * [x] Implement the equivalent of ZoomPanLayout in compose
  * [x] Implement a first minimal version (tiles loading only)
  * [x] Markers support
  * [x] Paths support

* Demo app
  * [x] Simple map view
  * [x] More advanced rotation APIs use
  * [x] Map with markers
  * [x] Center on marker with animation
  * [x] Map with paths

* Publication
  * [ ] Publish on maven central, under `ovh.plrapps.mapcompose`, artifact id `mapcompose`