package com.drumpads.app.di

import com.drumpads.app.core.audio.AudioEngine
import com.drumpads.app.core.audio.NativeAudioEngineBridge
import com.drumpads.app.domain.repository.DefaultPadRepository
import com.drumpads.app.domain.repository.PadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAudioEngine(impl: NativeAudioEngineBridge): AudioEngine

    @Binds
    @Singleton
    abstract fun bindPadRepository(impl: DefaultPadRepository): PadRepository
}
