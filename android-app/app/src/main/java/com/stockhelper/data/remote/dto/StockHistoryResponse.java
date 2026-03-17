package com.stockhelper.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class StockHistoryResponse {
    
    @SerializedName("code")
    private int code;
    
    @SerializedName("data")
    private Map<String, StockData> data;
    
    public int getCode() {
        return code;
    }
    
    public Map<String, StockData> getData() {
        return data;
    }
    
    public static class StockData {
        @SerializedName("qfq")
        private QfqData qfq;
        
        public QfqData getQfq() {
            return qfq;
        }
    }
    
    public static class QfqData {
        @SerializedName("day")
        private List<List<String>> day;
        
        public List<List<String>> getDay() {
            return day;
        }
    }
}
