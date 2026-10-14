package com.novel.saya;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private Handler uiHandler;
    private ExecutorService executor;

    private String apiKey = "";
    private String depsicApiKey = "";
    private String grupApiKey = "";
    private String selectedProvider = "Gemini";
    private String selectedModel = "gemini-3.8-flash";
    private List<String> models = new ArrayList<>();
    private List<String> blacklistedModels = new ArrayList<>();
    private boolean useRealtime = true;
    private Button btnKey;
    private TextView lblProvider;
    private boolean useWebSearch = true;
    private boolean autoFallback = true;
    private boolean useMemory = true;
    private boolean antiAiEnabled = true;
    private int antiAiLevel = 100;
    private String selectedStyle = "Netral";
    private String customStyleText = "";
    private String selectedCategory = "Drama";
    private String customCategoryText = "";
    private String writerRoom = "";
    private String characterRoom = "";
    private String worldBuilding = "";
    private String plotOutline = "";
    private String timelineData = "";
    private String magicTechSystem = "";
    private String factionsOrg = "";
    private String lastStory = "";
    private String lastUsedModel = "-";
    private boolean lastIsChapter = false;
    private boolean busy = false;
    private long runId = 0;
    private boolean cancelFlag = false;
    private int tempVal = 95;

    private TextView lblActiveStatus;
    private TextView lblOutputInfo;
    private TextView storyOutput;
    private Button btnSend;
    private Button btnCancel;
    private Button btnTts;
    private Button btnSources;
    private EditText reqInput;
    private CheckBox swRealtime, swWebSearch, swFallback, swMemory;

    private TextToSpeech ttsEngine;
    private boolean ttsReady = false;
    private boolean ttsSpeaking = false;
    private int ttsSession = 0;

    private StoryState storyState;

    private final String[] GAYA_BICARA = {
            "Netral",
            "Laki-laki (Lugas & Tangguh)",
            "Feminin (Vokal Ganda & Sangat Cewek)",
            "Ceplas-ceplos & Tanpa Sensor",
            "Ceplas-ceplos Sering Salah Ngomong & Salah Pengertian (Ada eh, anu, dll)",
            "Gagap / Latah (A-aku, Eh copot-copot)",
            "Sering Nge-gas (Emosional & Nada Tinggi)",
            "Sering Salah Tingkah (Canggung & Gelagapan)",
            "Gaya Suka Menangis (Cengeng & Melankolis)",
            "Puitis, Simbolis & Metaforis Mendalam",
            "Sinis, Sarkastik & Satir Sharp",
            "Berwibawa & Bijaksana (Raja/Tetua)",
            "Anak Kecil (Polos, Lucu & Banyak Tanya)",
            "Remaja Gaul (Bahasa Anak Muda & Santai)",
            "Horor Gelap (Tegang, Mencekam & Seram)",
            "Romantis Manis (Lembut, Mesra & Penuh Perasaan)",
            "Komedi Garing (Humor Kering, Absurd & Konyol)",
            "Custom (Gaya Penulisan Sendiri)"
    };

    private final String[] KATEGORI = {
            "Drama", "Horor", "Pertarungan", "Romantis", "Komedi", "Thriller", "Misteri", "Sci-Fi", "Fantasi", "Slice of Life",
            "Petualangan", "Psikologis", "Action", "Isekai", "Sejarah", "Kriminal", "Detektif", "Cyberpunk", "Steampunk", "Supernatural",
            "Tragedi", "Puisi Cinta", "Fabel", "Legenda", "Kerajaan", "Zombi", "Vampir", "Custom (Kategori Sendiri)"
    };

    private final String[] DEFAULT_MODELS = {
            "gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite", "gemini-3.1-pro-preview", "gemini-flash-latest"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("novel_pro_human_prefs", MODE_PRIVATE);
        uiHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newCachedThreadPool();
        loadPrefs();
        initTts();
        storyState = StoryState.load(this);
        buildMainUI();
    }

    private void loadPrefs() {
        apiKey = prefs.getString("gemini_api_key", "");
        depsicApiKey = prefs.getString("depsic_api_key", "");
        grupApiKey = prefs.getString("grup_api_key", "");
        selectedProvider = prefs.getString("selected_provider", "Gemini");
        selectedModel = prefs.getString("selected_model", "gemini-3.8-flash");
        if (selectedModel != null && (selectedModel.contains("2.5") || selectedModel.contains("2.0") || selectedModel.contains("1.5"))) {
            selectedModel = "gemini-3.8-flash";
        }
        useRealtime = prefs.getBoolean("use_realtime", true);
        useWebSearch = prefs.getBoolean("use_web_search", true);
        autoFallback = prefs.getBoolean("auto_fallback", true);
        useMemory = prefs.getBoolean("use_memory", true);
        antiAiEnabled = prefs.getBoolean("anti_ai_enabled", true);
        antiAiLevel = prefs.getInt("anti_ai_level", 100);
        selectedStyle = prefs.getString("selected_style", "Netral");
        customStyleText = prefs.getString("custom_style_text", "");
        selectedCategory = prefs.getString("selected_category", "Drama");
        customCategoryText = prefs.getString("custom_category_text", "");
        writerRoom = prefs.getString("writer_room", "");
        characterRoom = prefs.getString("character_room", "");
        worldBuilding = prefs.getString("world_building", "");
        plotOutline = prefs.getString("plot_outline", "");
        timelineData = prefs.getString("timeline_data", "");
        magicTechSystem = prefs.getString("magic_tech_system", "");
        factionsOrg = prefs.getString("factions_org", "");
        tempVal = prefs.getInt("temp_val", 95);
        lastStory = prefs.getString("last_story", "");
        lastUsedModel = prefs.getString("last_used_model", "-");

        try {
            JSONArray arr = new JSONArray(prefs.getString("blacklisted_models_json", "[]"));
            blacklistedModels.clear();
            for (int i = 0; i < arr.length(); i++) blacklistedModels.add(arr.getString(i));
        } catch (Exception ignored) {}

        try {
            JSONArray arr = new JSONArray(prefs.getString("models_list_json", "[]"));
            models.clear();
            for (int i = 0; i < arr.length(); i++) models.add(arr.getString(i));
        } catch (Exception ignored) {}
        boolean hasOldOnly = !models.isEmpty();
        if (hasOldOnly) {
            for (String m : models) {
                if (m != null && (m.contains("3.8") || m.contains("3.6") || m.contains("3.5") || m.contains("3.1") || m.contains("flash-latest"))) {
                    hasOldOnly = false;
                    break;
                }
            }
        }
        if (models.isEmpty() || hasOldOnly) {
            models.clear();
            for (String m : DEFAULT_MODELS) models.add(m);
        }
    }

    private void savePrefs() {
        SharedPreferences.Editor e = prefs.edit();
        e.putString("gemini_api_key", apiKey);
        e.putString("depsic_api_key", depsicApiKey);
        e.putString("grup_api_key", grupApiKey);
        e.putString("selected_provider", selectedProvider);
        e.putString("selected_model", selectedModel);
        e.putBoolean("use_realtime", useRealtime);
        e.putBoolean("use_web_search", useWebSearch);
        e.putBoolean("auto_fallback", autoFallback);
        e.putBoolean("use_memory", useMemory);
        e.putBoolean("anti_ai_enabled", antiAiEnabled);
        e.putInt("anti_ai_level", antiAiLevel);
        e.putString("selected_style", selectedStyle);
        e.putString("custom_style_text", customStyleText);
        e.putString("selected_category", selectedCategory);
        e.putString("custom_category_text", customCategoryText);
        e.putString("writer_room", writerRoom);
        e.putString("character_room", characterRoom);
        e.putString("world_building", worldBuilding);
        e.putString("plot_outline", plotOutline);
        e.putString("timeline_data", timelineData);
        e.putString("magic_tech_system", magicTechSystem);
        e.putString("factions_org", factionsOrg);
        e.putInt("temp_val", tempVal);
        e.putString("last_story", lastStory);
        e.putString("last_used_model", lastUsedModel);
        try {
            JSONArray arr = new JSONArray();
            for (String s : blacklistedModels) arr.put(s);
            e.putString("blacklisted_models_json", arr.toString());
        } catch (Exception ignored) {}
        try {
            JSONArray arr = new JSONArray();
            for (String s : models) arr.put(s);
            e.putString("models_list_json", arr.toString());
        } catch (Exception ignored) {}
        e.apply();
    }

    private void buildMainUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(15), dp(15), dp(15));

        TextView title = tv("Produksi Novel Pro Raksasa", 24, true, Color.parseColor("#4CAF50"));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        lblProvider = tv("Provider aktif: " + selectedProvider, 14, true, Color.parseColor("#FF5722"));
        lblProvider.setGravity(Gravity.CENTER);
        root.addView(lblProvider, lp(8));

        Button btnProvider = btn("PILIH PROVIDER (Gemini / Depsic / Grup)", Color.parseColor("#E65100"));
        btnProvider.setOnClickListener(v -> showProviderDialog());
        root.addView(btnProvider, lp(5));

        btnKey = btn(apiKeyButtonLabel(), Color.parseColor("#FF9800"));
        btnKey.setOnClickListener(v -> showApiKeyDialog());
        root.addView(btnKey, lp(10));

        Button btnModel = btn("PILIH MODEL ENGINE & BLACKLIST", Color.parseColor("#607D8B"));
        btnModel.setOnClickListener(v -> showModelDialog());
        root.addView(btnModel, lp(5));

        Button btnWriter = btn("RUANG KEPENULISAN (NAMA PENULIS)", Color.parseColor("#FF5722"));
        btnWriter.setOnClickListener(v -> showWriterRoom());
        root.addView(btnWriter, lp(10));

        Button btnChar = btn("RUANG KARAKTER (NAMA TOKOH)", Color.parseColor("#9C27B0"));
        btnChar.setOnClickListener(v -> showCharacterRoom());
        root.addView(btnChar, lp(5));

        Button btnWb = btn("WORLDBUILDING & FAKSI & SIHIR", Color.parseColor("#3F51B5"));
        btnWb.setOnClickListener(v -> showWorldbuildingDialog());
        root.addView(btnWb, lp(5));

        Button btnPlot = btn("PAPAN PLOT, BAB & TIMELINE", Color.parseColor("#009688"));
        btnPlot.setOnClickListener(v -> showPlotTimelineDialog());
        root.addView(btnPlot, lp(5));

        Button btnAnti = btn("DETEKTOR / ANTI DETEKTOR AI", Color.parseColor("#E91E63"));
        btnAnti.setOnClickListener(v -> showAntiAiDialog());
        root.addView(btnAnti, lp(5));

        Button btnStyle = btn("PILIH GAYA BICARA / PENULISAN", Color.parseColor("#00BCD4"));
        btnStyle.setOnClickListener(v -> showStyleDialog());
        root.addView(btnStyle, lp(5));

        Button btnCat = btn("PILIH / CUSTOM KATEGORI", Color.parseColor("#3F51B5"));
        btnCat.setOnClickListener(v -> showCategoryDialog());
        root.addView(btnCat, lp(5));

        TextView lblFitur = tv("Pengaturan & Fitur Tambahan:", 16, true, Color.BLACK);
        root.addView(lblFitur, lp(15));

        swRealtime = new CheckBox(this);
        swRealtime.setText("Real-timitas (Gunakan Tanggal & Waktu Nyata)");
        swRealtime.setChecked(useRealtime);
        swRealtime.setOnCheckedChangeListener((b, c) -> { useRealtime = c; savePrefs(); });
        root.addView(swRealtime, lp(5));

        swWebSearch = new CheckBox(this);
        swWebSearch.setText("Akses Browsing Web Realtime (Google Search)");
        swWebSearch.setChecked(useWebSearch);
        swWebSearch.setOnCheckedChangeListener((b, c) -> {
            useWebSearch = c;
            savePrefs();
            if (btnSources != null) btnSources.setVisibility(c ? View.VISIBLE : View.GONE);
        });
        root.addView(swWebSearch, lp(5));

        swFallback = new CheckBox(this);
        swFallback.setText("Alih Model Otomatis Saat Error");
        swFallback.setChecked(autoFallback);
        swFallback.setOnCheckedChangeListener((b, c) -> { autoFallback = c; savePrefs(); });
        root.addView(swFallback, lp(5));

        swMemory = new CheckBox(this);
        swMemory.setText("Aktifkan Memori Bersambung");
        swMemory.setChecked(useMemory);
        swMemory.setOnCheckedChangeListener((b, c) -> {
            useMemory = c;
            savePrefs();
            if (!busy) lblActiveStatus.setText(readyText());
        });
        root.addView(swMemory, lp(5));

        Button btnReset = btn("RESET MEMORI CERITA", Color.parseColor("#F44336"));
        btnReset.setOnClickListener(v -> confirmResetMemory());
        root.addView(btnReset, lp(5));

        reqInput = new EditText(this);
        reqInput.setHint("Ketik judul, ide, skrip, atau tema cerita raksasa...");
        reqInput.setMinLines(4);
        reqInput.setGravity(Gravity.TOP);
        root.addView(reqInput, lp(15));

        btnSend = btn("KIRIM REQUEST CERITA RAKSASA", Color.parseColor("#4CAF50"));
        btnSend.setTextSize(16);
        btnSend.setOnClickListener(v -> generateStory(reqInput.getText().toString().trim()));
        root.addView(btnSend, lp(10));

        btnCancel = btn("BATALKAN PROSES PEMBUATAN", Color.parseColor("#F44336"));
        btnCancel.setTextSize(16);
        btnCancel.setVisibility(View.GONE);
        btnCancel.setOnClickListener(v -> cancelProcess());
        root.addView(btnCancel, lp(10));

        lblActiveStatus = tv(readyText(), 14, true, Color.parseColor("#2196F3"));
        root.addView(lblActiveStatus, lp(10));

        lblOutputInfo = tv("HASIL DOKUMEN CERITA RAKSASA (Model: " + lastUsedModel + "):", 16, true, Color.BLACK);
        root.addView(lblOutputInfo, lp(15));

        storyOutput = new TextView(this);
        storyOutput.setText(cap(lastStory));
        storyOutput.setPadding(dp(15), dp(15), dp(15), dp(15));
        storyOutput.setBackgroundColor(Color.parseColor("#EEEEEE"));
        storyOutput.setTextIsSelectable(true);
        storyOutput.setTextSize(16);
        storyOutput.setTextColor(Color.BLACK);
        root.addView(storyOutput);

        btnSources = btn("TAMPILKAN LINK SUMBER", Color.parseColor("#009688"));
        btnSources.setVisibility(useWebSearch ? View.VISIBLE : View.GONE);
        btnSources.setOnClickListener(v -> showSourcesDialog());
        root.addView(btnSources, lp(10));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(2);
        Button btnCopy = btn("SALIN CERITA", Color.parseColor("#607D8B"));
        btnCopy.setOnClickListener(v -> copyStory());
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p1.setMargins(0, 0, dp(5), 0);
        row1.addView(btnCopy, p1);
        Button btnShare = btn("BAGIKAN CERITA", Color.parseColor("#607D8B"));
        btnShare.setOnClickListener(v -> shareStory());
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p2.setMargins(dp(5), 0, 0, 0);
        row1.addView(btnShare, p2);
        root.addView(row1, lp(10));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setWeightSum(2);
        btnTts = btn(ttsSpeaking ? "HENTIKAN TTS" : "BACA KERAS (TTS)", Color.parseColor("#8BC34A"));
        btnTts.setOnClickListener(v -> toggleTts());
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p3.setMargins(0, 0, dp(5), 0);
        row2.addView(btnTts, p3);
        Button btnExport = btn("OPSI EKSPOR DOKUMEN", Color.parseColor("#673AB7"));
        btnExport.setOnClickListener(v -> showExportDialog());
        LinearLayout.LayoutParams p4 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p4.setMargins(dp(5), 0, 0, 0);
        row2.addView(btnExport, p4);
        root.addView(row2, lp(5));

        Button btnDelete = new Button(this);
        btnDelete.setText("HAPUS HASIL CERITA");
        btnDelete.setTextColor(Color.parseColor("#FF4444"));
        btnDelete.setOnClickListener(v -> deleteResult());
        root.addView(btnDelete, lp(5));

        LinearLayout exitRow = new LinearLayout(this);
        exitRow.setGravity(Gravity.RIGHT);
        Button btnExit = btn("KELUAR", Color.parseColor("#212121"));
        btnExit.setOnClickListener(v -> finish());
        exitRow.addView(btnExit);
        root.addView(exitRow, lp(20));

        scroll.addView(root);
        setContentView(scroll);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private TextView tv(String t, int sizeSp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(color);
        return v;
    }

    private Button btn(String t, int bg) {
        Button b = new Button(this);
        b.setText(t);
        b.setBackgroundColor(bg);
        b.setTextColor(Color.WHITE);
        return b;
    }

    private LinearLayout.LayoutParams lp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    private void toast(String s) {
        runOnUiThread(() -> Toast.makeText(this, s, s.length() > 70 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }

    private String readyText() {
        if (useMemory && storyState != null && storyState.chapters.size() > 0) {
            String ttl = storyState.title == null || storyState.title.isEmpty() ? "novel" : storyState.title;
            return "Status: Siap. Melanjutkan " + ttl + ", bab berikutnya: Bab " + (storyState.chapters.size() + 1);
        }
        return "Status: Siap";
    }

    private String cap(String t) {
        if (t == null) t = "";
        if (t.length() > 300000) {
            return t.substring(0, 300000) + "\n\n[Tampilan dipotong supaya HP tidak berat. Teks lengkap tetap tersimpan dan bisa disalin, dibagikan, diekspor, atau dibacakan lewat tombol di bawah.]";
        }
        return t;
    }

    private void setBusy(boolean b) {
        busy = b;
        runOnUiThread(() -> {
            btnSend.setText(b ? "SEDANG MEMPROSES RAKSASA..." : "KIRIM REQUEST CERITA RAKSASA");
            btnCancel.setVisibility(b ? View.VISIBLE : View.GONE);
        });
    }

    private void status(String t) {
        runOnUiThread(() -> lblActiveStatus.setText("Status: " + t));
    }

    private void showResult(String text, String label) {
        lastStory = text;
        runOnUiThread(() -> {
            storyOutput.setText(cap(text));
            lblOutputInfo.setText(label);
        });
        savePrefs();
    }

    private String getCurrentDateIndo() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        return sdf.format(new Date());
    }

    private boolean isModelBlacklisted(String name) {
        if (name == null) return false;
        String clean = name.replace("models/", "");
        for (String b : blacklistedModels) {
            if (b.replace("models/", "").equals(clean)) return true;
        }
        return false;
    }

    private String apiKeyButtonLabel() {
        if ("Depsic".equals(selectedProvider)) return "SET API KEY DEPSIC";
        if ("Grup".equals(selectedProvider)) return "SET API KEY GRUP";
        return "SET API KEY GEMINI (BARU)";
    }

    private String currentApiKey() {
        if ("Depsic".equals(selectedProvider)) return depsicApiKey;
        if ("Grup".equals(selectedProvider)) return grupApiKey;
        return apiKey;
    }

    private void showProviderDialog() {
        final String[] providers = {"Gemini", "Depsic", "Grup"};
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("Pilih Provider AI", 18, true, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        TextView info = tv("Gemini = Google AI Studio (kunci AIza / AQ.)\nDepsic = DeepSeek\nGrup = Groq (bukan Grok/xAI)", 13, false, Color.DKGRAY);
        lay.addView(info, lp(8));
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providers);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < providers.length; i++) {
            if (providers[i].equals(selectedProvider)) { sp.setSelection(i); break; }
        }
        lay.addView(sp, lp(10));
        Button save = btn("SIMPAN PROVIDER", Color.parseColor("#E65100"));
        lay.addView(save, lp(15));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            selectedProvider = providers[sp.getSelectedItemPosition()];
            if ("Depsic".equals(selectedProvider)) {
                selectedModel = "deepseek-chat";
                models.clear();
                models.add("deepseek-chat");
                models.add("deepseek-reasoner");
            } else if ("Grup".equals(selectedProvider)) {
                selectedModel = "llama-3.3-70b-versatile";
                models.clear();
                models.add("llama-3.3-70b-versatile");
                models.add("llama-3.1-8b-instant");
                models.add("mixtral-8x7b-32768");
                models.add("gemma2-9b-it");
            } else {
                selectedModel = "gemini-3.8-flash";
                models.clear();
                for (String m : DEFAULT_MODELS) models.add(m);
            }
            savePrefs();
            if (lblProvider != null) lblProvider.setText("Provider aktif: " + selectedProvider);
            if (btnKey != null) btnKey.setText(apiKeyButtonLabel());
            toast("Provider: " + selectedProvider + " | Model: " + selectedModel);
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showApiKeyDialog() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        String titleText;
        String hintText;
        String current;
        if ("Depsic".equals(selectedProvider)) {
            titleText = "Masukkan API Key Depsic (DeepSeek)";
            hintText = "sk-...";
            current = depsicApiKey;
        } else if ("Grup".equals(selectedProvider)) {
            titleText = "Masukkan API Key Grup (Groq)";
            hintText = "gsk_...";
            current = grupApiKey;
        } else {
            titleText = "Masukkan API Key Gemini (Google AI Studio)";
            hintText = "AIzaSy... atau AQ.Ab8...";
            current = apiKey;
        }
        TextView t = tv(titleText, 16, true, Color.BLACK);
        lay.addView(t);
        if ("Gemini".equals(selectedProvider)) {
            TextView note = tv("Model default: gemini-3.8-flash.\nKunci AIza/AQ. dikirim via ?key= di URL (cara yang sama dengan ekstensi yang berhasil).", 13, false, Color.parseColor("#D32F2F"));
            lay.addView(note, lp(6));
        }
        EditText edit = new EditText(this);
        edit.setText(current);
        edit.setHint(hintText);
        lay.addView(edit);
        Button save = btn("SIMPAN PERMANEN", Color.parseColor("#2196F3"));
        lay.addView(save, lp(10));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            String val = edit.getText().toString().replaceAll("\\s+", "");
            if ("Depsic".equals(selectedProvider)) depsicApiKey = val;
            else if ("Grup".equals(selectedProvider)) grupApiKey = val;
            else apiKey = val;
            savePrefs();
            toast("API Key " + selectedProvider + " tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showAntiAiDialog() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("Pengaturan Anti-Detektor AI", 18, true, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        CheckBox sw = new CheckBox(this);
        sw.setText("Aktifkan Anti-Detektor AI");
        sw.setChecked(antiAiEnabled);
        lay.addView(sw, lp(10));
        TextView lbl = tv("Tingkat Anti-AI: " + antiAiLevel + "%", 16, false, Color.parseColor("#4CAF50"));
        lbl.setGravity(Gravity.CENTER);
        lay.addView(lbl, lp(15));
        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(antiAiLevel);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lbl.setText("Tingkat Anti-AI: " + progress + "%");
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        lay.addView(sb, lp(10));
        Button save = btn("SIMPAN PENGATURAN", Color.parseColor("#2196F3"));
        lay.addView(save, lp(20));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            antiAiEnabled = sw.isChecked();
            antiAiLevel = sb.getProgress();
            savePrefs();
            toast("Pengaturan Anti-AI Tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showStyleDialog() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("Pilih Gaya Bicara & Penulisan", 18, true, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GAYA_BICARA);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < GAYA_BICARA.length; i++) {
            if (GAYA_BICARA[i].equals(selectedStyle)) { sp.setSelection(i); break; }
        }
        lay.addView(sp, lp(10));
        EditText edit = new EditText(this);
        edit.setHint("Ketik gaya penulisan custom jika memilih Custom...");
        edit.setText(customStyleText);
        edit.setMinLines(3);
        edit.setVisibility(selectedStyle.equals("Custom (Gaya Penulisan Sendiri)") ? View.VISIBLE : View.GONE);
        lay.addView(edit, lp(10));
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                edit.setVisibility(GAYA_BICARA[pos].equals("Custom (Gaya Penulisan Sendiri)") ? View.VISIBLE : View.GONE);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        Button save = btn("SIMPAN PERMANEN", Color.parseColor("#4CAF50"));
        lay.addView(save, lp(15));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            selectedStyle = GAYA_BICARA[sp.getSelectedItemPosition()];
            customStyleText = edit.getText().toString();
            savePrefs();
            toast("Gaya penulisan tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showCategoryDialog() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("Pilih / Custom Kategori Novel", 18, true, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, KATEGORI);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < KATEGORI.length; i++) {
            if (KATEGORI[i].equals(selectedCategory)) { sp.setSelection(i); break; }
        }
        lay.addView(sp, lp(10));
        EditText edit = new EditText(this);
        edit.setHint("Ketik nama kategori custom...");
        edit.setText(customCategoryText);
        edit.setVisibility(selectedCategory.equals("Custom (Kategori Sendiri)") ? View.VISIBLE : View.GONE);
        lay.addView(edit, lp(10));
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                edit.setVisibility(KATEGORI[pos].equals("Custom (Kategori Sendiri)") ? View.VISIBLE : View.GONE);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        Button save = btn("SIMPAN KATEGORI", Color.parseColor("#2196F3"));
        lay.addView(save, lp(15));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            selectedCategory = KATEGORI[sp.getSelectedItemPosition()];
            customCategoryText = edit.getText().toString();
            savePrefs();
            toast("Kategori tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showWriterRoom() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("RUANG KEPENULISAN (NAMA PENULIS)", 18, true, Color.parseColor("#FF5722"));
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        TextView hint = tv("Masukkan daftar nama penulis (1 nama per baris):", 14, false, Color.BLACK);
        lay.addView(hint, lp(5));
        EditText edit = new EditText(this);
        edit.setText(writerRoom);
        edit.setHint("Contoh:\nAndi Baskoro\nSiti Aminah");
        edit.setMinLines(6);
        edit.setGravity(Gravity.TOP);
        lay.addView(edit, lp(10));
        Button save = btn("SIMPAN DAFTAR PENULIS", Color.parseColor("#FF5722"));
        lay.addView(save, lp(15));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            writerRoom = edit.getText().toString();
            savePrefs();
            toast("Daftar Penulis Tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showCharacterRoom() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("RUANG KARAKTER TOKOH", 18, true, Color.parseColor("#9C27B0"));
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        TextView hint = tv("Pasang daftar nama, sifat, dan peran karakter di bawah ini agar tersimpan permanen:", 14, false, Color.BLACK);
        lay.addView(hint, lp(5));
        EditText edit = new EditText(this);
        edit.setText(characterRoom);
        edit.setHint("Contoh:\n1. Syarif (Protagonis) - Pria tenang, suka musik.\n2. Maryam - Sahabat, ramah.");
        edit.setMinLines(6);
        edit.setGravity(Gravity.TOP);
        lay.addView(edit, lp(10));
        Button save = btn("TUTUP RUANGAN & SIMPAN", Color.parseColor("#9C27B0"));
        lay.addView(save, lp(15));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        save.setOnClickListener(v -> {
            characterRoom = edit.getText().toString();
            savePrefs();
            toast("Ruang Karakter Tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showWorldbuildingDialog() {
        ScrollView sc = new ScrollView(this);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("WORLDBUILDING & RISET DUNIA", 18, true, Color.parseColor("#3F51B5"));
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        lay.addView(tv("Aturan Geografi, Budaya & Latar Belakang Dunia:", 14, false, Color.BLACK), lp(10));
        EditText editWb = new EditText(this);
        editWb.setText(worldBuilding);
        editWb.setHint("Ketik detail geografi, sejarah, aturan sosial...");
        editWb.setMinLines(4);
        editWb.setGravity(Gravity.TOP);
        lay.addView(editWb);
        lay.addView(tv("Sistem Sihir / Teknologi / Hukum Fisika:", 14, false, Color.BLACK), lp(10));
        EditText editMagic = new EditText(this);
        editMagic.setText(magicTechSystem);
        editMagic.setHint("Penjelasan sistem kekuatan, energi...");
        editMagic.setMinLines(4);
        editMagic.setGravity(Gravity.TOP);
        lay.addView(editMagic);
        lay.addView(tv("Faksi / Organisasi / Kelompok Politik:", 14, false, Color.BLACK), lp(10));
        EditText editFac = new EditText(this);
        editFac.setText(factionsOrg);
        editFac.setHint("Nama faksi, hirarki, sekte, aliansi...");
        editFac.setMinLines(4);
        editFac.setGravity(Gravity.TOP);
        lay.addView(editFac);
        Button save = btn("SIMPAN SEMUA DATA DUNIA", Color.parseColor("#3F51B5"));
        lay.addView(save, lp(15));
        sc.addView(lay);
        AlertDialog dlg = new AlertDialog.Builder(this).setView(sc).create();
        save.setOnClickListener(v -> {
            worldBuilding = editWb.getText().toString();
            magicTechSystem = editMagic.getText().toString();
            factionsOrg = editFac.getText().toString();
            savePrefs();
            toast("Data Pembangunan Dunia Tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showPlotTimelineDialog() {
        ScrollView sc = new ScrollView(this);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("PAPAN PLOT, BAB & TIMELINE", 18, true, Color.parseColor("#009688"));
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        lay.addView(tv("Garis Besar Plot / Outlining Bab:", 14, false, Color.BLACK), lp(10));
        EditText editPlot = new EditText(this);
        editPlot.setText(plotOutline);
        editPlot.setHint("Bab 1: Perkenalan...\nBab 2: Konflik Utama...");
        editPlot.setMinLines(5);
        editPlot.setGravity(Gravity.TOP);
        lay.addView(editPlot);
        lay.addView(tv("Garis Waktu Kronologis (Timeline):", 14, false, Color.BLACK), lp(10));
        EditText editTl = new EditText(this);
        editTl.setText(timelineData);
        editTl.setHint("Tahun 1000: Perang Dimulai...");
        editTl.setMinLines(5);
        editTl.setGravity(Gravity.TOP);
        lay.addView(editTl);
        Button save = btn("SIMPAN PLOT & TIMELINE", Color.parseColor("#009688"));
        lay.addView(save, lp(15));
        sc.addView(lay);
        AlertDialog dlg = new AlertDialog.Builder(this).setView(sc).create();
        save.setOnClickListener(v -> {
            plotOutline = editPlot.getText().toString();
            timelineData = editTl.getText().toString();
            savePrefs();
            toast("Plot & Timeline Tersimpan!");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showModelDialog() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("Pengaturan Model Engine", 18, true, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        TextView lbl = tv("Model Utama: " + selectedModel, 14, true, Color.parseColor("#2196F3"));
        lay.addView(lbl, lp(10));
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, models);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).equals(selectedModel)) { sp.setSelection(i); break; }
        }
        lay.addView(sp, lp(10));
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (pos >= 0 && pos < models.size()) lbl.setText("Model Utama: " + models.get(pos));
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        Button btnRefresh = btn("SEGARKAN MODEL DARI SERVER", Color.parseColor("#009688"));
        lay.addView(btnRefresh, lp(15));
        Button btnBlack = btn("KELOLA BLACKLIST MODEL", Color.parseColor("#D32F2F"));
        lay.addView(btnBlack, lp(5));
        Button btnSave = btn("SIMPAN PILIHAN PERMANEN", Color.parseColor("#4CAF50"));
        lay.addView(btnSave, lp(10));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        btnRefresh.setOnClickListener(v -> fetchModels(() -> {
            ad.clear();
            ad.addAll(models);
            ad.notifyDataSetChanged();
        }));
        btnBlack.setOnClickListener(v -> showBlacklistDialog());
        btnSave.setOnClickListener(v -> {
            int idx = sp.getSelectedItemPosition();
            if (idx >= 0 && idx < models.size()) {
                selectedModel = models.get(idx);
                savePrefs();
                toast("Model Utama: " + selectedModel + " disimpan.");
                dlg.dismiss();
            }
        });
        dlg.show();
    }

    private void showBlacklistDialog() {
        if (models.isEmpty()) {
            toast("Daftar model kosong. Segarkan model terlebih dahulu!");
            return;
        }
        ScrollView sc = new ScrollView(this);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("KELOLA BLACKLIST MODEL", 18, true, Color.parseColor("#D32F2F"));
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        TextView hint = tv("Centang model yang ingin kamu DIBLOKIR/DIABAIKAN dari pembuatan cerita maupun alih otomatis:", 14, false, Color.BLACK);
        lay.addView(hint, lp(5));
        List<CheckBox> checks = new ArrayList<>();
        for (String m : models) {
            CheckBox cb = new CheckBox(this);
            cb.setText(m);
            cb.setChecked(isModelBlacklisted(m));
            cb.setTextColor(Color.BLACK);
            lay.addView(cb);
            checks.add(cb);
        }
        Button save = btn("SIMPAN PERUBAHAN BLACKLIST", Color.parseColor("#D32F2F"));
        lay.addView(save, lp(30));
        sc.addView(lay);
        AlertDialog dlg = new AlertDialog.Builder(this).setView(sc).create();
        save.setOnClickListener(v -> {
            blacklistedModels.clear();
            for (int i = 0; i < checks.size(); i++) {
                if (checks.get(i).isChecked()) blacklistedModels.add(models.get(i));
            }
            savePrefs();
            toast("Daftar Blacklist Diperbarui (" + blacklistedModels.size() + " model diblokir)");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showSourcesDialog() {
        String info = "Belum ada sumber. Sumber muncul setelah Gemini berhasil mencari di web saat membuat cerita. Pastikan pencarian web dicentang dan kuota API masih ada.";
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("SUMBER FAKTA DARI PENCARIAN WEB", 18, true, Color.parseColor("#009688"));
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        lay.addView(tv(info, 14, false, Color.BLACK), lp(8));
        Button close = btn("TUTUP", Color.parseColor("#212121"));
        lay.addView(close, lp(14));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        close.setOnClickListener(v -> dlg.dismiss());
        dlg.show();
    }

    private void showExportDialog() {
        boolean whole = useMemory && storyState != null && storyState.chapters.size() > 0 && lastIsChapter;
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = tv("PILIH FORMAT EKSPOR DOKUMEN", 18, true, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        lay.addView(title);
        String info = whole ? "Isi ekspor: seluruh novel (" + storyState.chapters.size() + " bab)" : "Isi ekspor: hasil cerita terakhir";
        lay.addView(tv(info, 14, false, Color.BLACK), lp(5));
        CheckBox chk = new CheckBox(this);
        chk.setText("Hanya bab terakhir (bukan seluruh novel)");
        chk.setVisibility(whole && storyState.chapters.size() > 1 ? View.VISIBLE : View.GONE);
        lay.addView(chk, lp(5));
        Button btnTxt = btn("EKSPOR FORMAT TXT", Color.parseColor("#795548"));
        lay.addView(btnTxt, lp(15));
        Button btnPdf = btn("EKSPOR FORMAT PDF", Color.parseColor("#D32F2F"));
        lay.addView(btnPdf, lp(10));
        AlertDialog dlg = new AlertDialog.Builder(this).setView(lay).create();
        btnTxt.setOnClickListener(v -> {
            String text = exportSource(chk.isChecked());
            saveAsTxt(text, false);
            dlg.dismiss();
        });
        btnPdf.setOnClickListener(v -> {
            String text = exportSource(chk.isChecked());
            saveAsPdf(text);
            dlg.dismiss();
        });
        dlg.show();
    }

    private String exportSource(boolean onlyLast) {
        if (!onlyLast && useMemory && storyState != null && storyState.chapters.size() > 0 && lastIsChapter) {
            String full = storyState.fullText(this);
            if (full != null && !full.isEmpty()) return full;
        }
        return lastStory == null ? "" : lastStory;
    }

    private void confirmResetMemory() {
        if (busy) {
            toast("Tunggu proses selesai atau batalkan dulu sebelum reset.");
            return;
        }
        if (storyState != null && storyState.chapters.size() > 0) {
            String ttl = storyState.title == null || storyState.title.isEmpty() ? "novel ini" : storyState.title;
            new AlertDialog.Builder(this)
                    .setTitle("RESET MEMORI CERITA")
                    .setMessage("Memori \"" + ttl + "\" (" + storyState.chapters.size() + " bab) akan dikosongkan dan cerita berikutnya mulai dari Bab 1. Naskah lama dicadangkan otomatis ke berkas TXT.")
                    .setPositiveButton("YA, RESET MEMORI", (d, w) -> doReset())
                    .setNegativeButton("BATAL", null)
                    .show();
        } else {
            doReset();
        }
    }

    private void doReset() {
        String backup = storyState != null ? storyState.reset(this) : null;
        storyState = null;
        if (!busy) lblActiveStatus.setText(readyText());
        if (backup != null) toast("Memori direset. Naskah lama dicadangkan di: " + backup);
        else toast("Memori berhasil direset!");
    }

    private void copyStory() {
        if (lastStory == null || lastStory.isEmpty()) {
            toast("Teks kosong!");
            return;
        }
        String content = lastStory;
        if (content.length() > 500000) {
            content = content.substring(0, 500000);
            toast("Teks sangat panjang, hanya 500.000 karakter pertama yang disalin.");
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("NovelProStory", content));
        toast("Tersalin ke Papan Klip!");
    }

    private void shareStory() {
        if (lastStory == null || lastStory.isEmpty()) {
            toast("Teks kosong!");
            return;
        }
        if (lastStory.length() > 400000) {
            saveAsTxt(lastStory, false);
            toast("Teks terlalu besar untuk dibagikan langsung, jadi disimpan sebagai berkas TXT.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, lastStory);
        startActivity(Intent.createChooser(intent, "Bagikan Novel Ke..."));
    }

    private void deleteResult() {
        boolean canUndo = useMemory && storyState != null && storyState.chapters.size() > 0
                && lastIsChapter && lastStory != null && !lastStory.isEmpty() && !busy;
        if (canUndo) {
            int n = storyState.chapters.size();
            new AlertDialog.Builder(this)
                    .setTitle("HAPUS HASIL CERITA")
                    .setMessage("Bab " + n + " tersimpan di memori cerita. Mau diapakan?")
                    .setPositiveButton("HAPUS DARI LAYAR SAJA", (d, w) -> clearScreen())
                    .setNeutralButton("BATALKAN BAB " + n + " DAN TULIS ULANG", (d, w) -> {
                        if (storyState.undoLast(this)) {
                            clearScreen();
                            lblActiveStatus.setText(readyText());
                            toast("Bab " + n + " dibatalkan. Memori dikembalikan seperti sebelum bab itu.");
                        }
                    })
                    .setNegativeButton("BATAL", null)
                    .show();
        } else {
            clearScreen();
        }
    }

    private void clearScreen() {
        storyOutput.setText("");
        lastStory = "";
        savePrefs();
        toast("Hasil cerita dihapus dari layar.");
    }

    private void cancelProcess() {
        cancelFlag = true;
        runId++;
        setBusy(false);
        status("Dibatalkan");
        toast("Proses pembuatan cerita dibatalkan.");
    }

    private void initTts() {
        ttsEngine = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = ttsEngine.setLanguage(new Locale("id", "ID"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsEngine.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
            }
        });
    }

    private void toggleTts() {
        if (ttsSpeaking) {
            ttsSession++;
            if (ttsEngine != null) ttsEngine.stop();
            ttsSpeaking = false;
            btnTts.setText("BACA KERAS (TTS)");
            toast("Pembacaan dihentikan.");
            return;
        }
        if (lastStory == null || lastStory.isEmpty()) {
            toast("Teks kosong!");
            return;
        }
        if (!ttsReady || ttsEngine == null) {
            toast("Mesin TTS gagal dimulai. Cek mesin suara di Setelan Android.");
            return;
        }
        ttsSession++;
        ttsSpeaking = true;
        btnTts.setText("HENTIKAN TTS");
        String cleaned = lastStory.replaceAll("(?m)^\\s*$", "").trim();
        int max = 3500;
        try {
            int m = TextToSpeech.getMaxSpeechInputLength();
            if (m > 600) max = Math.min(3500, m - 300);
        } catch (Exception ignored) {}
        List<String> chunks = splitText(cleaned, max);
        for (int i = 0; i < chunks.size(); i++) {
            int mode = i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
            ttsEngine.speak(chunks.get(i), mode, null, "np_" + ttsSession + "_" + i);
        }
        toast("Mulai membacakan " + chunks.size() + " bagian. Tekan tombol lagi untuk berhenti.");
        watchTts(ttsSession);
    }

    private void watchTts(int session) {
        uiHandler.postDelayed(() -> {
            if (session != ttsSession || !ttsSpeaking) return;
            if (ttsEngine != null && !ttsEngine.isSpeaking()) {
                ttsSpeaking = false;
                btnTts.setText("BACA KERAS (TTS)");
                toast("Selesai membacakan.");
            } else {
                watchTts(session);
            }
        }, 900);
    }

    private List<String> splitText(String text, int maxlen) {
        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (cur.length() + line.length() + 1 <= maxlen) {
                if (cur.length() > 0) cur.append("\n");
                cur.append(line);
            } else {
                if (cur.length() > 0) {
                    chunks.add(cur.toString());
                    cur = new StringBuilder();
                }
                if (line.length() > maxlen) {
                    int start = 0;
                    while (start < line.length()) {
                        int end = Math.min(start + maxlen, line.length());
                        chunks.add(line.substring(start, end));
                        start = end;
                    }
                } else {
                    cur.append(line);
                }
            }
        }
        if (cur.length() > 0) chunks.add(cur.toString());
        return chunks;
    }

    private File pickDir() {
        File[] cands = {
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ProduksiNovelPro"),
                new File(getExternalFilesDir(null), "ProduksiNovelPro"),
                new File(getFilesDir(), "ProduksiNovelPro")
        };
        for (File d : cands) {
            try {
                if (!d.exists()) d.mkdirs();
                if (d.exists() && d.isDirectory()) {
                    File probe = new File(d, ".probe_" + System.currentTimeMillis());
                    FileOutputStream fos = new FileOutputStream(probe);
                    fos.write('x');
                    fos.close();
                    probe.delete();
                    return d;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void saveAsTxt(String content, boolean isAuto) {
        if (content == null || content.isEmpty()) {
            toast("Tidak ada teks untuk disimpan!");
            return;
        }
        File dir = pickDir();
        if (dir == null) {
            toast("Tidak ada folder yang bisa ditulis.");
            return;
        }
        String prefix = isAuto ? "Novel_Raksasa_Auto_" : "Novel_Manual_";
        String filename = prefix + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";
        File f = new File(dir, filename);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.close();
            toast("TXT tersimpan di: " + f.getAbsolutePath().replace("/storage/emulated/0/", "").replace("/sdcard/", ""));
        } catch (Exception e) {
            toast("Gagal menyimpan TXT: " + e.getMessage());
        }
    }

    private void saveAsPdf(String content) {
        if (content == null || content.isEmpty()) {
            toast("Tidak ada teks untuk diubah ke PDF!");
            return;
        }
        File dir = pickDir();
        if (dir == null) {
            toast("Tidak ada folder yang bisa ditulis. Cek izin penyimpanan.");
            return;
        }
        toast("Memproses dokumen PDF...");
        executor.execute(() -> {
            try {
                PdfDocument doc = new PdfDocument();
                TextPaint paint = new TextPaint();
                paint.setTextSize(14);
                paint.setAntiAlias(true);
                int pageWidth = 595, pageHeight = 842, margin = 50;
                int textWidth = pageWidth - margin * 2;
                int avail = pageHeight - margin * 2;
                StaticLayout sl = StaticLayout.Builder.obtain(content, 0, content.length(), paint, textWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0, 1.2f)
                        .setIncludePad(false)
                        .build();
                int totalLines = sl.getLineCount();
                int pageNum = 1, startLine = 0;
                while (startLine < totalLines) {
                    int topStart = sl.getLineTop(startLine);
                    int endLine = startLine;
                    while (endLine < totalLines && (sl.getLineBottom(endLine) - topStart) <= avail) endLine++;
                    if (endLine == startLine) endLine = startLine + 1;
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                    PdfDocument.Page page = doc.startPage(pageInfo);
                    page.getCanvas().save();
                    page.getCanvas().translate(margin, margin - topStart);
                    page.getCanvas().clipRect(0, topStart, textWidth, sl.getLineBottom(endLine - 1));
                    sl.draw(page.getCanvas());
                    page.getCanvas().restore();
                    doc.finishPage(page);
                    startLine = endLine;
                    pageNum++;
                }
                File file = new File(dir, "Novel_Raksasa_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".pdf");
                FileOutputStream fos = new FileOutputStream(file);
                doc.writeTo(fos);
                doc.close();
                fos.close();
                toast("PDF berhasil disimpan di: " + file.getAbsolutePath().replace("/storage/emulated/0/", "").replace("/sdcard/", ""));
            } catch (Exception e) {
                toast("Gagal membuat PDF: " + e.getMessage());
            }
        });
    }

    private void fetchModels(Runnable onDone) {
        if (!"Gemini".equals(selectedProvider)) {
            toast("Segarkan model hanya untuk provider Gemini. Depsic/Grup pakai daftar model bawaan.");
            return;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            toast("Isi API Key Gemini baru terlebih dahulu!");
            return;
        }
        toast("Menghubungi server Google AI Studio (kunci baru)...");
        executor.execute(() -> {
            try {
                List<String> names = new ArrayList<>();
                String next = null;
                int pages = 0;
                do {
                    String urlStr = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000&key=" + java.net.URLEncoder.encode(apiKey, "UTF-8");
                    if (next != null) urlStr += "&pageToken=" + java.net.URLEncoder.encode(next, "UTF-8");
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("x-goog-api-key", apiKey);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);
                    int code = conn.getResponseCode();
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    if (code != 200) {
                        toast("Gagal menyegarkan model. Kode Error: " + code);
                        return;
                    }
                    JSONObject data = new JSONObject(sb.toString());
                    JSONArray arr = data.optJSONArray("models");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.getJSONObject(i);
                            String name = item.optString("name", "").replace("models/", "");
                            if (isTextModel(name, item.optJSONArray("supportedGenerationMethods"))) {
                                names.add(name);
                            }
                        }
                    }
                    next = data.optString("nextPageToken", null);
                    if (next != null && next.isEmpty()) next = null;
                    pages++;
                } while (next != null && pages < 15);
                if (!names.isEmpty()) {
                    models = names;
                    savePrefs();
                    toast("Berhasil! Ditemukan " + names.size() + " model teks siap pakai.");
                    runOnUiThread(onDone);
                } else {
                    toast("Respon tidak berisi daftar model valid.");
                }
            } catch (Exception e) {
                toast("Gagal: " + e.getMessage());
            }
        });
    }

    private boolean isTextModel(String name, JSONArray methods) {
        String n = name.toLowerCase();
        if (n.contains("embedding") || n.contains("embed") || n.contains("imagen") || n.contains("aqa") || n.contains("veo") || n.contains("ttsd"))
            return false;
        if (methods != null) {
            boolean has = false;
            for (int i = 0; i < methods.length(); i++) {
                if ("generateContent".equals(methods.optString(i))) { has = true; break; }
            }
            if (!has) return false;
        }
        return true;
    }

    private void generateStory(String userPrompt) {
        if (busy) {
            toast("Masih memproses. Tunggu selesai atau tekan tombol batalkan dulu ya.");
            return;
        }
        if (currentApiKey() == null || currentApiKey().isEmpty()) {
            toast("Atur API Key " + selectedProvider + " dulu!");
            return;
        }
        boolean serial = useMemory;
        boolean continuing = serial && storyState != null && storyState.chapters.size() > 0;
        if (userPrompt.isEmpty() && !continuing) {
            toast("Masukkan tema atau judul cerita terlebih dahulu.");
            return;
        }
        if ("Gemini".equals(selectedProvider) && isModelBlacklisted(selectedModel)) {
            toast("Gagal: Model utama (" + selectedModel + ") berada dalam daftar BLACKLIST! Hapus dari blacklist atau ganti model utama.");
            return;
        }
        runId++;
        long myRun = runId;
        busy = true;
        cancelFlag = false;
        setBusy(true);
        int chapterNo = continuing ? storyState.chapters.size() + 1 : 1;
        if (continuing) {
            String ttl = storyState.title == null || storyState.title.isEmpty() ? "novel ini" : storyState.title;
            toast("Melanjutkan " + ttl + " ke Bab " + chapterNo + ". Untuk mulai cerita baru, tekan RESET MEMORI CERITA dulu.");
            status("Sedang menulis Bab " + chapterNo + "...");
        } else {
            status("Sedang menulis cerita...");
        }
        toast("Menulis via " + selectedProvider + " (" + selectedModel + ")... mohon tunggu.");

        executor.execute(() -> {
            try {
                String system = buildSystem(serial, continuing, chapterNo);
                String userContent = buildUserContent(userPrompt, serial, continuing, chapterNo);
                String result;
                if ("Depsic".equals(selectedProvider)) {
                    result = callOpenAICompat("https://api.deepseek.com/chat/completions", depsicApiKey, selectedModel, system, userContent, myRun);
                } else if ("Grup".equals(selectedProvider)) {
                    result = callOpenAICompat("https://api.groq.com/openai/v1/chat/completions", grupApiKey, selectedModel, system, userContent, myRun);
                } else {
                    result = callGemini(selectedModel, system, userContent, myRun);
                }
                if (myRun != runId || cancelFlag) return;
                if (result == null || result.trim().isEmpty()) {
                    runOnUiThread(() -> {
                        setBusy(false);
                        status("Gagal (hasil kosong)");
                        toast("Model tidak menghasilkan teks. Coba lagi atau ganti model.");
                    });
                    return;
                }
                result = cleanText(result);
                if (serial) {
                    if (chapterNo >= 2) result = stripHeader(result);
                    result = ensureHeading(result, chapterNo);
                    if (storyState == null) {
                        String title = extractTitle(result);
                        if (title == null || title.isEmpty()) title = userPrompt.length() > 60 ? userPrompt.substring(0, 60) : userPrompt;
                        storyState = StoryState.create(title, pickAuthor(), activeCategory(), userPrompt);
                    }
                    StoryState.Chapter ch = new StoryState.Chapter();
                    ch.n = chapterNo;
                    ch.title = chapterTitle(result);
                    ch.chars = result.length();
                    ch.words = wordCount(result);
                    ch.model = selectedModel;
                    storyState.writeChapter(this, chapterNo, result);
                    storyState.chapters.add(ch);
                    storyState.save(this);
                    lastIsChapter = true;
                    lastUsedModel = selectedModel;
                    showResult(result, "HASIL BAB " + chapterNo + " (Diproses Model: " + selectedModel + "):");
                    toast("Selesai via " + selectedModel + "!\nTotal Karakter: " + result.length() + " | Kata: " + wordCount(result));
                    if (result.length() >= 100000) saveAsTxt(result, true);
                    runOnUiThread(() -> {
                        setBusy(false);
                        lblActiveStatus.setText(readyText());
                        toast("Memori cerita diperbarui. Bab berikutnya: Bab " + (chapterNo + 1));
                    });
                } else {
                    lastIsChapter = false;
                    lastUsedModel = selectedModel;
                    showResult(result, "HASIL NOVEL (Diproses Model: " + selectedModel + "):");
                    toast("Selesai via " + selectedModel + "!\nTotal Karakter: " + result.length() + " | Kata: " + wordCount(result));
                    if (result.length() >= 100000) saveAsTxt(result, true);
                    runOnUiThread(() -> {
                        setBusy(false);
                        status("Selesai diproses oleh Model: " + selectedModel);
                    });
                }
            } catch (Exception e) {
                if (myRun == runId) {
                    runOnUiThread(() -> {
                        setBusy(false);
                        status("Gagal (" + e.getMessage() + ")");
                        toast("Gagal memproses cerita: " + e.getMessage());
                    });
                }
            }
        });
    }

    private String activeCategory() {
        if ("Custom (Kategori Sendiri)".equals(selectedCategory)) {
            return customCategoryText.isEmpty() ? "Umum" : customCategoryText;
        }
        return selectedCategory;
    }

    private String pickAuthor() {
        if (writerRoom != null && !writerRoom.isEmpty()) {
            String[] lines = writerRoom.split("\n");
            List<String> authors = new ArrayList<>();
            for (String l : lines) {
                l = l.trim();
                if (!l.isEmpty()) authors.add(l);
            }
            if (!authors.isEmpty()) return authors.get((int) (Math.random() * authors.size()));
        }
        return "Penulis Anonim";
    }

    private String getStyleInstruction() {
        switch (selectedStyle) {
            case "Feminin (Vokal Ganda & Sangat Cewek)":
                return "Gaya penulisan feminin total, cewek banget, kaya ekspresi emosi vokal ganda (seperti: 'ihhh', 'kannn', 'aduuuh'), penuh perhatian pada detail emosional wanita.";
            case "Laki-laki (Lugas & Tangguh)":
                return "Gaya penulisan maskulin, langsung pada poin utama, lugas, tegas, tidak bertele-tele, menggunakan nada bicara pria tangguh.";
            case "Ceplas-ceplos & Tanpa Sensor":
                return "Gaya penulisan ceplas-ceplos, blak-blakan, tanpa sensor kata, sangat murni ekspresi lisan sehari-hari.";
            case "Ceplas-ceplos Sering Salah Ngomong & Salah Pengertian (Ada eh, anu, dll)":
                return "Gaya penulisan ceplas-ceplos yang sering salah ucap, salah paham, penuh kata penyela spontan ('eh', 'anu', 'maksudku bukan gitu'), sangat realistis.";
            case "Gagap / Latah (A-aku, Eh copot-copot)":
                return "Gaya penulisan dengan karakter yang sering gagap lisan dan latah spontan ('eh copot') saat narasi atau dialog.";
            case "Sering Nge-gas (Emosional & Nada Tinggi)":
                return "Gaya penulisan emosional, sering nge-gas, gampang tersulut emosi, menggunakan nada tinggi.";
            case "Sering Salah Tingkah (Canggung & Gelagapan)":
                return "Gaya penulisan canggung, sering salah tingkah (salting), gelagapan, narasi serba bingung sendiri.";
            case "Gaya Suka Menangis (Cengeng & Melankolis)":
                return "Gaya penulisan sangat melankolis, cengeng, mudah tersutuh, derai air mata dan kesedihan mendalam.";
            case "Puitis, Simbolis & Metaforis Mendalam":
                return "Gaya penulisan kaya akan metafora puitis, deskripsi artistik mendalam, gaya sastra tinggi bernilai estetis mutlak.";
            case "Sinis, Sarkastik & Satir Sharp":
                return "Gaya penulisan penuh nada sinis, sindiran tajam, sarkasme organik, dan pandangan dunia yang gelap humoris.";
            case "Berwibawa & Bijaksana (Raja/Tetua)":
                return "Gaya penulisan formal berwibawa, penuh kearifan tua, lugas, tenang, anggun, bagaikan narator sejarah agung.";
            case "Anak Kecil (Polos, Lucu & Banyak Tanya)":
                return "Gaya penulisan seperti sudut pandang anak kecil: polos, jujur, sering bertanya, bahasa sederhana, penuh rasa ingin tahu, dan lucu tanpa dibuat-buat.";
            case "Remaja Gaul (Bahasa Anak Muda & Santai)":
                return "Gaya penulisan remaja gaul, bahasa anak muda, santai, banyak slang sehari-hari, dialog kasual, dan suasana kekinian.";
            case "Horor Gelap (Tegang, Mencekam & Seram)":
                return "Gaya penulisan horor gelap: tegang, mencekam, seram, fokus pada detail gelap, bayangan, bunyi aneh, dan suasana menakutkan.";
            case "Romantis Manis (Lembut, Mesra & Penuh Perasaan)":
                return "Gaya penulisan romantis manis: lembut, mesra, penuh perasaan, detail gestur kasih sayang, bisikan batin, dan suasana hangat.";
            case "Komedi Garing (Humor Kering, Absurd & Konyol)":
                return "Gaya penulisan komedi garing: humor kering, absurd, konyol, timing lucu yang datar, dan situasi konyol tanpa terlalu dipaksakan.";
            case "Custom (Gaya Penulisan Sendiri)":
                return customStyleText.isEmpty() ? "Gaya penulisan bebas manusia alami." : customStyleText;
            default:
                return "Gaya penulisan netral alami manusia.";
        }
    }

    private String buildSystem(boolean serial, boolean continuing, int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("SISTEM: KAMU ADALAH MODEL PRO PENULIS NOVEL MANUSIA ASLI DENGAN KEMAMPUAN BEBAS LENGKAP. TUGASMU HANYA MENULIS CERITA UTUH LENGKAP TANPA BATASAN DAN TANPA TULISAN ROBOTIK. DILARANG MENYAPA DAN DILARANG MEMBERI PENJELASAN DI AWAL/AKHIR.\n");
        sb.append("Kategori Novel: ").append(activeCategory());
        String author = pickAuthor();
        if (serial && continuing) {
            String ttl = storyState.title == null || storyState.title.isEmpty() ? "novel ini" : storyState.title;
            sb.append("\n[STRUKTUR FORMAT BAB LANJUTAN]:\nIni adalah BAB ").append(n).append(" dari novel bersambung berjudul \"").append(ttl)
                    .append("\", ditulis oleh ").append(author)
                    .append(". JANGAN menulis ulang blok Judul / Ditulis oleh / Pembukaan. Mulai persis dengan baris:\nBAB ").append(n)
                    .append(": [judul bab yang menarik]\nlalu langsung lanjutkan cerita, menyambung mulus dari bab sebelumnya. ")
                    .append("Pertahankan nama tokoh, tempat, aturan dunia, dan gaya penulis (").append(author)
                    .append(") serta kombinasi gaya penulisan berikut: ").append(getStyleInstruction())
                    .append(" Tulis HANYA Bab ").append(n).append(". Akhiri bab dengan penutup yang natural dan menarik.");
        } else {
            sb.append("\n[STRUKTUR FORMAT PERMULAAN NOVEL]:\nCerita HARUS dibuka persis seperti struktur berikut pada baris paling awal:\n")
                    .append("Judul: [Tentukan Judul Novel Sesuai Permintaan]\n")
                    .append("Ditulis oleh: ").append(author).append("\n");
            if (useRealtime) sb.append("Rilis pada: ").append(getCurrentDateIndo()).append("\n");
            sb.append("Pembukaan:\n\n[Mulai alur cerita/prolog. Sesuaikan gaya penulisan, persepsi, emosi, dan gaya bahasa penulis sesuai dengan gender/nama (")
                    .append(author).append(") serta kombinasi gaya penulisan berikut: ").append(getStyleInstruction()).append("].");
            if (serial) {
                sb.append("\n[CATATAN NOVEL BERSAMBUNG PER BAB]: Ini adalah BAB 1 dari novel bersambung. Setelah blok pembukaan di atas, awali isi cerita dengan baris \"BAB 1: [judul bab yang menarik]\". ")
                        .append("Tulis HANYA Bab 1 dan akhiri dengan penutup bab yang membuat pembaca penasaran.");
            }
        }
        if (antiAiEnabled) {
            sb.append("\n[INSTRUKSI KHUSUS ANTI-DETEKTOR AI LEVEL ").append(antiAiLevel).append("%]:\n")
                    .append("- DILARANG KERAS menggunakan kata-kata/pola AI klise seperti: 'dalam dunia yang...', 'suatu hari', 'pada akhirnya', 'merupakan', 'menciptakan ikatan', 'takdir', 'kesimpulan'.\n")
                    .append("- Tulis dengan pola tulisan manusia murni yang penuh imperfeksi alami lisan, variasi panjang kalimat secara acak, dialog organik, emosi jujur, dan alur bebas kaku.\n")
                    .append("- Hapus semua gaya pembuka/penutup robotik.");
        }
        if (characterRoom != null && !characterRoom.isEmpty())
            sb.append("\n[DATABASE NAMA & KARAKTER TOKOH PERMANEN]:\n").append(characterRoom).append("\n");
        if (worldBuilding != null && !worldBuilding.isEmpty())
            sb.append("\n[DATABASE PEMBANGUNAN DUNIA (WORLDBUILDING)]:\n").append(worldBuilding).append("\n");
        if (plotOutline != null && !plotOutline.isEmpty())
            sb.append("\n[GARIS BESAR PLOT & BAB CERITA]:\n").append(plotOutline).append("\n");
        if (magicTechSystem != null && !magicTechSystem.isEmpty())
            sb.append("\n[SISTEM SIHIR / TEKNOLOGI / ATURAN DUNIA]:\n").append(magicTechSystem).append("\n");
        if (factionsOrg != null && !factionsOrg.isEmpty())
            sb.append("\n[PROFIL ORGANISASI / FAKSI / KELOMPOK]:\n").append(factionsOrg).append("\n");
        if (serial && continuing && storyState != null) {
            sb.append("\n").append(storyState.context(this, 80000)).append("\n(Lanjutkan jalurnya secara berkesinambungan).");
        }
        sb.append("\n[PERINGATAN EKSTREM KELUARAN TOKEN MAXIMUM & GIANT DOCUMENT TOTALITY 100%]:\n")
                .append("- TULISKAN CERITA/NOVEL INI DENGAN PANJANG MAKSIMUM TAK TERBATAS (GIANT NOVEL). KONSUMSI DAN HABISKAN SETIAP BITS TOKEN YANG TERSEDIA!\n")
                .append("- KEMBANGKAN SETIAP ADEGAN SECARA SANGAT PANJANG, DETAIL, MENDALAM, DAN EKSTREM: jabarkan suasana latar tempat, tekstur benda, pencahayaan, aroma, bau, gestur halus tokoh, dinamika emosi, bisikan batin, monolog internal, dan dialog bernas berliku-liku.\n")
                .append("- JANGAN PERNAH MENYINGKAT CERITA, JANGAN MELEWATKAN ADEGAN DENGAN RANGKUMAN CEPAT, JANGAN MEMPERCEPAT WAKTU SECARA INSTAN!\n")
                .append("- EXHAUSTIVE EXPANSION: Buat setiap interaksi terasa hidup, nyata, dramatis, kompleks, dan raksasa.");
        return sb.toString();
    }

    private String buildUserContent(String prompt, boolean serial, boolean continuing, int n) {
        if (serial && continuing) {
            String direction = prompt.isEmpty()
                    ? "(tidak ada arahan khusus: lanjutkan alur secara natural sesuai memori, garis besar plot, dan arah yang disarankan)"
                    : prompt;
            return "\n\nARAHAN UNTUK BAB " + n + ": " + direction;
        }
        return "\n\nREQ PERMINTAAN TEMA BARU: " + prompt;
    }

    private String callGemini(String model, String system, String userContent, long myRun) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", userContent);
        parts.put(part);
        userMsg.put("parts", parts);
        contents.put(userMsg);
        body.put("contents", contents);

        if (system != null && !system.isEmpty()) {
            JSONObject sys = new JSONObject();
            JSONArray sysParts = new JSONArray();
            JSONObject sp = new JSONObject();
            sp.put("text", system);
            sysParts.put(sp);
            sys.put("parts", sysParts);
            body.put("systemInstruction", sys);
        }

        JSONObject gc = new JSONObject();
        gc.put("temperature", tempVal / 100.0);
        gc.put("maxOutputTokens", 65536);
        body.put("generationConfig", gc);

        if (useWebSearch) {
            JSONArray tools = new JSONArray();
            JSONObject tool = new JSONObject();
            tool.put("google_search", new JSONObject());
            tools.put(tool);
            body.put("tools", tools);
        }

        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + java.net.URLEncoder.encode(apiKey, "UTF-8");
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("x-goog-api-key", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(900000);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        out.write(body.toString());
        out.flush();
        out.close();

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();

        if (myRun != runId || cancelFlag) return null;
        if (code != 200) {
            String errBody = sb.toString();
            String low = errBody.toLowerCase();
            if (code == 404 && low.contains("no longer available") && !model.equals("gemini-3.8-flash")) {
                selectedModel = "gemini-3.8-flash";
                savePrefs();
                return callGemini("gemini-3.8-flash", system, userContent, myRun);
            }
            if (code == 404 && low.contains("no longer available") && !model.equals("gemini-3.6-flash") && !model.equals("gemini-3.8-flash")) {
                return callGemini("gemini-3.6-flash", system, userContent, myRun);
            }
            throw new Exception("HTTP " + code + ": " + errBody.substring(0, Math.min(300, errBody.length())));
        }
        JSONObject resp = new JSONObject(sb.toString());
        JSONArray cands = resp.optJSONArray("candidates");
        if (cands == null || cands.length() == 0) throw new Exception("respons kosong dari model");
        JSONObject cand = cands.getJSONObject(0);
        JSONObject content = cand.optJSONObject("content");
        if (content == null) throw new Exception("respons kosong dari model");
        JSONArray rparts = content.optJSONArray("parts");
        if (rparts == null) throw new Exception("respons kosong dari model");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < rparts.length(); i++) {
            JSONObject p = rparts.getJSONObject(i);
            if (!p.optBoolean("thought", false) && p.has("text")) {
                text.append(p.getString("text"));
            }
        }
        return text.toString();
    }

    private String callOpenAICompat(String endpoint, String key, String model, String system, String userContent, long myRun) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", tempVal / 100.0);
        body.put("max_tokens", 8192);
        JSONArray messages = new JSONArray();
        if (system != null && !system.isEmpty()) {
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", system);
            messages.put(sysMsg);
        }
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userContent);
        messages.put(userMsg);
        body.put("messages", messages);

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(900000);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        out.write(body.toString());
        out.flush();
        out.close();

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();

        if (myRun != runId || cancelFlag) return null;
        if (code != 200) {
            throw new Exception("HTTP " + code + ": " + sb.toString().substring(0, Math.min(300, sb.length())));
        }
        JSONObject resp = new JSONObject(sb.toString());
        JSONArray choices = resp.optJSONArray("choices");
        if (choices == null || choices.length() == 0) throw new Exception("respons kosong dari model");
        JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
        if (msg == null) throw new Exception("respons kosong dari model");
        return msg.optString("content", "");
    }

    private String cleanText(String t) {
        t = t.replace("\r\n", "\n");
        t = t.replaceAll("(?m)^\\s*Tentu[^\\n]*:\\s*\\n", "");
        t = t.replaceAll("(?m)^\\s*Berikut[^\\n]*:\\s*\\n", "");
        t = t.replaceAll("Sebagai AI[^\\n]*\\.", "");
        t = t.replaceAll("(?m)^```[\\w]*\\n", "");
        t = t.replaceAll("(?m)\\n```[\\w]*\\s*$", "");
        t = t.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        t = t.replaceAll("(?m)^#+[ \\t]*", "");
        t = t.replaceAll("(?m)\\n#+[ \\t]*", "\n");
        return t.trim();
    }

    private String stripHeader(String text) {
        String[] lines = text.split("\n");
        int i = 0, removed = 0;
        while (i < lines.length && removed < 6) {
            String t = lines[i].trim().toLowerCase();
            if (t.isEmpty()) { i++; continue; }
            if (t.startsWith("judul:") || t.startsWith("ditulis oleh:") || t.startsWith("rilis pada:") || t.equals("pembukaan") || t.equals("pembukaan:")) {
                i++; removed++;
            } else break;
        }
        if (removed == 0) return text;
        StringBuilder out = new StringBuilder();
        for (int j = i; j < lines.length; j++) {
            if (j > i) out.append("\n");
            out.append(lines[j]);
        }
        return out.toString();
    }

    private boolean isChapterLine(String t) {
        if (t.length() == 0 || t.length() > 120) return false;
        String low = t.replaceAll("^[\\*#_\\s]+", "").toLowerCase();
        if (low.matches("^bab\\s+\\d+.*") || low.matches("^chapter\\s+\\d+.*")) return true;
        if (low.matches("^bab\\s+[ivxlc]+$") || low.matches("^bab\\s+[ivxlc]+[\\s\\.:\\-].*")) return true;
        String word = low.replaceAll("[\\s\\p{Punct}]+$", "");
        return word.equals("prolog") || word.equals("epilog");
    }

    private String chapterTitle(String text) {
        String[] lines = text.split("\n");
        int cnt = 0;
        for (String l : lines) {
            if (cnt > 12) break;
            String t = l.trim();
            if (t.isEmpty()) continue;
            cnt++;
            if (isChapterLine(t)) {
                String rest = t.replaceAll("(?i)^\\w+\\s+[\\dIVXLCivxlc]+\\s*[:\\.\\-]*\\s*", "").replaceAll("[\\*#_]", "").trim();
                return rest;
            }
        }
        return "";
    }

    private String ensureHeading(String text, int n) {
        String[] lines = text.split("\n");
        int cnt = 0;
        for (String l : lines) {
            String t = l.trim();
            if (t.isEmpty()) continue;
            cnt++;
            if (isChapterLine(t)) return text;
            if (cnt >= 12) break;
        }
        return "BAB " + n + "\n\n" + text;
    }

    private String extractTitle(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)judul\\s*:\\s*([^\\n]+)").matcher(text);
        if (m.find()) return m.group(1).replaceAll("[\\*#_]", "").trim();
        return null;
    }

    private int wordCount(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return s.trim().split("\\s+").length;
    }

    @Override
    protected void onDestroy() {
        if (ttsEngine != null) {
            ttsEngine.stop();
            ttsEngine.shutdown();
        }
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    static class StoryState {
        String id, title, author, category, premise, synopsis, facts, nextHint;
        List<Chapter> chapters = new ArrayList<>();
        List<String> refUrls = new ArrayList<>();

        static class Chapter {
            int n;
            String title = "", summary = "", model = "";
            int chars, words;
            boolean folded;
        }

        static StoryState create(String title, String author, String category, String premise) {
            StoryState s = new StoryState();
            s.id = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            s.title = title;
            s.author = author;
            s.category = category;
            s.premise = premise;
            s.synopsis = "";
            s.facts = "";
            s.nextHint = "";
            return s;
        }

        static File dir(Context ctx) {
            File d = new File(ctx.getFilesDir(), "novelpro_story");
            if (!d.exists()) d.mkdirs();
            return d;
        }

        static File path(Context ctx, int n) {
            return new File(dir(ctx), String.format(Locale.US, "bab_%03d.txt", n));
        }

        static File statePath(Context ctx) {
            return new File(dir(ctx), "state.json");
        }

        static StoryState load(Context ctx) {
            try {
                File f = statePath(ctx);
                if (!f.exists()) return null;
                FileInputStream fis = new FileInputStream(f);
                byte[] data = new byte[(int) f.length()];
                fis.read(data);
                fis.close();
                JSONObject o = new JSONObject(new String(data, StandardCharsets.UTF_8));
                StoryState s = new StoryState();
                s.id = o.optString("id");
                s.title = o.optString("title");
                s.author = o.optString("author");
                s.category = o.optString("category");
                s.premise = o.optString("premise");
                s.synopsis = o.optString("synopsis");
                s.facts = o.optString("facts");
                s.nextHint = o.optString("next_hint");
                JSONArray chs = o.optJSONArray("chapters");
                if (chs != null) {
                    for (int i = 0; i < chs.length(); i++) {
                        JSONObject c = chs.getJSONObject(i);
                        Chapter ch = new Chapter();
                        ch.n = c.optInt("n");
                        ch.title = c.optString("title");
                        ch.summary = c.optString("summary");
                        ch.chars = c.optInt("chars");
                        ch.words = c.optInt("words");
                        ch.model = c.optString("model");
                        ch.folded = c.optBoolean("folded");
                        s.chapters.add(ch);
                    }
                }
                return s;
            } catch (Exception e) {
                return null;
            }
        }

        void save(Context ctx) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("title", title);
                o.put("author", author);
                o.put("category", category);
                o.put("premise", premise);
                o.put("synopsis", synopsis);
                o.put("facts", facts);
                o.put("next_hint", nextHint);
                JSONArray chs = new JSONArray();
                for (Chapter c : chapters) {
                    JSONObject co = new JSONObject();
                    co.put("n", c.n);
                    co.put("title", c.title);
                    co.put("summary", c.summary);
                    co.put("chars", c.chars);
                    co.put("words", c.words);
                    co.put("model", c.model);
                    co.put("folded", c.folded);
                    chs.put(co);
                }
                o.put("chapters", chs);
                FileOutputStream fos = new FileOutputStream(statePath(ctx));
                fos.write(o.toString().getBytes(StandardCharsets.UTF_8));
                fos.close();
            } catch (Exception ignored) {}
        }

        void writeChapter(Context ctx, int n, String text) {
            try {
                FileOutputStream fos = new FileOutputStream(path(ctx, n));
                fos.write(text.getBytes(StandardCharsets.UTF_8));
                fos.close();
            } catch (Exception ignored) {}
        }

        String readChapter(Context ctx, int n) {
            try {
                File f = path(ctx, n);
                if (!f.exists()) return "";
                FileInputStream fis = new FileInputStream(f);
                byte[] data = new byte[(int) f.length()];
                fis.read(data);
                fis.close();
                return new String(data, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "";
            }
        }

        String fullText(Context ctx) {
            StringBuilder sb = new StringBuilder();
            for (Chapter c : chapters) {
                String d = readChapter(ctx, c.n);
                if (d != null && !d.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n\n\n");
                    sb.append(d);
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }

        String context(Context ctx, int budget) {
            int n = chapters.size();
            if (n == 0) return "";
            StringBuilder parts = new StringBuilder();
            parts.append("[MEMORI CERITA BERSAMBUNG: JAGA KESINAMBUNGAN SECARA KETAT]\nJudul novel: ").append(title)
                    .append("\nPenulis: ").append(author)
                    .append("\nBab yang sudah ditulis: ").append(n).append(" (terakhir Bab ").append(n).append(")");
            if (synopsis != null && !synopsis.isEmpty())
                parts.append("\n\n[SINOPSIS BAB-BAB AWAL]:\n").append(synopsis);
            if (facts != null && !facts.isEmpty())
                parts.append("\n\n[FAKTA CANON YANG WAJIB KONSISTEN]:\n").append(facts);
            int first = Math.max(1, n - 5);
            StringBuilder rec = new StringBuilder();
            for (int i = first - 1; i < n; i++) {
                Chapter c = chapters.get(i);
                if (c.summary != null && !c.summary.isEmpty()) {
                    rec.append("Bab ").append(c.n);
                    if (c.title != null && !c.title.isEmpty()) rec.append(" (").append(c.title).append(")");
                    rec.append(": ").append(c.summary).append("\n");
                }
            }
            if (rec.length() > 0) parts.append("\n\n[RINGKASAN BAB-BAB TERAKHIR]:\n").append(rec);
            if (nextHint != null && !nextHint.isEmpty())
                parts.append("\n\n[ARAH YANG DISARANKAN UNTUK BAB BERIKUTNYA]:\n").append(nextHint);
            String tail = readChapter(ctx, n);
            if (tail.length() > 5000) tail = tail.substring(tail.length() - 5000);
            if (!tail.isEmpty())
                parts.append("\n\n[CUPLIKAN AKHIR BAB ").append(n).append(" (teks asli). Sambung dari sini dengan suasana, suara narasi, dan gaya yang sama]:\n").append(tail);
            String text = parts.toString();
            if (text.length() > budget) text = text.substring(0, budget);
            return text;
        }

        String reset(Context ctx) {
            String backup = null;
            if (chapters.size() > 0) {
                String full = fullText(ctx);
                if (full != null && !full.isEmpty()) {
                    File dir = new File(ctx.getFilesDir(), "ProduksiNovelPro");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, "Novel_Cadangan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt");
                    try {
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(full.getBytes(StandardCharsets.UTF_8));
                        fos.close();
                        backup = f.getAbsolutePath();
                    } catch (Exception ignored) {}
                }
                for (Chapter c : chapters) path(ctx, c.n).delete();
            }
            statePath(ctx).delete();
            return backup;
        }

        boolean undoLast(Context ctx) {
            if (chapters.isEmpty()) return false;
            int n = chapters.size();
            path(ctx, chapters.get(n - 1).n).delete();
            chapters.remove(n - 1);
            if (chapters.isEmpty()) {
                statePath(ctx).delete();
            } else {
                save(ctx);
            }
            return true;
        }
    }
}