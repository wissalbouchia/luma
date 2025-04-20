package com.example.luma;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    private ListView trashListView;
    private TextView emptyTrashTextView;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        trashListView = findViewById(R.id.trashListView);
        emptyTrashTextView = findViewById(R.id.emptyTrashTextView);
        databaseHelper = new DatabaseHelper(this);

        loadDeletedNotes();

        // Ajouter un menu contextuel pour restaurer/vider définitivement
        registerForContextMenu(trashListView);
    }

    private void loadDeletedNotes() {
        List<Note> deletedNotes = databaseHelper.getDeletedNotes();

        if (deletedNotes.isEmpty()) {
            emptyTrashTextView.setVisibility(View.VISIBLE);
            trashListView.setVisibility(View.GONE);
        } else {
            emptyTrashTextView.setVisibility(View.GONE);
            trashListView.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1,
                    deletedNotes.stream().map(Note::getTitle).toArray(String[]::new));
            trashListView.setAdapter(adapter);
        }
    }

    @Override
    public void onCreateContextMenu(android.view.ContextMenu menu, View v,
                                    android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.trash_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        Note selectedNote = databaseHelper.getDeletedNotes().get(position);

        if (item.getItemId() == R.id.restore_note) {
            databaseHelper.restoreNote(selectedNote.getId());
            loadDeletedNotes();
            return true;
        } else if (item.getItemId() == R.id.empty_note) {
            databaseHelper.emptyNote(selectedNote.getId());
            loadDeletedNotes();
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
