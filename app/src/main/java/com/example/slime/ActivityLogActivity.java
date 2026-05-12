package com.example.slime;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ActivityLogActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PAGE_SIZE = 3;
    // lux threshold: ≤10 = sensor covered, >10 = uncovered
    private static final float DARK_THRESHOLD = 10f;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    // --- Search date-time state ---
    private int fromYear, fromMonth, fromDay, fromHour, fromMinute;
    private int toYear,   toMonth,   toDay,   toHour,   toMinute;

    // --- Pagination state ---
    private List<ActivityLogDbHelper.ActivityLogEntry> allResults = new ArrayList<>();
    private int currentPage = 0;

    // --- Views ---
    private TextView tvFromDateTime, tvToDateTime;
    private TextView tvPageInfo, tvSensorHint, tvEmpty;
    private RecyclerView recyclerView;
    private LogAdapter adapter;

    // --- Sensor ---
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean sensorTriggerReady = true; // allows one trigger per dark event

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_activity_log);

        ActivityLogger.log(this, "Activity Log");

        // --- Bind views ---
        tvFromDateTime = findViewById(R.id.tvFromDateTime);
        tvToDateTime   = findViewById(R.id.tvToDateTime);
        Button btnSearch = findViewById(R.id.btnSearch);
        tvPageInfo     = findViewById(R.id.tvPageInfo);
        tvSensorHint   = findViewById(R.id.tvSensorHint);
        tvEmpty        = findViewById(R.id.tvActivityLogEmpty);
        recyclerView   = findViewById(R.id.rvActivityLog);
        Button btnBack = findViewById(R.id.btnActivityLogBack);

        // --- Setup RecyclerView ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        // --- Default date range: today 00:00 → now ---
        Calendar now = Calendar.getInstance();
        toYear = now.get(Calendar.YEAR);
        toMonth = now.get(Calendar.MONTH);
        toDay = now.get(Calendar.DAY_OF_MONTH);
        toHour = now.get(Calendar.HOUR_OF_DAY);
        toMinute = now.get(Calendar.MINUTE);

        fromYear = toYear;
        fromMonth = toMonth;
        fromDay = toDay;
        fromHour = 0;
        fromMinute = 0;

        updateDateTimeLabels();

        // --- Picker listeners ---
        tvFromDateTime.setOnClickListener(v -> pickDateTime(true));
        tvToDateTime.setOnClickListener(v -> pickDateTime(false));

        // --- Search button ---
        btnSearch.setOnClickListener(v -> performSearch());

        // --- Back button ---
        btnBack.setOnClickListener(v -> finish());

        // --- Sensor setup ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        // --- Initial load: all records, first page ---
        allResults = ActivityLogDbHelper.getInstance(this).queryAll();
        currentPage = 0;
        refreshDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // -------------------------------------------------------------------------
    // Light sensor: cover sensor (lux ≤ DARK_THRESHOLD) → next page
    // -------------------------------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;
        float lux = event.values[0];

        if (lux <= DARK_THRESHOLD) {
            // Sensor covered → trigger next page (once per dark event)
            if (sensorTriggerReady && !allResults.isEmpty()) {
                sensorTriggerReady = false;
                advancePage();
            }
        } else {
            // Sensor uncovered → allow next trigger
            sensorTriggerReady = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------
    private void advancePage() {
        int totalPages = totalPages();
        if (totalPages == 0) return;
        currentPage = (currentPage + 1) % totalPages;
        refreshDisplay();
        int first = currentPage * PAGE_SIZE + 1;
        int last  = Math.min(first + PAGE_SIZE - 1, allResults.size());
        Toast.makeText(this,
                getString(R.string.log_page_toast, first, last, allResults.size()),
                Toast.LENGTH_SHORT).show();
    }

    private int totalPages() {
        if (allResults.isEmpty()) return 0;
        return (int) Math.ceil((double) allResults.size() / PAGE_SIZE);
    }

    private void refreshDisplay() {
        if (allResults.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvPageInfo.setText(getString(R.string.log_no_results));
            tvSensorHint.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        int first = currentPage * PAGE_SIZE;
        int last  = Math.min(first + PAGE_SIZE, allResults.size());
        List<ActivityLogDbHelper.ActivityLogEntry> pageData = allResults.subList(first, last);

        adapter.setData(pageData, first);

        int displayFirst = first + 1;
        int displayLast  = last;
        int total        = allResults.size();
        int totalPg      = totalPages();
        tvPageInfo.setText(getString(R.string.log_page_info,
                displayFirst, displayLast, total, currentPage + 1, totalPg));

        if (lightSensor != null && totalPg > 1) {
            tvSensorHint.setVisibility(View.VISIBLE);
            tvSensorHint.setText(getString(R.string.log_sensor_hint));
        } else {
            tvSensorHint.setVisibility(View.GONE);
        }
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------
    private void performSearch() {
        String fromTs = String.format(Locale.getDefault(),
                "%04d-%02d-%02d %02d:%02d:00",
                fromYear, fromMonth + 1, fromDay, fromHour, fromMinute);
        String toTs = String.format(Locale.getDefault(),
                "%04d-%02d-%02d %02d:%02d:59",
                toYear, toMonth + 1, toDay, toHour, toMinute);

        allResults = ActivityLogDbHelper.getInstance(this).queryByTimeRange(fromTs, toTs);
        currentPage = 0;
        sensorTriggerReady = true;
        refreshDisplay();
    }

    // -------------------------------------------------------------------------
    // Date / Time pickers
    // -------------------------------------------------------------------------
    private void pickDateTime(boolean isFrom) {
        int year   = isFrom ? fromYear  : toYear;
        int month  = isFrom ? fromMonth : toMonth;
        int day    = isFrom ? fromDay   : toDay;

        new DatePickerDialog(this, (view, y, m, d) -> {
            if (isFrom) { fromYear = y; fromMonth = m; fromDay = d; }
            else        { toYear   = y; toMonth   = m; toDay   = d; }

            int hour = isFrom ? fromHour : toHour;
            int min  = isFrom ? fromMinute : toMinute;
            new TimePickerDialog(this, (tv, h, mn) -> {
                if (isFrom) { fromHour = h; fromMinute = mn; }
                else        { toHour   = h; toMinute   = mn; }
                updateDateTimeLabels();
            }, hour, min, true).show();

        }, year, month, day).show();
    }

    private void updateDateTimeLabels() {
        tvFromDateTime.setText(String.format(Locale.getDefault(),
                "%04d-%02d-%02d  %02d:%02d",
                fromYear, fromMonth + 1, fromDay, fromHour, fromMinute));
        tvToDateTime.setText(String.format(Locale.getDefault(),
                "%04d-%02d-%02d  %02d:%02d",
                toYear, toMonth + 1, toDay, toHour, toMinute));
    }

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------
    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private List<ActivityLogDbHelper.ActivityLogEntry> data = new ArrayList<>();
        private int offset = 0; // global index of first item in this page

        void setData(List<ActivityLogDbHelper.ActivityLogEntry> page, int offset) {
            this.data   = page;
            this.offset = offset;
            notifyDataSetChanged();
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
            holder.tvNo.setText(String.valueOf(offset + position + 1));
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
        public int getItemCount() { return data.size(); }

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
