package com.example.luma;

import android.content.Intent;
import android.content.ContentValues;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextWatcher;
import android.text.Editable;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.text.TextUtils;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.text.DateFormat;
import java.util.Calendar;

public class AddEditNoteActivity extends AppCompatActivity {
    // Éléments de l'interface
    private EditText titleEditText, contentEditText;
    private Button addTableButton, previewButton, shareButton, setAlarmButton;
    private TextView contentTextView;

    // Variables pour gérer la base de données
    private DatabaseHelper databaseHelper;
    private boolean isEditMode = false;
    private boolean isPreviewMode = false;
    private int noteId;

    // Constantes pour les requêtes
    private static final int REQUEST_CODE_CREATE_FILE = 1002;

    // Alarme
    private String alarmTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        // Initialiser les éléments de l'interface
        initViews();

        // Configurer la base de données
        databaseHelper = new DatabaseHelper(this);

        // Vérifier si nous sommes en mode édition
        checkEditMode();

        // Configurer les écouteurs des boutons
        setupButtonListeners();

        // Configurer le TextWatcher pour le sauvegarde automatique
        setupTextWatcher();

        // Enregistrer le menu contextuel (pour le bouton de partage)
        registerForContextMenu(shareButton);
    }

    /**
     * Initialiser les éléments de l'interface
     */
    private void initViews() {
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        contentTextView = findViewById(R.id.contentTextView);
        addTableButton = findViewById(R.id.addTableButton);
        previewButton = findViewById(R.id.previewButton);
        shareButton = findViewById(R.id.shareButton);
        setAlarmButton = findViewById(R.id.setAlarmButton); // Bouton pour définir une alarme
    }

    /**
     * Vérifier si nous sommes en mode édition
     */
    private void checkEditMode() {
        if (getIntent().hasExtra("NOTE_ID")) {
            isEditMode = true;
            noteId = getIntent().getIntExtra("NOTE_ID", -1);
            String noteTitle = getIntent().getStringExtra("NOTE_TITLE");
            String noteContent = getIntent().getStringExtra("NOTE_CONTENT");
            titleEditText.setText(noteTitle);
            contentEditText.setText(noteContent);
        }
    }

    /**
     * Configurer les écouteurs des boutons
     */
    private void setupButtonListeners() {
        // Bouton pour ajouter un tableau
        addTableButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTableActivity.class);
            startActivityForResult(intent, 1);
        });

        // Bouton pour basculer entre le mode prévisualisation et le mode édition
        previewButton.setOnClickListener(v -> togglePreviewMode());

        // Bouton pour partager la note
        shareButton.setOnClickListener(v -> shareNote());

        // Bouton pour définir une alarme
        setAlarmButton.setOnClickListener(v -> showDateTimePicker());
    }

    /**
     * Configurer le TextWatcher pour le sauvegarde automatique
     */
    private void setupTextWatcher() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveNoteAutomatically();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        titleEditText.addTextChangedListener(textWatcher);
        contentEditText.addTextChangedListener(textWatcher);
    }

    /**
     * Basculer entre le mode prévisualisation et le mode édition
     */
    private void togglePreviewMode() {
        if (isPreviewMode) {
            // Retourner au mode édition
            contentEditText.setVisibility(View.VISIBLE);
            contentTextView.setVisibility(View.GONE);
            previewButton.setText("Prévisualiser");
        } else {
            // Passer au mode prévisualisation
            String markdownContent = contentEditText.getText().toString();
            displayMarkdownContent(markdownContent);
            contentEditText.setVisibility(View.GONE);
            contentTextView.setVisibility(View.VISIBLE);
            previewButton.setText("Éditer");
        }
        isPreviewMode = !isPreviewMode;
    }

    /**
     * Afficher le contenu de la note au format Markdown
     */
    private void displayMarkdownContent(String markdownContent) {
        if (TextUtils.isEmpty(markdownContent)) {
            contentTextView.setText("");
            return;
        }
        Markwon markwon = Markwon.builder(this)
                .usePlugin(TablePlugin.create(this))
                .build();
        markwon.setMarkdown(contentTextView, markdownContent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Résultat de l'ajout d'un tableau
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String markdownTable = data.getStringExtra("MARKDOWN_TABLE");
            contentEditText.append("\n" + markdownTable);
        }

        // Résultat de l'exportation d'un fichier
        else if (requestCode == REQUEST_CODE_CREATE_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                Toast.makeText(this, "L'exportation sera exécutée ici", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Sauvegarder la note automatiquement lors des modifications
     */
    private void saveNoteAutomatically() {
        String title = titleEditText.getText().toString();
        String content = contentEditText.getText().toString();

        if (isEditMode) {
            boolean isUpdated = databaseHelper.updateNote(new Note(noteId, title, content));
            if (isUpdated) {
                setResult(RESULT_OK);
            }
        } else {
            Note newNote = new Note(title, content);
            boolean isAdded = databaseHelper.addNote(newNote);
            if (isAdded) {
                noteId = getLastInsertedId();
                isEditMode = true;
                setResult(RESULT_OK);
            }
        }
    }

    /**
     * Obtenir le dernier ID inséré
     */
    private int getLastInsertedId() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(id) FROM " + DatabaseHelper.TABLE_NAME, null);
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    /**
     * Partager la note via d'autres applications
     */
    private void shareNote() {
        String title = titleEditText.getText().toString();
        String content = contentEditText.getText().toString();
        String fullText = title + "\n" + content;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, fullText);

        Intent chooser = Intent.createChooser(shareIntent, "Partager la note via");
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, "Aucune application de partage installée", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Exporter la note en PDF
     */
    private void exportToPdf() {
        String title = titleEditText.getText().toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, title + ".pdf");
        startActivityForResult(intent, REQUEST_CODE_CREATE_FILE);
    }

    /**
     * Exporter la note en Word
     */
    private void exportToWord() {
        String title = titleEditText.getText().toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/msword");
        intent.putExtra(Intent.EXTRA_TITLE, title + ".doc");
        startActivityForResult(intent, REQUEST_CODE_CREATE_FILE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.export_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_share) {
            shareNote();
            return true;
        } else if (itemId == R.id.action_export_pdf) {
            exportToPdf();
            return true;
        } else if (itemId == R.id.action_export_word) {
            exportToWord();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Afficher le sélecteur de date et d'heure pour définir une alarme
     */
    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (view1, selectedHour, selectedMinute) -> {
                                calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
                                alarmTime = DateFormat.getDateTimeInstance().format(calendar.getTime());
                                setAlarm(calendar.getTimeInMillis());
                                Toast.makeText(this, "Alarme définie pour : " + alarmTime, Toast.LENGTH_SHORT).show();
                            }, hour, minute, true);
                    timePickerDialog.show();
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Définir une alarme
     */
    private void setAlarm(long alarmTimeInMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmeReceiver.class);
        intent.putExtra("NOTE_TITLE", titleEditText.getText().toString());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, noteId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeInMillis, pendingIntent);
        }

        // Enregistrer l'heure de l'alarme dans la base de données
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("alarm_time", alarmTime);
        db.update(DatabaseHelper.TABLE_NAME, values, DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(noteId)});
    }
}