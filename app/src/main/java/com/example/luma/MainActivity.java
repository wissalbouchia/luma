package com.example.luma;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView notesListView;
    private Button addNoteButton;
    private Button viewTrashButton;
    private Button viewArchivedNotesButton;
    private DatabaseHelper databaseHelper;
    private ArrayAdapter<String> adapter;
    private List<Note> notesList;
    private List<Note> filteredNotesList; // Liste filtrée pour la recherche
    private SearchView searchView;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des éléments UI
        initViews();

        // Initialisation de la base de données
        databaseHelper = new DatabaseHelper(this);

        // Charger toutes les notes
        loadNotes();

        // Configuration de la recherche
        setupSearch();

        // Configuration des écouteurs de clic
        setupClickListeners();

        // Configuration du menu contextuel
        registerForContextMenu(notesListView);

        // Créer un canal de notification pour Android 8.0+
        createNotificationChannel();

        // Demander la permission POST_NOTIFICATIONS si nécessaire (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Initialiser les éléments de l'interface
     */
    private void initViews() {
        searchView = findViewById(R.id.searchView);
        notesListView = findViewById(R.id.notesListView);
        addNoteButton = findViewById(R.id.addNoteButton);
        viewTrashButton = findViewById(R.id.viewTrashButton);
        viewArchivedNotesButton = findViewById(R.id.viewArchivedNotesButton);
    }

    /**
     * Configurer la recherche
     */
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });
    }

    /**
     * Configurer les écouteurs des boutons
     */
    private void setupClickListeners() {
        // Ajouter une nouvelle note
        addNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
            startActivityForResult(intent, 1);
        });

        // Voir la corbeille
        viewTrashButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TrashActivity.class);
            startActivity(intent);
        });

        // Voir les notes archivées
        viewArchivedNotesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ArchivedNotesActivity.class);
            startActivity(intent);
        });

        // Gérer les clics sur les notes
        notesListView.setOnItemClickListener((parent, view, position, id) -> {
            openNoteForEditing(filteredNotesList.get(position));
        });
    }

    /**
     * Ouvrir une note pour modification
     */
    private void openNoteForEditing(Note note) {
        Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
        intent.putExtra("NOTE_ID", note.getId());
        intent.putExtra("NOTE_TITLE", note.getTitle());
        intent.putExtra("NOTE_CONTENT", note.getContent());
        startActivityForResult(intent, 2);
    }

    /**
     * Charger toutes les notes depuis la base de données
     */
    private void loadNotes() {
        notesList = databaseHelper.getAllNotes();
        filteredNotesList = new ArrayList<>(notesList);
        updateListView();
    }

    /**
     * Filtrer les notes en fonction du texte de recherche
     */
    private void filterNotes(String query) {
        filteredNotesList.clear();

        if (TextUtils.isEmpty(query)) {
            filteredNotesList.addAll(notesList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Note note : notesList) {
                if (note.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        note.getContent().toLowerCase().contains(lowerCaseQuery)) {
                    filteredNotesList.add(note);
                }
            }
        }
        updateListView();
    }

    /**
     * Mettre à jour la liste des notes dans l'interface
     */
    private void updateListView() {
        if (adapter == null) {
            adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1,
                    getNoteTitles(filteredNotesList));
            notesListView.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(getNoteTitles(filteredNotesList));
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Obtenir les titres des notes
     */
    private List<String> getNoteTitles(List<Note> notes) {
        List<String> titles = new ArrayList<>();
        for (Note note : notes) {
            titles.add(note.getTitle());
        }
        return titles;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    @Override
    public void onCreateContextMenu(android.view.ContextMenu menu, View v,
                                    android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        Note selectedNote = filteredNotesList.get(position);

        if (item.getItemId() == R.id.delete_note) {
            databaseHelper.deleteNote(selectedNote.getId());
            loadNotes();
            return true;
        } else if (item.getItemId() == R.id.archive_note) {
            databaseHelper.archiveNote(selectedNote.getId(), true);
            loadNotes();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Créer un canal de notification pour Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "note_alarm_channel",
                    "Note Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}