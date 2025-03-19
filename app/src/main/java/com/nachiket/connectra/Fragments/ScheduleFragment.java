package com.nachiket.connectra.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nachiket.connectra.ChatActivity;
import com.nachiket.connectra.ConnectionActivity;
import com.nachiket.connectra.ConnectionRequestsActivity;
import com.nachiket.connectra.R;
import com.nachiket.connectra.adapter.TaskAdapter;
import com.nachiket.connectra.model.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.utility.MessageFilter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView rvTasks;
    private TextView tvNoTasks, tvConnectedPartner;
    private FloatingActionButton btnAddTask;
    private ProgressBar progressBar;
    private List<String> connectedUserIds = new ArrayList<>();
    private Map<DatabaseReference, ValueEventListener> taskListeners = new HashMap<>();
    private Map<String, List<Task>> userTasksMap = new HashMap<>();
    private Button btnManageConnections;
    private Button test;

    private DatabaseReference tasksRef;
    private String selectedDate;

    private TaskAdapter taskAdapter;
    private List<Task> taskList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        Window window = requireActivity().getWindow();
        int statusBarColor = getResources().getColor(R.color.white, requireContext().getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        btnManageConnections = view.findViewById(R.id.btn_manage_connections);
        btnManageConnections.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ConnectionActivity.class))
        );

        // Initialize Views
        calendarView = view.findViewById(R.id.calendar_view);
        rvTasks = view.findViewById(R.id.rv_tasks);
        tvNoTasks = view.findViewById(R.id.tv_no_tasks);
        btnAddTask = view.findViewById(R.id.btn_add_task);
        progressBar = view.findViewById(R.id.pgBar);
        test = view.findViewById(R.id.test);
        tvConnectedPartner = view.findViewById(R.id.tv_connected_partner);
        updateConnectedPartnerUI();

        // Initialize Firebase Database
        tasksRef = FirebaseDatabase.getInstance().getReference("Tasks").child(FirebaseAuth.getInstance().getUid());

        // Initialize RecyclerView
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList);
        rvTasks.setAdapter(taskAdapter);

        // Set default date to today
        selectedDate = getCurrentDate();
        calendarView.setDate(System.currentTimeMillis(), false, true);
        progressBar.setVisibility(View.VISIBLE);

        // Listen for date changes
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String formattedMonth = String.format(Locale.getDefault(), "%02d", month + 1);
            String formattedDay = String.format(Locale.getDefault(), "%02d", dayOfMonth);
            selectedDate = year + "-" + formattedMonth + "-" + formattedDay;
            progressBar.setVisibility(View.VISIBLE);
            setupTaskListeners(); // Refresh listeners for new date
        });

        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ConnectionRequestsActivity.class);
                startActivity(intent);
            }
        });

        taskAdapter = new TaskAdapter(taskList);
        taskAdapter.setOnDeleteClickListener(task -> deleteTask(task));
        rvTasks.setAdapter(taskAdapter);

        btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        taskAdapter.setOnCheckListener((task, isChecked) -> {
            final String currentUserId = FirebaseAuth.getInstance().getUid();
            final String taskOwnerId = (task.getOwnerId() != null) ? task.getOwnerId() : currentUserId;

            // Update the checked status for the current user
            DatabaseReference currentUserTaskRef = FirebaseDatabase.getInstance().getReference("Tasks")
                    .child(currentUserId)
                    .child(selectedDate)
                    .child(task.getId())
                    .child("checked");

            currentUserTaskRef.setValue(isChecked)
                    .addOnSuccessListener(aVoid -> {
                        String partnerId = null;

                        if (currentUserId.equals(taskOwnerId)) {
                            if (!connectedUserIds.isEmpty()) {
                                partnerId = connectedUserIds.get(0);
                            }
                        } else {
                            partnerId = taskOwnerId;
                        }

                        if (partnerId != null) {
                            FirebaseDatabase.getInstance().getReference("Tasks")
                                    .child(partnerId)
                                    .child(selectedDate)
                                    .child(task.getId())
                                    .child("checked")
                                    .setValue(isChecked);
                        }
                    });
        });

        // Add in onCreateView()
        DatabaseReference connRef = FirebaseDatabase.getInstance().getReference("Connections")
                .child(FirebaseAuth.getInstance().getUid());
        connRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateConnectedPartnerUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });


        fetchConnectedUsers();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove all task listeners
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : taskListeners.entrySet()) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        taskListeners.clear();
    }

    private void fetchConnectedUsers() {
        DatabaseReference connectionsRef = FirebaseDatabase.getInstance().getReference("Connections")
                .child(FirebaseAuth.getInstance().getUid());

        connectionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                connectedUserIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if ("connected".equals(ds.child("status").getValue(String.class))) {
                        connectedUserIds.add(ds.getKey());
                    }
                }
                setupTaskListeners();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("Connection error: " + error.getMessage());
            }
        });
    }

    private void setupTaskListeners() {
        for (DatabaseReference ref : taskListeners.keySet()) {
            ref.removeEventListener(taskListeners.get(ref));
        }
        taskListeners.clear();
        userTasksMap.clear();

        // Add listener for current user
        addUserTaskListener(FirebaseAuth.getInstance().getUid());

        // Add listeners for connected users
        for (String userId : connectedUserIds) {
            addUserTaskListener(userId);
        }
    }
    private void addUserTaskListener(String userId) {
        DatabaseReference userTaskRef = FirebaseDatabase.getInstance().getReference("Tasks")
                .child(userId)
                .child(selectedDate);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processTasks(userId, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("Task load error: " + error.getMessage());
            }
        };

        userTaskRef.addValueEventListener(listener);
        taskListeners.put(userTaskRef, listener);
    }

    private void processTasks(String userId, DataSnapshot snapshot) {
        List<Task> tasks = new ArrayList<>();
        for (DataSnapshot taskSnap : snapshot.getChildren()) {
            Task task = taskSnap.getValue(Task.class);
            if (task != null) {
                task.setId(taskSnap.getKey());
                tasks.add(task); // OwnerId is already set from task data
            }
        }
        userTasksMap.put(userId, tasks);
        aggregateTasks();
    }

    private void aggregateTasks() {
        Map<String, Task> taskMap = new HashMap<>();
        for (List<Task> tasks : userTasksMap.values()) {
            for (Task task : tasks) {
                // Use task ID as key to avoid duplicates
                taskMap.put(task.getId(), task);
            }
        }
        taskList.clear();
        taskList.addAll(taskMap.values());
        taskAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void updateUI() {
        if (taskList.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
        progressBar.setVisibility(View.GONE);
    }

    private void deleteTask(Task task) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId.equals(task.getOwnerId())) {
            tasksRef.child(selectedDate).child(task.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        for (String userId : connectedUserIds) {
                            FirebaseDatabase.getInstance().getReference("Tasks")
                                    .child(userId)
                                    .child(selectedDate)
                                    .child(task.getId())
                                    .removeValue();
                        }
                        snackbar("Task deleted");
                    });
        } else {
            toast("Only the owner can delete this task");
        }
    }


    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null, false);
        EditText etTitle = dialogView.findViewById(R.id.tv_task_title);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();

            if (!TextUtils.isEmpty(title)) {
                if (MessageFilter.containsInappropriateContent(title)) {
                    Toast.makeText(getContext(),
                            "Can't use inappropriate words",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                addTaskToFirebase(title);
            } else {
                toast("Task title cannot be empty");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addTaskToFirebase(String title) {
        String taskId = FirebaseDatabase.getInstance().getReference().push().getKey();
        HashMap<String, Object> taskMap = new HashMap<>();
        taskMap.put("title", title);
        taskMap.put("checked", false);
        taskMap.put("ownerId", FirebaseAuth.getInstance().getUid());

        // Save to current user
        tasksRef.child(selectedDate).child(taskId).setValue(taskMap)
                .addOnSuccessListener(aVoid -> {
                    // Propagate to connected users
                    for (String userId : connectedUserIds) {
                        FirebaseDatabase.getInstance().getReference("Tasks")
                                .child(userId)
                                .child(selectedDate)
                                .child(taskId)
                                .setValue(taskMap);
                    }
                    snackbar("Task added");
                });
    }

    private void updateConnectedPartnerUI() {
        DatabaseReference connRef = FirebaseDatabase.getInstance().getReference("Connections")
                .child(FirebaseAuth.getInstance().getUid());

        connRef.orderByChild("status").equalTo("connected").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot ds : snapshot.getChildren()) {
                        String partnerId = ds.getKey();
                        FirebaseDatabase.getInstance().getReference("Users").child(partnerId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        String username = userSnapshot.child("username").getValue(String.class);
                                        tvConnectedPartner.setText("Connected: @" + username);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                        return;
                    }
                }
                tvConnectedPartner.setText("No connected partner");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date(calendarView.getDate()));
    }


    private void snackbar(String msg){
        Snackbar snackbar = Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        params.bottomMargin = 180; // Adjust the value as needed
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    private void toast(String msg){
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}