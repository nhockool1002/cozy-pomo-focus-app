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
        Room.databaseBuilder(context, CozyPomoDatabase::class.java, "cozypomo.db")
            // App chưa release chính thức — chưa cần viết Migration thủ công cho mỗi lần đổi
            // schema (T-029/T-030 mới xây, dữ liệu local mất khi đổi bản không sao).
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideSessionDao(database: CozyPomoDatabase): SessionDao = database.sessionDao()
}
