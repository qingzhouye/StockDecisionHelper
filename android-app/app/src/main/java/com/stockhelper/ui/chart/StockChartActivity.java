package com.stockhelper.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.stockhelper.R;
import com.stockhelper.data.repository.StockRepository;
import com.stockhelper.model.StockHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockChartActivity extends AppCompatActivity {
    
    private LineChart chart;
    private ProgressBar progressBar;
    private TextView tvError;
    private StockRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_chart);
        
        String stockCode = getIntent().getStringExtra("stock_code");
        String stockName = getIntent().getStringExtra("stock_name");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(stockName != null ? stockName : "股票走势");
        }
        
        repository = StockRepository.getInstance(this);
        
        chart = findViewById(R.id.chart);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        
        setupChart();
        
        if (stockCode != null) {
            loadChartData(stockCode);
        } else {
            showError("股票代码无效");
        }
    }
    
    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(true);
    }
    
    private void loadChartData(String stockCode) {
        showLoading();
        
        executorService.execute(() -> {
            try {
                StockHistory history = repository.fetchStockHistory(stockCode);
                
                runOnUiThread(() -> {
                    if (history != null && !history.getDates().isEmpty()) {
                        displayChart(history);
                    } else {
                        showError("暂无历史数据");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError("加载失败: " + e.getMessage());
                });
            }
        });
    }
    
    private void displayChart(StockHistory history) {
        hideLoading();
        
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < history.getPrices().size(); i++) {
            entries.add(new Entry(i, history.getPrices().get(i).floatValue()));
        }
        
        // Sample labels to avoid overcrowding
        int step = Math.max(1, history.getDates().size() / 6);
        for (int i = 0; i < history.getDates().size(); i += step) {
            labels.add(history.getDates().get(i).substring(5)); // Show MM-DD
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "收盘价");
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(2f);
        dataSet.setColor(Color.parseColor("#667eea"));
        dataSet.setFillColor(Color.parseColor("#667eea"));
        dataSet.setFillAlpha(50);
        dataSet.setDrawFilled(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size(), true);
        
        chart.invalidate();
        chart.animateX(1000);
    }
    
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        chart.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }
    
    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        chart.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
