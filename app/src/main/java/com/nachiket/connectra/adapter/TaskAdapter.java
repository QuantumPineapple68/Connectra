package com.nachiket.connectra.adapter;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.nachiket.connectra.model.Task;
import com.nachiket.connectra.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Task task);
    }

    public interface OnCheckListener {
        void onCheck(Task task, boolean isChecked);
    }
    private OnCheckListener checkListener;

    public void setOnCheckListener(OnCheckListener listener) {
        this.checkListener = listener;
    }

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTaskTitle.setText(task.getTitle());
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isChecked());

        if (task.isChecked()) {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.GRAY);
        } else {
            holder.tvTaskTitle.setPaintFlags(0);
            holder.tvTaskTitle.setTextColor(Color.BLACK);
        }

        String currentUserId = FirebaseAuth.getInstance().getUid();
        String ownerId = task.getOwnerId();
        if (ownerId != null && !ownerId.equals(currentUserId)) {
            Drawable groupDrawable = ContextCompat.getDrawable(holder.tvTaskTitle.getContext(), R.drawable.ic_group);
            int marginInPx = 0;
            try {
                marginInPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        32,
                        holder.tvTaskTitle.getContext().getResources().getDisplayMetrics());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // Create an InsetDrawable with a right inset (end margin)
            InsetDrawable insetDrawable = new InsetDrawable(groupDrawable, 0, 0, marginInPx, 0);

            holder.tvTaskTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, insetDrawable, null);
        } else {
            holder.tvTaskTitle.setCompoundDrawables(null, null, null, null);
        }


        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkListener != null) {
                checkListener.onCheck(task, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Clicked: " + task.getTitle(), Toast.LENGTH_SHORT).show()
        );

        holder.deleteImage.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public CheckBox checkBox;
        TextView tvTaskTitle;
        ImageView deleteImage;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            deleteImage = itemView.findViewById(R.id.del);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}