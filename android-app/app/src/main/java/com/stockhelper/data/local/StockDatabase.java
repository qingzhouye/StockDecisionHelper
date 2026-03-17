package com.stockhelper.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.stockhelper.data.local.entities.StockEntity;

@Database(entities = {StockEntity.class}, version = 1, exportSchema = false)
public abstract class StockDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "stock_database";
    private static StockDatabase instance;

    public abstract StockDao stockDao();

    public static synchronized StockDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    StockDatabase.class,
                    DATABASE_NAME
            ).build();
        }
        return instance;
    }
}
