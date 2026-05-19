package com.sdm.agendanusantara.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite helper for Agenda Nusantara.
 * Manages two tables:
 *   - users  : stores login credentials (default: user / user)
 *   - tasks  : stores agenda items per user
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "agenda_nusantara.db"
        const val DATABASE_VERSION = 1

        /* ── users table ────────────────────────────────────────── */
        const val TABLE_USERS   = "users"
        const val COL_USER_ID   = "id"
        const val COL_USERNAME  = "username"
        const val COL_PASSWORD  = "password"

        /* ── tasks table ────────────────────────────────────────── */
        const val TABLE_TASKS        = "tasks"
        const val COL_TASK_ID        = "id"
        const val COL_TASK_TITLE     = "title"
        const val COL_TASK_DESC      = "description"
        const val COL_TASK_DUE_DATE  = "due_date"        // "YYYY-MM-DD"
        const val COL_TASK_CATEGORY  = "category"        // "penting" | "biasa"
        const val COL_TASK_DONE      = "is_completed"    // 0 | 1
        const val COL_TASK_USER_ID   = "user_id"

        /* category constants */
        const val CATEGORY_PENTING = "penting"
        const val CATEGORY_BIASA   = "biasa"
    }

    // ── onCreate ────────────────────────────────────────────────────
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID  INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT    UNIQUE NOT NULL,
                $COL_PASSWORD TEXT    NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_TASKS (
                $COL_TASK_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TASK_TITLE    TEXT    NOT NULL,
                $COL_TASK_DESC     TEXT,
                $COL_TASK_DUE_DATE TEXT,
                $COL_TASK_CATEGORY TEXT    NOT NULL DEFAULT '$CATEGORY_BIASA',
                $COL_TASK_DONE     INTEGER NOT NULL DEFAULT 0,
                $COL_TASK_USER_ID  INTEGER NOT NULL,
                FOREIGN KEY ($COL_TASK_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
            )
        """.trimIndent())

        // Seed default account
        db.insert(TABLE_USERS, null, ContentValues().apply {
            put(COL_USERNAME, "user")
            put(COL_PASSWORD, "user")
        })
    }

    // ── onUpgrade ────────────────────────────────────────────────────
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // ═══════════════════════════════════════════════════════════════
    //  USER operations
    // ═══════════════════════════════════════════════════════════════

    /** Returns true if username + password match a record. */
    fun validateUser(username: String, password: String): Boolean {
        val db     = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_USER_ID),
            "$COL_USERNAME = ? AND $COL_PASSWORD = ?",
            arrayOf(username, password),
            null, null, null
        )
        val valid = cursor.count > 0
        cursor.close()
        return valid
    }

    /** Returns the user_id for the given username, or -1 if not found. */
    fun getUserId(username: String): Int {
        val db     = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_USER_ID),
            "$COL_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )
        var id = -1
        if (cursor.moveToFirst()) id = cursor.getInt(0)
        cursor.close()
        return id
    }

    /**
     * Updates the password for an existing user.
     * @return true on success.
     */
    fun updatePassword(username: String, newPassword: String): Boolean {
        val db      = writableDatabase
        val values  = ContentValues().apply { put(COL_PASSWORD, newPassword) }
        val updated = db.update(TABLE_USERS, values, "$COL_USERNAME = ?", arrayOf(username))
        return updated > 0
    }

    // ═══════════════════════════════════════════════════════════════
    //  TASK operations
    // ═══════════════════════════════════════════════════════════════

    /** Insert a new task. Returns the new row id, or -1 on error. */
    fun insertTask(
        userId: Int,
        title: String,
        description: String,
        dueDate: String,
        category: String
    ): Long {
        val db     = writableDatabase
        val values = ContentValues().apply {
            put(COL_TASK_TITLE,    title)
            put(COL_TASK_DESC,     description)
            put(COL_TASK_DUE_DATE, dueDate)
            put(COL_TASK_CATEGORY, category)
            put(COL_TASK_DONE,     0)
            put(COL_TASK_USER_ID,  userId)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    /** Update an existing task. */
    fun updateTask(
        taskId: Int,
        title: String,
        description: String,
        dueDate: String,
        category: String
    ): Boolean {
        val db     = writableDatabase
        val values = ContentValues().apply {
            put(COL_TASK_TITLE,    title)
            put(COL_TASK_DESC,     description)
            put(COL_TASK_DUE_DATE, dueDate)
            put(COL_TASK_CATEGORY, category)
        }
        return db.update(TABLE_TASKS, values, "$COL_TASK_ID = ?", arrayOf(taskId.toString())) > 0
    }

    /** Toggle the is_completed flag of a task. */
    fun toggleTaskDone(taskId: Int, isDone: Boolean): Boolean {
        val db     = writableDatabase
        val values = ContentValues().apply { put(COL_TASK_DONE, if (isDone) 1 else 0) }
        return db.update(TABLE_TASKS, values, "$COL_TASK_ID = ?", arrayOf(taskId.toString())) > 0
    }

    /** Delete a task by id. */
    fun deleteTask(taskId: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_TASKS, "$COL_TASK_ID = ?", arrayOf(taskId.toString())) > 0
    }

    /** Returns all tasks for a user as a List<Task> (sorted by due_date). */
    fun getAllTasksAsList(userId: Int): MutableList<com.sdm.agendanusantara.Task> {
        val list   = mutableListOf<com.sdm.agendanusantara.Task>()
        val cursor = getAllTasks(userId)
        while (cursor.moveToNext()) {
            list.add(
                com.sdm.agendanusantara.Task(
                    id          = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_ID)),
                    title       = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_DESC)) ?: "",
                    dueDate     = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_DUE_DATE)) ?: "",
                    category    = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_CATEGORY)),
                    isDone      = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_DONE)) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    /** Fetch all tasks for a user, ordered by due_date. */
    fun getAllTasks(userId: Int): Cursor = readableDatabase.query(
        TABLE_TASKS, null,
        "$COL_TASK_USER_ID = ?",
        arrayOf(userId.toString()),
        null, null, "$COL_TASK_DUE_DATE ASC"
    )

    /** Fetch tasks filtered by category. */
    fun getTasksByCategory(userId: Int, category: String): Cursor = readableDatabase.query(
        TABLE_TASKS, null,
        "$COL_TASK_USER_ID = ? AND $COL_TASK_CATEGORY = ?",
        arrayOf(userId.toString(), category),
        null, null, "$COL_TASK_DUE_DATE ASC"
    )

    /**
     * Count tasks completed (is_completed=1) on a specific date for the weekly chart.
     * due_date is stored as "YYYY-MM-DD".
     */
    fun countTasksDoneOnDate(userId: Int, date: String): Int {
        val cursor = readableDatabase.query(
            TABLE_TASKS, arrayOf("COUNT(*)"),
            "$COL_TASK_USER_ID = ? AND $COL_TASK_DONE = 1 AND $COL_TASK_DUE_DATE = ?",
            arrayOf(userId.toString(), date),
            null, null, null
        )
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }

    /** Count tasks for a user (optionally filtered by category and/or done status). */
    fun countTasks(userId: Int, category: String? = null, isDone: Boolean? = null): Int {
        val sb     = StringBuilder("$COL_TASK_USER_ID = ?")
        val args   = mutableListOf(userId.toString())
        if (category != null) { sb.append(" AND $COL_TASK_CATEGORY = ?"); args += category }
        if (isDone  != null) { sb.append(" AND $COL_TASK_DONE = ?"); args += (if (isDone) "1" else "0") }

        val cursor = readableDatabase.query(
            TABLE_TASKS, arrayOf("COUNT(*)"),
            sb.toString(), args.toTypedArray(),
            null, null, null
        )
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()
        return count
    }
}
