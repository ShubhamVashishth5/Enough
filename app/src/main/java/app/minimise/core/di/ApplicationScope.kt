package app.minimise.core.di

import javax.inject.Qualifier

/** A CoroutineScope that lives as long as the app process. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
