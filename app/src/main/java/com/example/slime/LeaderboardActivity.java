package com.example.slime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private LeaderboardAdapter adapter;
    private List<ScoreEntry> scoreList;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        scoreList = new ArrayList<>();
        adapter   = new LeaderboardAdapter(scoreList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeToLeaderboard();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void subscribeToLeaderboard() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        listenerRegistration = db.collection("leaderboard")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        tvEmpty.setText(getString(R.string.leaderboard_load_error));
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    scoreList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        long score  = doc.getLong("score") != null ? doc.getLong("score") : 0;
                        String name = doc.getString("displayName");
                        if (name == null) name = "Unknown";
                        scoreList.add(new ScoreEntry(name, score));
                    }
                    adapter.notifyDataSetChanged();

                    if (scoreList.isEmpty()) {
                        tvEmpty.setText(getString(R.string.leaderboard_empty));
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                });
    }

    // --- Data Model ---
    static class ScoreEntry {
        String name;
        long score;
        ScoreEntry(String name, long score) {
            this.name  = name;
            this.score = score;
        }
    }

    // --- Adapter ---
    static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private final List<ScoreEntry> data;

        LeaderboardAdapter(List<ScoreEntry> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScoreEntry entry = data.get(position);
            holder.tvRank.setText(String.valueOf(position + 1));
            holder.tvName.setText(entry.name);
            holder.tvScore.setText(String.valueOf(entry.score));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvScore;
            ViewHolder(View v) {
                super(v);
                tvRank  = v.findViewById(R.id.tvRank);
                tvName  = v.findViewById(R.id.tvName);
                tvScore = v.findViewById(R.id.tvScore);
            }
        }
    }
}
