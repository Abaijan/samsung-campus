package com.abaijan.todo.firebase;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.abaijan.todo.data.model.Task;
import com.abaijan.todo.provider.ToDo.ToDoItem;

import java.util.HashMap;
import java.util.Map;

public class FirestoreSyncService {

    private static final String TAG = "FirestoreSyncService";
    private final FirebaseFirestore db;
    private final ContentResolver contentResolver;
    private final String userId;

    public FirestoreSyncService(ContentResolver contentResolver, String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.contentResolver = contentResolver;
        this.userId = userId;
    }

    /**
     * Загружает задачи пользователя из Firestore в локальную SQLite
     */
    public void syncTasksFromFirestore(final SyncCallback callback) {
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("modTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int synced = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setId(doc.getId());
                            saveTaskToLocal(task);
                            synced++;
                        }
                    }
                    callback.onSuccess("Синхронизировано задач: " + synced);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing from Firestore", e);
                    callback.onError(e.getMessage());
                });
    }


    private void saveTaskToLocal(Task task) {
        // ════════════════════════════════════════════════════════
        // ПРОВЕРКА: существует ли задача с таким описанием?
        // ════════════════════════════════════════════════════════
        Cursor existingTask = contentResolver.query(
                ToDoItem.CONTENT_URI,
                new String[] { ToDoItem._ID, ToDoItem.MOD_TIME },
                ToDoItem.DESCRIPTION + " = ?",  // ← ИЗМЕНЕНО: проверяем по description
                new String[] { task.getDescription() },
                null
        );

        boolean taskExists = false;
        long existingId = -1;
        long localModTime = 0;

        if (existingTask != null) {
            if (existingTask.moveToFirst()) {
                taskExists = true;
                existingId = existingTask.getLong(0);

                // Проверяем modTime (если есть)
                int modTimeIndex = existingTask.getColumnIndex(ToDoItem.MOD_TIME);
                if (modTimeIndex >= 0 && !existingTask.isNull(modTimeIndex)) {
                    localModTime = existingTask.getLong(modTimeIndex);
                }
            }
            existingTask.close();
        }

        // ════════════════════════════════════════════════════════
        // Пропускаем если локальная версия новее
        // ════════════════════════════════════════════════════════
        if (taskExists && task.getModTime() < localModTime) {
            Log.d(TAG, "Skipping older version: " + task.getDescription());
            return;
        }

        // ════════════════════════════════════════════════════════
        // Подготовка данных
        // ════════════════════════════════════════════════════════
        ContentValues values = new ContentValues();
        values.put(ToDoItem.DESCRIPTION, task.getDescription());
        values.put(ToDoItem.CHECKED, task.isChecked() ? 1 : 0);
        values.put(ToDoItem.NOTE, task.getNote());
        values.put(ToDoItem.PRIORITY, task.getPriority());
        values.put(ToDoItem.CATEGORY_ID, task.getCategoryId());
        values.put(ToDoItem.PRIVATE, task.isPrivate() ? 1 : 0);
        values.put(ToDoItem.MOD_TIME, task.getModTime());

        if (task.getDueTime() != null) {
            values.put(ToDoItem.DUE_TIME, task.getDueTime());
        }
        if (task.getCompletedTime() != null) {
            values.put(ToDoItem.COMPLETED_TIME, task.getCompletedTime());
        }

        // ════════════════════════════════════════════════════════
        // Обновляем или вставляем
        // ════════════════════════════════════════════════════════
        if (taskExists) {
            // Обновляем существующую задачу
            int updated = contentResolver.update(
                    ToDoItem.CONTENT_URI,
                    values,
                    ToDoItem._ID + " = ?",
                    new String[] { String.valueOf(existingId) }
            );
            Log.d(TAG, "Updated existing task: " + task.getDescription() + " (rows: " + updated + ")");
        } else {
            // Вставляем новую задачу
            contentResolver.insert(ToDoItem.CONTENT_URI, values);
            Log.d(TAG, "Inserted new task: " + task.getDescription());
        }
    }


    public void uploadTaskToFirestore(Cursor itemCursor, final SyncCallback callback) {
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        Task task = cursorToTask(itemCursor);
        task.setUserId(userId);

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("userId", task.getUserId());
        taskMap.put("description", task.getDescription());
        taskMap.put("checked", task.isChecked());
        taskMap.put("note", task.getNote());
        taskMap.put("priority", task.getPriority());
        taskMap.put("categoryId", task.getCategoryId());
        taskMap.put("isPrivate", task.isPrivate());
        taskMap.put("modTime", task.getModTime());

        if (task.getDueTime() != null) {
            taskMap.put("dueTime", task.getDueTime());
        }
        if (task.getCompletedTime() != null) {
            taskMap.put("completedTime", task.getCompletedTime());
        }

        db.collection("tasks")
                .add(taskMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Task uploaded: " + documentReference.getId());
                    callback.onSuccess("Задача загружена в облако");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading task", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Конвертирует Cursor в Task
     */
    private Task cursorToTask(Cursor cursor) {
        Task task = new Task();

        int descIndex = cursor.getColumnIndex(ToDoItem.DESCRIPTION);
        if (descIndex >= 0) {
            task.setDescription(cursor.getString(descIndex));
        }

        int checkedIndex = cursor.getColumnIndex(ToDoItem.CHECKED);
        if (checkedIndex >= 0) {
            task.setChecked(cursor.getInt(checkedIndex) != 0);
        }

        int noteIndex = cursor.getColumnIndex(ToDoItem.NOTE);
        if (noteIndex >= 0 && !cursor.isNull(noteIndex)) {
            task.setNote(cursor.getString(noteIndex));
        }

        int priorityIndex = cursor.getColumnIndex(ToDoItem.PRIORITY);
        if (priorityIndex >= 0) {
            task.setPriority(cursor.getInt(priorityIndex));
        }

        int categoryIndex = cursor.getColumnIndex(ToDoItem.CATEGORY_ID);
        if (categoryIndex >= 0) {
            task.setCategoryId(cursor.getLong(categoryIndex));
        }

        int privateIndex = cursor.getColumnIndex(ToDoItem.PRIVATE);
        if (privateIndex >= 0) {
            task.setPrivate(cursor.getInt(privateIndex) != 0);
        }

        int dueTimeIndex = cursor.getColumnIndex(ToDoItem.DUE_TIME);
        if (dueTimeIndex >= 0 && !cursor.isNull(dueTimeIndex)) {
            task.setDueTime(cursor.getLong(dueTimeIndex));
        }

        int completedIndex = cursor.getColumnIndex(ToDoItem.COMPLETED_TIME);
        if (completedIndex >= 0 && !cursor.isNull(completedIndex)) {
            task.setCompletedTime(cursor.getLong(completedIndex));
        }

        int modTimeIndex = cursor.getColumnIndex(ToDoItem.MOD_TIME);
        if (modTimeIndex >= 0) {
            task.setModTime(cursor.getLong(modTimeIndex));
        }

        return task;
    }

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}