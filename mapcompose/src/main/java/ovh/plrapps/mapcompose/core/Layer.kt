package ovh.plrapps.mapcompose.core

data class Layer(val id: String, val tileStreamProvider: TileStreamProvider)

internal const val mainLayerId: String = "mainLayer#!@"
