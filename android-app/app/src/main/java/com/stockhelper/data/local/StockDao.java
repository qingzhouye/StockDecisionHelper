package com.stockhelper.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.stockhelper.data.local.entities.StockEntity;

import java.util.List;

@Dao
public interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY id DESC")
    LiveData<List<StockEntity>> getAllStocks();

    @Query("SELECT * FROM stocks ORDER BY id DESC")
    List<StockEntity> getAllStocksSync();

    @Query("SELECT * FROM stocks WHERE id = :id")
    StockEntity getStockById(int id);

    @Insert
    long insertStock(StockEntity stock);

    @Update
    void updateStock(StockEntity stock);

    @Delete
    void deleteStock(StockEntity stock);

    @Query("DELETE FROM stocks WHERE id = :id")
    void deleteStockById(int id);

    @Query("UPDATE stocks SET currentPrice = :price, updateTime = :updateTime WHERE id = :id")
    void updatePrice(int id, double price, long updateTime);

    @Query("UPDATE stocks SET notified = :notified WHERE id = :id")
    void updateNotifiedStatus(int id, boolean notified);
}
