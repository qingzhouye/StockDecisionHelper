package com.stockhelper.data.remote;

import com.stockhelper.data.remote.dto.StockHistoryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StockApiService {
    
    @GET("https://qt.gtimg.cn/q")
    Call<String> getStockPrice(@Query("q") String code);
    
    @GET("https://web.ifzq.gtimg.cn/appstock/app/fqkline/get")
    Call<StockHistoryResponse> getStockHistory(@Query("param") String param);
}
