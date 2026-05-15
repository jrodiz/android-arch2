package com.rodiz.arch2.core.common.coroutine

import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class MainDispatcher
