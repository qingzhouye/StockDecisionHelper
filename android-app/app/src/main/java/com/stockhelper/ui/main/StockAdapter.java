package com.stockhelper.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.stockhelper.R;
import com.stockhelper.data.local.entities.StockEntity;
import com.stockhelper.utils.StockCodeUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
    
    private List<StockEntity> stocks = new ArrayList<>();
    private OnStockClickListener listener;
    
    public interface OnStockClickListener {
        void onDeleteClick(StockEntity stock);
        void onChartClick(StockEntity stock);
    }
    
    public void setOnStockClickListener(OnStockClickListener listener) {
        this.listener = listener;
    }
    
    public void setStocks(List<StockEntity> stocks) {
        this.stocks = stocks;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockEntity stock = stocks.get(position);
        holder.bind(stock);
    }
    
    @Override
    public int getItemCount() {
        return stocks.size();
    }
    
    class StockViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvName;
        private final TextView tvCode;
        private final TextView tvBuyPrice;
        private final TextView tvCurrentPrice;
        private final TextView tvQuantity;
        private final TextView tvTargetReturn;
        private final TextView tvProfit;
        private final TextView tvReturnRate;
        private final TextView tvUpdateTime;
        private final TextView tvAlert;
        private final Button btnDelete;
        private final Button btnChart;
        
        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvBuyPrice = itemView.findViewById(R.id.tvBuyPrice);
            tvCurrentPrice = itemView.findViewById(R.id.tvCurrentPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvTargetReturn = itemView.findViewById(R.id.tvTargetReturn);
            tvProfit = itemView.findViewById(R.id.tvProfit);
            tvReturnRate = itemView.findViewById(R.id.tvReturnRate);
            tvUpdateTime = itemView.findViewById(R.id.tvUpdateTime);
            tvAlert = itemView.findViewById(R.id.tvAlert);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnChart = itemView.findViewById(R.id.btnChart);
        }
        
        public void bind(StockEntity stock) {
            String displayName = stock.getName() != null && !stock.getName().isEmpty() 
                    ? stock.getName() 
                    : StockCodeUtil.getDisplayCode(stock.getCode());
            tvName.setText(displayName);
            tvCode.setText(stock.getCode());
            tvBuyPrice.setText(String.format(Locale.getDefault(), "¥%.2f", stock.getBuyPrice()));
            tvQuantity.setText(String.format(Locale.getDefault(), "%d 股", stock.getQuantity()));
            tvTargetReturn.setText(String.format(Locale.getDefault(), "+%.1f%%", stock.getTargetReturn()));
            
            if (stock.getCurrentPrice() > 0) {
                tvCurrentPrice.setText(String.format(Locale.getDefault(), "¥%.2f", stock.getCurrentPrice()));
                
                double profit = stock.getProfit();
                double returnRate = stock.getReturnRate();
                
                tvProfit.setText(String.format(Locale.getDefault(), "%s¥%.2f", 
                        profit >= 0 ? "+" : "", profit));
                tvReturnRate.setText(String.format(Locale.getDefault(), "%s%.2f%%", 
                        returnRate >= 0 ? "+" : "", returnRate));
                
                int profitColor = profit >= 0 ? 
                        itemView.getContext().getColor(android.R.color.holo_red_dark) :
                        itemView.getContext().getColor(android.R.color.holo_green_dark);
                tvProfit.setTextColor(profitColor);
                tvReturnRate.setTextColor(profitColor);
                
                if (stock.shouldSell()) {
                    cardView.setStrokeColor(itemView.getContext().getColor(android.R.color.holo_red_light));
                    cardView.setStrokeWidth(4);
                    tvAlert.setVisibility(View.VISIBLE);
                } else {
                    cardView.setStrokeWidth(0);
                    tvAlert.setVisibility(View.GONE);
                }
            } else {
                tvCurrentPrice.setText("加载中...");
                tvProfit.setText("--");
                tvReturnRate.setText("--");
                tvAlert.setVisibility(View.GONE);
            }
            
            if (stock.getUpdateTime() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                tvUpdateTime.setText("更新: " + sdf.format(new Date(stock.getUpdateTime())));
            } else {
                tvUpdateTime.setText("更新: 未更新");
            }
            
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(stock);
                }
            });
            
            btnChart.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChartClick(stock);
                }
            });
        }
    }
}
