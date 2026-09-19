package uk.brimstone.simpletype;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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
        root.setPadding(dp(22), dp(20), dp(22), dp(14));
        root.setBackgroundColor(Color.rgb(244, 242, 238));

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Wen Word Processor");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(54, 50, 57));
        title.setTypeface(FontManager.get(this, FontManager.OPEN_DYSLEXIC));
        heading.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button about = new Button(this);
        about.setText("About");
        about.setAllCaps(false);
        about.setTextSize(15);
        about.setContentDescription("About Wen Word Processor");
        about.setOnClickListener(v -> showAbout());
        heading.addView(about);
        root.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("Offline writing");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.rgb(104, 98, 108));
        subtitle.setPadding(0, dp(2), 0, dp(14));
        root.addView(subtitle);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        Button add = new Button(this);
        add.setText("New document");
        add.setAllCaps(false);
        add.setTextSize(16);
        add.setMinHeight(dp(52));
        add.setContentDescription("Create a new document");
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
        totalWords.setTextColor(Color.rgb(92, 87, 96));
        totalWords.setPadding(0, dp(12), 0, dp(10));
        root.addView(totalWords);

        TextView empty = new TextView(this);
        empty.setText("No documents yet\n\nTap New document to start writing.");
        empty.setTextSize(20);
        empty.setTextColor(Color.rgb(112, 106, 116));
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ListView list = new ListView(this);
        adapter = new DocumentAdapter();
        list.setAdapter(adapter);
        list.setEmptyView(empty);
        list.setDividerHeight(dp(8));
        list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Object tag = view.getTag();
            if (tag instanceof DocumentRepository.DocInfo)
                openDocument((DocumentRepository.DocInfo) tag);
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            Object tag = view.getTag();
            if (tag instanceof DocumentRepository.DocInfo)
                confirmDelete((DocumentRepository.DocInfo) tag);
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

    private void showAbout() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(28), dp(22), dp(28), dp(16));

        BrimstoneMarkView mark = new BrimstoneMarkView(this);
        box.addView(mark, new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView app = new TextView(this);
        app.setText("Wen Word Processor");
        app.setTextSize(24);
        app.setTextColor(Color.rgb(54, 50, 57));
        app.setTypeface(FontManager.get(this, FontManager.OPEN_DYSLEXIC));
        app.setGravity(Gravity.CENTER);
        app.setPadding(0, dp(12), 0, dp(4));
        box.addView(app);

        TextView company = new TextView(this);
        company.setText("Brimstone");
        company.setTextSize(20);
        company.setTextColor(Color.rgb(87, 20, 112));
        company.setGravity(Gravity.CENTER);
        box.addView(company);

        String version = "0.1.4";
        try {
            version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}

        TextView details = new TextView(this);
        details.setText("Version " + version + " Alpha\n\n"
                + "Fully offline · no Internet permission\n"
                + "Documents and voice calibration stay on this device.\n\n"
                + "Source: github.com/lurcheous73/wen-word-processor\n\n"
                + "© 2026 Brimstone");
        details.setTextSize(16);
        details.setTextColor(Color.rgb(92, 87, 96));
        details.setGravity(Gravity.CENTER);
        details.setPadding(0, dp(12), 0, 0);
        box.addView(details);

        new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("Close", null)
                .show();
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
        else if (mode == 2) documents.sort(
                Comparator.<DocumentRepository.DocInfo>comparingLong(d -> d.modified)
                        .reversed().thenComparing(alpha));
        else documents.sort(
                Comparator.<DocumentRepository.DocInfo>comparingLong(d -> d.modified)
                        .thenComparing(alpha));
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
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setBackgroundResource(R.drawable.document_row);
                title = new TextView(MainActivity.this);
                title.setTextSize(20);
                title.setTextColor(Color.rgb(48, 45, 51));
                title.setTypeface(FontManager.get(MainActivity.this, FontManager.OPEN_DYSLEXIC));
                detail = new TextView(MainActivity.this);
                detail.setTextSize(14);
                detail.setTextColor(Color.rgb(104, 98, 108));
                row.addView(title);
                row.addView(detail);
            }
            DocumentRepository.DocInfo info = documents.get(position);
            row.setTag(info);
            title.setText(info.title);
            DateFormat fmt = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.UK);
            detail.setText(String.format(Locale.UK, "%,d words  •  altered %s",
                    info.wordCount, fmt.format(new Date(info.modified))));
            return row;
        }
    }
}
