package ovh.plrapps.mapcompose.api

@RequiresOptIn(message = "This API is experimental. It is likely to change before becoming stable.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class ExperimentalClusteringApi