package com.stockhelper.ui.addstock;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.stockhelper.R;
import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.data.repository.StockRepository;
import com.stockhelper.utils.StockCodeUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddStockActivity extends AppCompatActivity {
    
    private TextInputEditText etCode;
    private TextInputEditText etBuyPrice;
    private TextInputEditText etQuantity;
    private TextInputEditText etTargetReturn;
    private StockRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stock);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("添加股票");
        }
        
        repository = StockRepository.getInstance(this);
        
        initViews();
    }
    
    private void initViews() {
        etCode = findViewById(R.id.etCode);
        etBuyPrice = findViewById(R.id.etBuyPrice);
        etQuantity = findViewById(R.id.etQuantity);
        etTargetReturn = findViewById(R.id.etTargetReturn);
        Button btnSave = findViewById(R.id.btnSave);
        
        etTargetReturn.setText("15");
        
        btnSave.setOnClickListener(v -> saveStock());
    }
    
    private void saveStock() {
        String codeInput = etCode.getText() != null ? etCode.getText().toString().trim() : "";
        String buyPriceInput = etBuyPrice.getText() != null ? etBuyPrice.getText().toString().trim() : "";
        String quantityInput = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
        String targetReturnInput = etTargetReturn.getText() != null ? etTargetReturn.getText().toString().trim() : "";
        
        if (TextUtils.isEmpty(codeInput)) {
            etCode.setError("请输入股票代码");
            return;
        }
        
        if (TextUtils.isEmpty(buyPriceInput)) {
            etBuyPrice.setError("请输入买入价格");
            return;
        }
        
        if (TextUtils.isEmpty(quantityInput)) {
            etQuantity.setError("请输入买入数量");
            return;
        }
        
        String formattedCode = StockCodeUtil.formatStockCode(codeInput);
        
        double buyPrice;
        int quantity;
        double targetReturn;
        
        try {
            buyPrice = Double.parseDouble(buyPriceInput);
            if (buyPrice <= 0) {
                etBuyPrice.setError("买入价格必须大于0");
                return;
            }
        } catch (NumberFormatException e) {
            etBuyPrice.setError("请输入有效的价格");
            return;
        }
        
        try {
            quantity = Integer.parseInt(quantityInput);
            if (quantity <= 0) {
                etQuantity.setError("买入数量必须大于0");
                return;
            }
        } catch (NumberFormatException e) {
            etQuantity.setError("请输入有效的数量");
            return;
        }
        
        try {
            targetReturn = TextUtils.isEmpty(targetReturnInput) ? 15.0 : Double.parseDouble(targetReturnInput);
        } catch (NumberFormatException e) {
            targetReturn = 15.0;
        }
        
        StockEntity stock = new StockEntity();
        stock.setCode(formattedCode);
        stock.setBuyPrice(buyPrice);
        stock.setQuantity(quantity);
        stock.setTargetReturn(targetReturn);
        stock.setUpdateTime(0);
        
        executorService.execute(() -> {
            // Fetch current price
            com.stockhelper.model.Stock priceInfo = repository.fetchStockPrice(formattedCode);
            if (priceInfo != null) {
                stock.setName(priceInfo.getName());
                stock.setCurrentPrice(priceInfo.getPrice());
                stock.setPreviousClose(priceInfo.getPreviousClose());
                stock.setOpenPrice(priceInfo.getOpen());
                stock.setHighPrice(priceInfo.getHigh());
                stock.setLowPrice(priceInfo.getLow());
                stock.setVolume(priceInfo.getVolume());
                stock.setUpdateTime(System.currentTimeMillis());
            }
            
            repository.insertStock(stock);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "股票添加成功", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
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
