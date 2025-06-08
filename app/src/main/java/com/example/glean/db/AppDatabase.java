package com.example.glean.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.glean.db.CommentDao;
import com.example.glean.db.DatabaseSeeder;
import com.example.glean.db.DaoRecord;
import com.example.glean.db.DaoTrash;
import com.example.glean.db.UserDao;
import com.example.glean.db.LocationPointDao;
import com.example.glean.db.NewsDao;
import com.example.glean.db.PostDao;
import com.example.glean.model.CommentEntity;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.NewsItem;
import com.example.glean.model.PostEntity;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.util.Converters;

@Database(
    entities = {
        UserEntity.class, 
        RecordEntity.class, 
        TrashEntity.class,
        LocationPointEntity.class,
        NewsItem.class,
        PostEntity.class,
        CommentEntity.class
    },
    version = 16, // Increment to version 16 after adding photoPath to RecordEntity
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "glean_database";
    private static volatile AppDatabase INSTANCE;    public abstract UserDao userDao();
    public abstract DaoRecord recordDao();
    public abstract DaoTrash trashDao();
    public abstract LocationPointDao locationPointDao();
    public abstract NewsDao newsDao();
    public abstract PostDao postDao();
    public abstract CommentDao commentDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    // Use destructive migration to handle schema mismatches
                    .fallbackToDestructiveMigration()                    // Optional: Add callback to populate initial data
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Database is created fresh - seed initial data
                            seedInitialData(context);
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Close the database instance
     */
    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
    
    /**
     * Seed initial data when database is created or opened
     */
    private static void seedInitialData(Context context) {
        DatabaseSeeder seeder = new DatabaseSeeder(context);
        seeder.seedDatabaseIfEmpty();
    }
}