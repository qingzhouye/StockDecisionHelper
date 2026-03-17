package com.stockhelper.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stocks")
public class StockEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String code;
    private String name;
    private double buyPrice;
    private int quantity;
    private double targetReturn;
    private double currentPrice;
    private double previousClose;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private long volume;
    private long updateTime;
    private boolean notified;

    public StockEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTargetReturn() {
        return targetReturn;
    }

    public void setTargetReturn(double targetReturn) {
        this.targetReturn = targetReturn;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    public double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public double getProfit() {
        return (currentPrice - buyPrice) * quantity;
    }

    public double getReturnRate() {
        if (buyPrice == 0) return 0;
        return ((currentPrice - buyPrice) / buyPrice) * 100;
    }

    public boolean shouldSell() {
        return getReturnRate() >= targetReturn;
    }
}
