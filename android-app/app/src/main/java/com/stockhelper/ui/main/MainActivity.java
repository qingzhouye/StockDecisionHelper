package com.stockhelper.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stockhelper.R;
import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.notification.StockNotificationManager;
import com.stockhelper.ui.addstock.AddStockActivity;
import com.stockhelper.ui.chart.StockChartActivity;
import com.stockhelper.viewmodel.StockViewModel;

public class MainActivity extends AppCompatActivity implements StockAdapter.OnStockClickListener {
    
    private StockViewModel viewModel;
    private StockAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;
    private StockNotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        notificationManager = new StockNotificationManager(this);
        
        initViews();
        initViewModel();
    }
    
    private void initViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        
        adapter = new StockAdapter();
        adapter.setOnStockClickListener(this);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddStockActivity.class);
            startActivity(intent);
        });
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshAllPrices();
        });
    }
    
    private void initViewModel() {
        viewModel = new ViewModelProvider(this, 
                new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                .get(StockViewModel.class);
        
        viewModel.getAllStocks().observe(this, stocks -> {
            if (stocks != null && !stocks.isEmpty()) {
                adapter.setStocks(stocks);
                tvEmptyState.setVisibility(View.GONE);
                
                // Check for stocks that reached target
                for (StockEntity stock : stocks) {
                    if (stock.shouldSell() && !stock.isNotified()) {
                        notificationManager.showSellNotification(stock);
                        viewModel.updateNotifiedStatus(stock.getId(), true);
                    }
                }
            } else {
                adapter.setStocks(stocks);
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            swipeRefreshLayout.setRefreshing(isLoading != null && isLoading);
        });
        
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onDeleteClick(StockEntity stock) {
        new AlertDialog.Builder(this)
                .setTitle("删除股票")
                .setMessage("确定要删除 " + stock.getName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deleteStock(stock);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    @Override
    public void onChartClick(StockEntity stock) {
        Intent intent = new Intent(this, StockChartActivity.class);
        intent.putExtra("stock_code", stock.getCode());
        intent.putExtra("stock_name", stock.getName());
        startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refreshAllPrices();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshAllPrices();
    }
}
