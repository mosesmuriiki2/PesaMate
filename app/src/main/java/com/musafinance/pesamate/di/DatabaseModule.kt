package com.musafinance.pesamate.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.musafinance.pesamate.data.local.PesaMateDao
import com.musafinance.pesamate.data.local.PesaMateDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): PesaMateDatabase {
        return Room.databaseBuilder(
            context,
            PesaMateDatabase::class.java,
            "pesamate_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDao(database: PesaMateDatabase): PesaMateDao {
        return database.pesamateDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}