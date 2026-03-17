package com.stockhelper.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.data.repository.StockRepository;
import com.stockhelper.model.Stock;
import com.stockhelper.notification.StockNotificationManager;

import java.util.List;

public class StockUpdateWorker extends Worker {
    
    private final StockRepository repository;
    private final StockNotificationManager notificationManager;

    public StockUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        repository = StockRepository.getInstance(context);
        notificationManager = new StockNotificationManager(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<StockEntity> stocks = repository.getAllStocksSync();
            
            for (StockEntity stock : stocks) {
                Stock updatedStock = repository.fetchStockPrice(stock.getCode());
                
                if (updatedStock != null) {
                    stock.setName(updatedStock.getName());
                    stock.setCurrentPrice(updatedStock.getPrice());
                    stock.setPreviousClose(updatedStock.getPreviousClose());
                    stock.setOpenPrice(updatedStock.getOpen());
                    stock.setHighPrice(updatedStock.getHigh());
                    stock.setLowPrice(updatedStock.getLow());
                    stock.setVolume(updatedStock.getVolume());
                    stock.setUpdateTime(System.currentTimeMillis());
                    
                    repository.updateStock(stock);
                    
                    // Check if target reached and not notified
                    if (stock.shouldSell() && !stock.isNotified()) {
                        notificationManager.showSellNotification(stock);
                        repository.updateNotifiedStatus(stock.getId(), true);
                    }
                }
            }
            
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
