package com.example.connectra.Fragments;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.R;
import com.example.connectra.adapter.TileAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TileAdapter tileAdapter;
    private List<NewUser> userList;
    private DatabaseReference databaseRef;
    TextView hi;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        hi = view.findViewById(R.id.welcome_name);

        // Initialize Realtime Database and RecyclerView
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        recyclerView = view.findViewById(R.id.recycler_view_home);

        // Dynamically set the span count
        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(8)); // 16dp spacing


        // Initialize user list and adapter
        userList = new ArrayList<>();
        tileAdapter = new TileAdapter(getContext(), userList);
        recyclerView.setAdapter(tileAdapter);

        fetchUserData();
        fetchUsersFromDatabase();

        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update span count on orientation change
        int spanCount = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        tileAdapter.notifyDataSetChanged();
    }



    private void fetchUserData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        databaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("name").getValue(String.class);
                    if (getActivity() != null && fullName != null) {
                        hi.setText("Hi, " + fullName);
                    }
                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUsersFromDatabase() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear(); // Clear the list before adding new data
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (!userId.equals(currentUserId)) { // Exclude current user's profile
                        String name = userSnapshot.child("name").getValue(String.class);
                        String myskill = userSnapshot.child("myskill").getValue(String.class);
                        String goalskill = userSnapshot.child("goalskill").getValue(String.class);
                        String gender = userSnapshot.child("gender").getValue(String.class);
                        String age = userSnapshot.child("age").getValue(String.class);
                        String bio = userSnapshot.child("bio").getValue(String.class);
                        String userName = userSnapshot.child("username").getValue(String.class);
                        String profileImage = userSnapshot.child("profileImage").getValue(String.class);

                        Log.e("test1",profileImage+"");
                        // Add user to the list
                        userList.add(new NewUser(name, myskill, goalskill, gender, age, userId, userName, bio, profileImage));
                    }
                }
                // Notify adapter of data changes
                tileAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public GridSpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = spacing;
            outRect.right = spacing;
            outRect.bottom = spacing;

            // Add top margin only for the first row to avoid double space between items
            if (parent.getChildAdapterPosition(view) < 2) {
                outRect.top = spacing;
            }
        }
    }
}
