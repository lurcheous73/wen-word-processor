package uk.brimstone.simpletype;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private DocumentRepository repository;
    private final ArrayList<DocumentRepository.DocInfo> documents = new ArrayList<>();
    private DocumentAdapter adapter;
    private Spinner sortSpinner;
    private TextView totalWords;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new DocumentRepository(this);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));

        TextView title = new TextView(this);
        title.setText("SimpleType");
        title.setTextSize(28);
        title.setTypeface(FontManager.get(this, FontManager.OPEN_DYSLEXIC));
        root.addView(title);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        Button add = new Button(this);
        add.setText("New document");
        add.setOnClickListener(v -> createDocument());
        controls.addView(add);

        sortSpinner = new Spinner(this);
        String[] sorts = {"A–Z", "Z–A", "Newest altered", "Oldest altered"};
        sortSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sorts));
        controls.addView(sortSpinner, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(controls);

        totalWords = new TextView(this);
        totalWords.setTextSize(16);
        totalWords.setPadding(0, dp(8), 0, dp(8));
        root.addView(totalWords);

        ListView list = new ListView(this);
        adapter = new DocumentAdapter();
        list.setAdapter(adapter);
        list.setDividerHeight(1);
        list.setOnItemClickListener((parent, view, position, id) -> openDocument(documents.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(documents.get(position));
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                sortDocuments();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        setContentView(root);
    }

    private void createDocument() {
        EditText name = new EditText(this);
        name.setHint("Document name");
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int pad = dp(20);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, 0, pad, 0);
        box.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("New document")
                .setView(box)
                .setPositiveButton("Create", (d, which) -> {
                    try {
                        String fileName = repository.create(name.getText().toString());
                        openDocument(new DocumentRepository.DocInfo(
                                fileName, name.getText().toString(), System.currentTimeMillis(), 0));
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not create document", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        name.requestFocus();
    }

    private void openDocument(DocumentRepository.DocInfo info) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_FILE_NAME, info.fileName);
        startActivity(intent);
    }

    private void confirmDelete(DocumentRepository.DocInfo info) {
        new AlertDialog.Builder(this)
                .setTitle("Delete “" + info.title + "”?")
                .setMessage("This permanently deletes the document.")
                .setPositiveButton("Delete", (d, w) -> {
                    repository.delete(info.fileName);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refresh() {
        documents.clear();
        documents.addAll(repository.list());
        sortDocuments();
        totalWords.setText(String.format(Locale.UK, "%d documents  •  %,d words in all documents",
                documents.size(), repository.totalWordCount()));
    }

    private void sortDocuments() {
        if (adapter == null || sortSpinner == null) return;
        int mode = sortSpinner.getSelectedItemPosition();
        Comparator<DocumentRepository.DocInfo> alpha =
                Comparator.comparing(d -> d.title.toLowerCase(Locale.UK));
        if (mode == 0) documents.sort(alpha);
        else if (mode == 1) documents.sort(alpha.reversed());
        else if (mode == 2) documents.sort((a, b) -> Long.compare(b.modified, a.modified));
        else documents.sort(Comparator.comparingLong(d -> d.modified));
        adapter.notifyDataSetChanged();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class DocumentAdapter extends ArrayAdapter<DocumentRepository.DocInfo> {
        DocumentAdapter() {
            super(MainActivity.this, android.R.layout.simple_list_item_2, documents);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            TextView title;
            TextView detail;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
                title = (TextView) row.getChildAt(0);
                detail = (TextView) row.getChildAt(1);
            } else {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(10), dp(12), dp(10), dp(12));
                title = new TextView(MainActivity.this);
                title.setTextSize(20);
                detail = new TextView(MainActivity.this);
                detail.setTextSize(14);
                row.addView(title);
                row.addView(detail);
            }
            DocumentRepository.DocInfo info = documents.get(position);
            title.setText(info.title);
            DateFormat fmt = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.UK);
            detail.setText(String.format(Locale.UK, "%,d words  •  altered %s",
                    info.wordCount, fmt.format(new Date(info.modified))));
            return row;
        }
    }
}
