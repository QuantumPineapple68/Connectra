package com.example.connectra.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.R;
import com.example.connectra.adapter.TaskAdapter;
import com.example.connectra.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ScheduleFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView rvTasks;
    private TextView tvNoTasks;
    private Button btnAddTask;

    private DatabaseReference tasksRef;
    private String selectedDate;

    private TaskAdapter taskAdapter;
    private List<Task> taskList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        // Initialize Views
        calendarView = view.findViewById(R.id.calendar_view);
        rvTasks = view.findViewById(R.id.rv_tasks);
        tvNoTasks = view.findViewById(R.id.tv_no_tasks);
        btnAddTask = view.findViewById(R.id.btn_add_task);

        // Initialize Firebase Database
        tasksRef = FirebaseDatabase.getInstance().getReference("Tasks").child(FirebaseAuth.getInstance().getUid());

        // Initialize RecyclerView
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList);
        rvTasks.setAdapter(taskAdapter);

        // Set default date to today
        selectedDate = getCurrentDate();

        // Fetch tasks for the selected date
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            fetchTasks();
        });

        btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Fetch initial tasks
        fetchTasks();

        return view;
    }

    private void fetchTasks() {
        tasksRef.child(selectedDate).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                        Task task = taskSnapshot.getValue(Task.class);
                        taskList.add(task);
                    }
                    taskAdapter.notifyDataSetChanged();
                    rvTasks.setVisibility(View.VISIBLE);
                    tvNoTasks.setVisibility(View.GONE);
                } else {
                    rvTasks.setVisibility(View.GONE);
                    tvNoTasks.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.item_task, null, false);
        EditText etTitle = dialogView.findViewById(R.id.tv_task_title);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();

            if (!TextUtils.isEmpty(title)) {
                addTaskToFirebase(title);
            } else {
                Toast.makeText(getContext(), "Task title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addTaskToFirebase(String title) {
        String taskId = tasksRef.child(selectedDate).push().getKey();
        HashMap<String, String> task = new HashMap<>();
        task.put("title", title);

        if (taskId != null) {
            tasksRef.child(selectedDate).child(taskId).setValue(task)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add task", Toast.LENGTH_SHORT).show());
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date(calendarView.getDate()));
    }
}
