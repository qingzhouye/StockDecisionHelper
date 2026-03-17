package com.stockhelper.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.stockhelper.data.local.StockDao;
import com.stockhelper.data.local.StockDatabase;
import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.data.remote.StockApiService;
import com.stockhelper.data.remote.dto.StockHistoryResponse;
import com.stockhelper.model.Stock;
import com.stockhelper.model.StockHistory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class StockRepository {
    private static final String TAG = "StockRepository";
    private static StockRepository instance;
    
    private final StockDao stockDao;
    private final StockApiService apiService;
    
    private StockRepository(Context context) {
        StockDatabase database = StockDatabase.getInstance(context);
        stockDao = database.stockDao();
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://qt.gtimg.cn/")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiService = retrofit.create(StockApiService.class);
    }
    
    public static synchronized StockRepository getInstance(Context context) {
        if (instance == null) {
            instance = new StockRepository(context);
        }
        return instance;
    }
    
    public LiveData<List<StockEntity>> getAllStocks() {
        return stockDao.getAllStocks();
    }
    
    public List<StockEntity> getAllStocksSync() {
        return stockDao.getAllStocksSync();
    }
    
    public long insertStock(StockEntity stock) {
        return stockDao.insertStock(stock);
    }
    
    public void updateStock(StockEntity stock) {
        stockDao.updateStock(stock);
    }
    
    public void deleteStock(StockEntity stock) {
        stockDao.deleteStock(stock);
    }
    
    public void deleteStockById(int id) {
        stockDao.deleteStockById(id);
    }
    
    public void updateNotifiedStatus(int id, boolean notified) {
        stockDao.updateNotifiedStatus(id, notified);
    }
    
    public Stock fetchStockPrice(String code) {
        try {
            Response<String> response = apiService.getStockPrice(code).execute();
            if (response.isSuccessful() && response.body() != null) {
                return parseStockData(response.body(), code);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch stock price", e);
        }
        return getMockStockData(code);
    }
    
    public StockHistory fetchStockHistory(String code) {
        try {
            String param = code + ",day,,,250,qfq";
            Response<StockHistoryResponse> response = apiService.getStockHistory(param).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                StockHistoryResponse historyResponse = response.body();
                if (historyResponse.getData() != null) {
                    StockHistoryResponse.StockData stockData = historyResponse.getData().get(code);
                    if (stockData != null && stockData.getQfq() != null) {
                        List<List<String>> days = stockData.getQfq().getDay();
                        List<String> dates = new ArrayList<>();
                        List<Double> prices = new ArrayList<>();
                        
                        for (List<String> day : days) {
                            if (day.size() >= 3) {
                                dates.add(day.get(0));
                                prices.add(Double.parseDouble(day.get(2)));
                            }
                        }
                        
                        return new StockHistory(dates, prices);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch stock history", e);
        }
        return getMockHistoryData(code);
    }
    
    private Stock parseStockData(String response, String code) {
        Stock stock = new Stock();
        stock.setCode(code);
        
        Pattern pattern = Pattern.compile("v_[^=]+=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            String[] data = matcher.group(1).split("~");
            if (data.length >= 35) {
                stock.setName(data[1]);
                stock.setPrice(Double.parseDouble(data[3]));
                stock.setPreviousClose(Double.parseDouble(data[4]));
                stock.setOpen(Double.parseDouble(data[5]));
                stock.setHigh(Double.parseDouble(data[6]));
                stock.setLow(Double.parseDouble(data[7]));
                stock.setVolume(Long.parseLong(data[8]));
            }
        }
        
        return stock;
    }
    
    private Stock getMockStockData(String code) {
        Stock stock = new Stock();
        stock.setCode(code);
        
        java.util.Map<String, Object> mockData = new java.util.HashMap<>();
        mockData.put("sh600519", new Object[]{"贵州茅台", 1680.0});
        mockData.put("sz000001", new Object[]{"平安银行", 12.5});
        mockData.put("sz000858", new Object[]{"五粮液", 145.0});
        mockData.put("sh000001", new Object[]{"上证指数", 3050.0});
        mockData.put("sz399001", new Object[]{"深证成指", 9850.0});
        mockData.put("hk00700", new Object[]{"腾讯控股", 385.0});
        mockData.put("hk03690", new Object[]{"美团-W", 125.0});
        mockData.put("hk09988", new Object[]{"阿里巴巴-SW", 85.0});
        
        Object[] data = (Object[]) mockData.getOrDefault(code, new Object[]{code, 100.0});
        stock.setName((String) data[0]);
        
        double basePrice = (Double) data[1];
        double fluctuation = (Math.random() - 0.5) * 0.1;
        stock.setPrice(basePrice * (1 + fluctuation));
        stock.setPreviousClose(basePrice);
        stock.setOpen(basePrice * (1 + (Math.random() - 0.5) * 0.02));
        stock.setHigh(stock.getPrice() * 1.02);
        stock.setLow(stock.getPrice() * 0.98);
        stock.setVolume((long) (Math.random() * 1000000));
        stock.setMock(true);
        
        return stock;
    }
    
    private StockHistory getMockHistoryData(String code) {
        java.util.Map<String, Double> basePrices = new java.util.HashMap<>();
        basePrices.put("sh600519", 1680.0);
        basePrices.put("sz000001", 12.5);
        basePrices.put("sz000858", 145.0);
        basePrices.put("sh000001", 3050.0);
        basePrices.put("sz399001", 9850.0);
        basePrices.put("hk00700", 385.0);
        basePrices.put("hk03690", 125.0);
        basePrices.put("hk09988", 85.0);
        
        double basePrice = basePrices.getOrDefault(code, 100.0);
        
        List<String> dates = new ArrayList<>();
        List<Double> prices = new ArrayList<>();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        double currentPrice = basePrice * 0.85;
        
        for (int i = 250; i >= 0; i--) {
            cal.setTime(new java.util.Date());
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i);
            
            if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY ||
                cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY) {
                continue;
            }
            
            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            dates.add(dateStr);
            
            double change = (Math.random() - 0.48) * 0.06;
            currentPrice = currentPrice * (1 + change);
            prices.add(Math.round(currentPrice * 100.0) / 100.0);
        }
        
        return new StockHistory(dates, prices);
    }
}
