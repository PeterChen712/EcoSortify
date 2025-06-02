package com.example.glean.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.glean.dao.DaoRecord;
import com.example.glean.dao.DaoTrash;
import com.example.glean.dao.DaoUser;
import com.example.glean.dao.LocationPointDao;
import com.example.glean.dao.NewsDao;
import com.example.glean.model.LocationPointEntity;
import com.example.glean.model.NewsEntity;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.util.Converters;

@Database(
    entities = {
        UserEntity.class, 
        RecordEntity.class, 
        TrashEntity.class,
        LocationPointEntity.class,  // Add this missing entity
        NewsEntity.class            // Add this missing entity
    },
    version = 7,  // Increment version since we're adding new tables
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "glean_database";
    private static volatile AppDatabase INSTANCE;
    
    public abstract DaoUser userDao();
    public abstract DaoRecord recordDao();
    public abstract DaoTrash trashDao();
    public abstract LocationPointDao locationPointDao();
    public abstract NewsDao newsDao();
    
    // Migration from version 1 to 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE records ADD COLUMN totalDistance REAL DEFAULT 0.0");
            database.execSQL("ALTER TABLE records ADD COLUMN duration INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE records ADD COLUMN endTime INTEGER DEFAULT 0");
        }
    };
    
    // Migration from version 2 to 3
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN username TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE users ADD COLUMN firstName TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE users ADD COLUMN lastName TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE users ADD COLUMN profileImagePath TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE users ADD COLUMN createdAt INTEGER DEFAULT 0");
        }
    };
    
    // Migration from version 3 to 4
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create location_points table
            database.execSQL("CREATE TABLE IF NOT EXISTS `location_points` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`recordId` INTEGER NOT NULL, " +
                    "`latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, " +
                    "`altitude` REAL NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`distanceFromLast` REAL NOT NULL, " +
                    "FOREIGN KEY(`recordId`) REFERENCES `records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            
            // Create index for recordId
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_location_points_recordId` ON `location_points` (`recordId`)");
        }
    };
    
    // Migration from version 4 to 5
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create news table
            database.execSQL("CREATE TABLE IF NOT EXISTS `news` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT, " +
                    "`preview` TEXT, " +
                    "`fullContent` TEXT, " +
                    "`date` TEXT, " +
                    "`source` TEXT, " +
                    "`imageUrl` TEXT, " +
                    "`url` TEXT, " +
                    "`category` TEXT, " +
                    "`createdAt` INTEGER NOT NULL DEFAULT 0)");
        }
    };
    
    // Migration from version 5 to 6
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN points INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN decorations TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE users ADD COLUMN activeDecoration TEXT DEFAULT ''");
        }
    };
    
    // Migration from version 6 to 7
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add any additional columns or tables if needed
            // This migration ensures all tables are properly created
        }
    };
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(
                        MIGRATION_1_2, 
                        MIGRATION_2_3, 
                        MIGRATION_3_4, 
                        MIGRATION_4_5, 
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}