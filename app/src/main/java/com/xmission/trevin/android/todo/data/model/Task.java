package com.abaijan.todo.data.model;

public class Task {
    private String id;
    private String userId;          // КЛЮЧЕВОЕ ПОЛЕ!
    private String description;
    private boolean checked;
    private String note;
    private Long dueTime;
    private Long completedTime;
    private long categoryId;
    private int priority;
    private boolean isPrivate;
    private long modTime;

    // Пустой конструктор для Firestore
    public Task() {}

    public Task(String userId, String description) {
        this.userId = userId;
        this.description = description;
        this.checked = false;
        this.modTime = System.currentTimeMillis();
    }

    // Все геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Long getDueTime() { return dueTime; }
    public void setDueTime(Long dueTime) { this.dueTime = dueTime; }

    public Long getCompletedTime() { return completedTime; }
    public void setCompletedTime(Long completedTime) { this.completedTime = completedTime; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public long getModTime() { return modTime; }
    public void setModTime(long modTime) { this.modTime = modTime; }
}