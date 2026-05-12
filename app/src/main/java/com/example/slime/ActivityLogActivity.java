package com.example.slime;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActivityLogActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_activity_log);

        ActivityLogger.log(this, "Activity Log");

        RecyclerView recyclerView = findViewById(R.id.rvActivityLog);
        TextView tvEmpty = findViewById(R.id.tvActivityLogEmpty);
        Button btnBack = findViewById(R.id.btnActivityLogBack);

        btnBack.setOnClickListener(v -> finish());

        List<ActivityLogDbHelper.ActivityLogEntry> entries =
                ActivityLogDbHelper.getInstance(this).queryAll();

        if (entries.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new LogAdapter(entries));
        }
    }

    // --- Adapter ---
    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private final List<ActivityLogDbHelper.ActivityLogEntry> data;

        LogAdapter(List<ActivityLogDbHelper.ActivityLogEntry> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_activity_log, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ActivityLogDbHelper.ActivityLogEntry entry = data.get(position);
            holder.tvNo.setText(String.valueOf(position + 1));
            holder.tvTimestamp.setText(entry.timestamp);
            holder.tvScreen.setText(entry.screenName);
            holder.tvUsername.setText(entry.username != null ? entry.username : "Guest");
            if (entry.managerName != null && !entry.managerName.isEmpty()) {
                holder.tvManager.setVisibility(View.VISIBLE);
                holder.tvManager.setText(entry.managerName);
            } else {
                holder.tvManager.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNo, tvTimestamp, tvScreen, tvUsername, tvManager;

            ViewHolder(View v) {
                super(v);
                tvNo        = v.findViewById(R.id.tvLogNo);
                tvTimestamp = v.findViewById(R.id.tvLogTimestamp);
                tvScreen    = v.findViewById(R.id.tvLogScreen);
                tvUsername  = v.findViewById(R.id.tvLogUsername);
                tvManager   = v.findViewById(R.id.tvLogManager);
            }
        }
    }
}
