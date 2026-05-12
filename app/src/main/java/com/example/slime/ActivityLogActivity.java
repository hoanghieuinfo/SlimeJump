package com.example.slime;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());

    // --- Views for search input ---
    private EditText etFromDateTime, etToDateTime;

    // --- Pagination state ---
    private List<ActivityLogDbHelper.ActivityLogEntry> allResults = new ArrayList<>();
    private int currentPage = 0;

    // --- Views ---
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
        etFromDateTime = findViewById(R.id.etFromDateTime);
        etToDateTime   = findViewById(R.id.etToDateTime);
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
        String fromInput = etFromDateTime.getText().toString().trim();
        String toInput   = etToDateTime.getText().toString().trim();

        // Both empty → show all records
        if (fromInput.isEmpty() && toInput.isEmpty()) {
            allResults = ActivityLogDbHelper.getInstance(this).queryAll();
            currentPage = 0;
            sensorTriggerReady = true;
            refreshDisplay();
            return;
        }

        // Validate non-empty fields
        String fromTs = null, toTs = null;
        if (!fromInput.isEmpty()) {
            fromTs = parseToStorageFormat(fromInput);
            if (fromTs == null) {
                etFromDateTime.setError(getString(R.string.log_datetime_error));
                return;
            }
        }
        if (!toInput.isEmpty()) {
            toTs = parseToStorageFormat(toInput);
            if (toTs == null) {
                etToDateTime.setError(getString(R.string.log_datetime_error));
                return;
            }
            // toTs: include the full last minute (append :59)
            toTs = toTs.replace(":00", ":59");
        }

        allResults = ActivityLogDbHelper.getInstance(this).queryByTimeRange(fromTs, toTs);
        currentPage = 0;
        sensorTriggerReady = true;
        refreshDisplay();
    }

    /**
     * Parses "yyyy-MM-dd HH:mm" input to storage format "yyyy-MM-dd HH:mm:00".
     * Returns null if the input doesn't match the expected pattern.
     */
    private String parseToStorageFormat(String input) {
        try {
            SDF.setLenient(false);
            SDF.parse(input); // validates the date
            return input + ":00";
        } catch (ParseException e) {
            return null;
        }
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
