package com.example.luma;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Constantes pour la base de données
    private static final String DATABASE_NAME = "Notes.db";
    private static final int DATABASE_VERSION = 4; // Version mise à jour pour inclure 'alarm_time'
    public static final String TABLE_NAME = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Création de la table avec toutes les colonnes, y compris 'alarm_time'
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CONTENT + " TEXT, " +
                "table_data TEXT, " + // Pour stocker les données de table
                "list_items TEXT, " + // Pour stocker les éléments de liste
                "audio_file_path TEXT, " + // Pour stocker le chemin du fichier audio
                "image_file_path TEXT, " + // Pour stocker le chemin de l'image
                "is_archived INTEGER DEFAULT 0, " + // Pour les notes archivées
                "is_deleted INTEGER DEFAULT 0, " + // Pour la corbeille
                "alarm_time TEXT)"; // Nouvelle colonne pour l'alarme
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migration pour ajouter la colonne 'is_deleted' si elle n'existe pas
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN is_deleted INTEGER DEFAULT 0");
        }
        // Migration pour ajouter la colonne 'alarm_time'
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN alarm_time TEXT");
        }
    }

    // Ajouter une nouvelle note
    public boolean addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put("alarm_time", note.getAlarmTime()); // Ajout de l'heure de l'alarme

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    // Récupérer toutes les notes non supprimées
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE is_deleted=0", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                String alarmTime = cursor.getString(cursor.getColumnIndexOrThrow("alarm_time")); // Récupérer l'heure de l'alarme
                Note note = new Note(id, title, content);
                note.setAlarmTime(alarmTime); // Définir l'heure de l'alarme
                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }

    // Mettre à jour une note existante
    public boolean updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put("alarm_time", note.getAlarmTime()); // Mettre à jour l'heure de l'alarme

        int result = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(note.getId())});
        return result > 0;
    }

    // Archiver/désarchiver une note
    public boolean archiveNote(int id, boolean isArchived) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_archived", isArchived ? 1 : 0);

        int result = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // Marquer une note comme supprimée (déplacement vers la corbeille)
    public boolean deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_deleted", 1);

        int result = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // Supprimer définitivement une note
    public boolean emptyNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // Restaurer une note depuis la corbeille
    public boolean restoreNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_deleted", 0);

        int result = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // Récupérer toutes les notes dans la corbeille
    public List<Note> getDeletedNotes() {
        List<Note> deletedNotes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE is_deleted=1", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                String alarmTime = cursor.getString(cursor.getColumnIndexOrThrow("alarm_time")); // Récupérer l'heure de l'alarme
                Note note = new Note(id, title, content);
                note.setAlarmTime(alarmTime); // Définir l'heure de l'alarme
                deletedNotes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return deletedNotes;
    }

    // Récupérer toutes les notes archivées
    public List<Note> getArchivedNotes() {
        List<Note> archivedNotes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE is_archived=1 AND is_deleted=0", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                String alarmTime = cursor.getString(cursor.getColumnIndexOrThrow("alarm_time")); // Récupérer l'heure de l'alarme
                Note note = new Note(id, title, content);
                note.setAlarmTime(alarmTime); // Définir l'heure de l'alarme
                archivedNotes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return archivedNotes;
    }
}