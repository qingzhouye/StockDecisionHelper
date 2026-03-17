package com.stockhelper.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.data.repository.StockRepository;
import com.stockhelper.model.Stock;
import com.stockhelper.model.StockHistory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockViewModel extends AndroidViewModel {
    
    private final StockRepository repository;
    private final LiveData<List<StockEntity>> allStocks;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public StockViewModel(@NonNull Application application) {
        super(application);
        repository = StockRepository.getInstance(application);
        allStocks = repository.getAllStocks();
    }

    public LiveData<List<StockEntity>> getAllStocks() {
        return allStocks;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void insertStock(StockEntity stock) {
        executorService.execute(() -> {
            repository.insertStock(stock);
        });
    }

    public void updateStock(StockEntity stock) {
        executorService.execute(() -> {
            repository.updateStock(stock);
        });
    }

    public void deleteStock(StockEntity stock) {
        executorService.execute(() -> {
            repository.deleteStock(stock);
        });
    }

    public void deleteStockById(int id) {
        executorService.execute(() -> {
            repository.deleteStockById(id);
        });
    }

    public void updateNotifiedStatus(int id, boolean notified) {
        executorService.execute(() -> {
            repository.updateNotifiedStatus(id, notified);
        });
    }

    public void refreshAllPrices() {
        executorService.execute(() -> {
            isLoading.postValue(true);
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
                    }
                }
            } catch (Exception e) {
                errorMessage.postValue("更新价格失败: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public StockHistory fetchStockHistory(String code) {
        return repository.fetchStockHistory(code);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
