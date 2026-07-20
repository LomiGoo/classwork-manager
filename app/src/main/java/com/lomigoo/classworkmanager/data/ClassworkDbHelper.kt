package com.lomigoo.classworkmanager.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ClassworkDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_COMPLETED INTEGER DEFAULT 0")
        }
    }

    fun insertClasswork(course: String, action: String, created: String, target: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE, course)
            put(COLUMN_ACTION, action)
            put(COLUMN_CREATED, created)
            put(COLUMN_TARGET, target)
            put(COLUMN_COMPLETED, 0)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllClasswork(): List<Classwork> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)
        val list = mutableListOf<Classwork>()

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
                val courseName = getString(getColumnIndexOrThrow(COLUMN_COURSE))
                val actionDescription = getString(getColumnIndexOrThrow(COLUMN_ACTION))
                val dateCreated = getString(getColumnIndexOrThrow(COLUMN_CREATED))
                val dateTarget = getString(getColumnIndexOrThrow(COLUMN_TARGET))
                val isCompleted = getInt(getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1
                list.add(Classwork(id, courseName, actionDescription, dateCreated, dateTarget, isCompleted))
            }
            close()
        }
        return list
    }

    fun updateClasswork(classwork: Classwork): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE, classwork.courseName)
            put(COLUMN_ACTION, classwork.actionDescription)
            put(COLUMN_TARGET, classwork.dateTarget)
            put(COLUMN_COMPLETED, if (classwork.isCompleted) 1 else 0)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(classwork.id.toString()))
    }

    fun deleteClasswork(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    companion object {
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "classwork_direct.db"
        const val TABLE_NAME = "classwork"
        const val COLUMN_ID = "id"
        const val COLUMN_COURSE = "courseName"
        const val COLUMN_ACTION = "actionDescription"
        const val COLUMN_CREATED = "dateCreated"
        const val COLUMN_TARGET = "dateTarget"
        const val COLUMN_COMPLETED = "isCompleted"

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_COURSE TEXT," +
                    "$COLUMN_ACTION TEXT," +
                    "$COLUMN_CREATED TEXT," +
                    "$COLUMN_TARGET TEXT," +
                    "$COLUMN_COMPLETED INTEGER DEFAULT 0)"
    }
}
