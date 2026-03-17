package com.stockhelper.model;

import java.util.List;

public class StockHistory {
    private List<String> dates;
    private List<Double> prices;

    public StockHistory(List<String> dates, List<Double> prices) {
        this.dates = dates;
        this.prices = prices;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public List<Double> getPrices() {
        return prices;
    }

    public void setPrices(List<Double> prices) {
        this.prices = prices;
    }
}
