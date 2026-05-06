package com.example.taskmateapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoActivity extends AppCompatActivity {

    private RecyclerView recyclerTodo, recyclerCompleted;
    private TextView textPendingCount, textDoneCount;
    private FloatingActionButton fabAdd;
    private ImageButton btnProfile, btnTheme;

    private final List<Task> allTasks = new ArrayList<>();
    private final List<Task> todoTasks = new ArrayList<>();
    private final List<Task> completedTasks = new ArrayList<>();

    private TaskAdapter todoAdapter;
    private TaskAdapter completedAdapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String uid;

    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_todo);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        uid = auth.getCurrentUser().getUid();

        recyclerTodo = findViewById(R.id.recyclerTodo);
        recyclerCompleted = findViewById(R.id.recyclerCompleted);
        textPendingCount = findViewById(R.id.textPendingCount);
        textDoneCount = findViewById(R.id.textDoneCount);
        fabAdd = findViewById(R.id.fabAdd);
        btnProfile = findViewById(R.id.btnProfile);
        btnTheme = findViewById(R.id.btnTheme);

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(TodoActivity.this, ProfileActivity.class));
        });

        btnTheme.setOnClickListener(v -> toggleTheme());

        setupRecyclerViews();
        loadTasksFromFirebase();

        fabAdd.setOnClickListener(v -> showTaskDialog(null));
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;

        SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.apply();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupRecyclerViews() {
        recyclerTodo.setLayoutManager(new LinearLayoutManager(this));
        recyclerCompleted.setLayoutManager(new LinearLayoutManager(this));

        todoAdapter = new TaskAdapter(todoTasks, taskListener);
        completedAdapter = new TaskAdapter(completedTasks, taskListener);

        recyclerTodo.setAdapter(todoAdapter);
        recyclerCompleted.setAdapter(completedAdapter);
    }

    private void loadTasksFromFirebase() {
        db.collection("tasks")
                .whereEqualTo("uid", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    allTasks.clear();

                    if (value != null) {
                        for (var document : value.getDocuments()) {
                            Task task = document.toObject(Task.class);

                            if (task != null) {
                                task.setId(document.getId());
                                allTasks.add(task);
                            }
                        }
                    }

                    refreshLists();
                });
    }

    private final TaskAdapter.TaskListener taskListener = new TaskAdapter.TaskListener() {
        @Override
        public void onCheckChanged(Task task) {
            db.collection("tasks")
                    .document(task.getId())
                    .update("completed", !task.isCompleted())
                    .addOnFailureListener(e ->
                            Toast.makeText(TodoActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }

        @Override
        public void onEdit(Task task) {
            showTaskDialog(task);
        }

        @Override
        public void onDelete(Task task) {
            new AlertDialog.Builder(TodoActivity.this)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.collection("tasks")
                                .document(task.getId())
                                .delete()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(TodoActivity.this, "Task deleted", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(TodoActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .show();
        }
    };

    private void refreshLists() {
        todoTasks.clear();
        completedTasks.clear();

        for (Task task : allTasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            } else {
                todoTasks.add(task);
            }
        }

        textPendingCount.setText(todoTasks.size() + " Pending");
        textDoneCount.setText(completedTasks.size() + " Done");

        todoAdapter.notifyDataSetChanged();
        completedAdapter.notifyDataSetChanged();
    }

    private void showTaskDialog(Task existingTask) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_task);

        TextView textDialogTitle = dialog.findViewById(R.id.textDialogTitle);
        TextInputEditText etTask = dialog.findViewById(R.id.etTask);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnAdd = dialog.findViewById(R.id.btnAdd);

        if (existingTask != null) {
            textDialogTitle.setText("Edit Task");
            btnAdd.setText("Save");
            etTask.setText(existingTask.getTitle());
        } else {
            textDialogTitle.setText("New Task");
            btnAdd.setText("Add");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String taskTitle = etTask.getText() != null ? etTask.getText().toString().trim() : "";

            if (taskTitle.isEmpty()) {
                etTask.setError("Enter task");
                return;
            }

            btnAdd.setEnabled(false);

            if (existingTask == null) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("title", taskTitle);
                taskMap.put("completed", false);
                taskMap.put("uid", uid);
                taskMap.put("createdAt", FieldValue.serverTimestamp());

                db.collection("tasks")
                        .add(taskMap)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            btnAdd.setEnabled(true);
                            Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } else {
                db.collection("tasks")
                        .document(existingTask.getId())
                        .update("title", taskTitle)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            btnAdd.setEnabled(true);
                            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(TodoActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}