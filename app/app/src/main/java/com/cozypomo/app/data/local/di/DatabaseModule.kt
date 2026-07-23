package com.cozypomo.app.data.local.di

import android.content.Context
import androidx.room.Room
import com.cozypomo.app.data.local.CozyPomoDatabase
import com.cozypomo.app.data.local.session.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CozyPomoDatabase =
        Room.databaseBuilder(context, CozyPomoDatabase::class.java, "cozypomo.db").build()

    @Provides
    @Singleton
    fun provideSessionDao(database: CozyPomoDatabase): SessionDao = database.sessionDao()
}
