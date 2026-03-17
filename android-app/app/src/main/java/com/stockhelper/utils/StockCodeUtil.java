package com.stockhelper.utils;

public class StockCodeUtil {
    
    public static String formatStockCode(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String code = input.trim().replaceAll("[^\\d]", "");
        
        if (code.length() == 5) {
            return "hk" + code;
        } else if (code.length() == 6) {
            if (code.startsWith("6")) {
                return "sh" + code;
            } else if (code.startsWith("0") || code.startsWith("3")) {
                return "sz" + code;
            }
        }
        
        return code;
    }
    
    public static String getDisplayCode(String fullCode) {
        if (fullCode == null) return "";
        if (fullCode.startsWith("sh") || fullCode.startsWith("sz") || fullCode.startsWith("hk")) {
            return fullCode.substring(2);
        }
        return fullCode;
    }
    
    public static String getMarketName(String fullCode) {
        if (fullCode == null) return "";
        if (fullCode.startsWith("sh")) return "上海";
        if (fullCode.startsWith("sz")) return "深圳";
        if (fullCode.startsWith("hk")) return "港股";
        return "";
    }
}
