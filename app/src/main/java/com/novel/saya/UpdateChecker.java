package com.novel.saya;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
* Update otomatis via GitHub Releases.
* Dipanggil dari NovanService / App — TIDAK bergantung pada MainActivity.
*/
public class UpdateChecker {
public static final String GITHUB_OWNER = "depinna3-hue";
public static final String GITHUB_REPO  = "nofela";
private static final String TAG = "UpdateChecker";
private static final String PREFS = "update_checker_prefs";
private static final String KEY_LAST_CHECK = "last_check_ms";
private static final long CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000;
private final Context appContext;
private final Handler ui = new Handler(Looper.getMainLooper());
private final ExecutorService executor = Executors.newSingleThreadExecutor();
private boolean silent;
public UpdateChecker(Context context) {
this.appContext = context.getApplicationContext();
}
/** Panggil dari Service / Application saat app dibuka. */
public void checkSilent() {
SharedPreferences sp = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
long last = sp.getLong(KEY_LAST_CHECK, 0);
if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return;
silent = true;
doCheck();
}
/** Panggil manual (misalnya dari tombol di mana saja). */
public void checkManual() {
silent = false;
Toast.makeText(appContext, "Memeriksa update...", Toast.LENGTH_SHORT).show();
doCheck();
}
private Activity currentActivity() {
try {
return App.getCurrentActivity();
} catch (Throwable t) {
return null;
}
}
private void doCheck() {
executor.execute(() -> {
try {
String api = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";
String json = httpGet(api);
if (json == null || json.isEmpty()) {
notifyFail("Gagal menghubungi server update.");
return;
}
JSONObject release = new JSONObject(json);
String tag = release.optString("tag_name", "").replace("v", "").trim();
String body = release.optString("body", "");
String name = release.optString("name", tag);
int remoteCode = parseVersionCode(tag);
int localCode = getLocalVersionCode();
JSONArray assets = release.optJSONArray("assets");
String apkUrl = null;
String apkName = null;
if (assets != null) {
for (int i = 0; i < assets.length(); i++) {
JSONObject a = assets.getJSONObject(i);
String n = a.optString("name", "");
if (n.toLowerCase().endsWith(".apk")) {
apkUrl = a.optString("browser_download_url", null);
apkName = n;
break;
}
}
}
if (apkUrl == null) {
notifyFail("Tidak ada file APK di rilis terbaru.");
return;
}
appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();
if (remoteCode > localCode) {
final String finalApkUrl = apkUrl;
final String finalApkName = apkName;
String msg = "Versi baru tersedia!\n\n"
+ "Versi lokal : " + getLocalVersionName() + " (" + localCode + ")\n"
+ "Versi baru  : " + name + " (" + remoteCode + ")\n\n"
+ (body.isEmpty() ? "" : body + "\n\n")
+ "Unduh dan pasang sekarang?";
ui.post(() -> showUpdateDialog(msg, finalApkUrl, finalApkName));
} else {
if (!silent) {
ui.post(() -> Toast.makeText(appContext,
"Sudah versi terbaru (" + getLocalVersionName() + ")",
Toast.LENGTH_LONG).show());
}
}
} catch (Exception e) {
Log.e(TAG, "check failed", e);
notifyFail("Error: " + e.getMessage());
}
});
}
private void showUpdateDialog(String message, String apkUrl, String apkName) {
Activity act = currentActivity();
if (act == null || act.isFinishing()) {
// Belum ada Activity — coba lagi sebentar
ui.postDelayed(() -> showUpdateDialog(message, apkUrl, apkName), 1500);
return;
}
new AlertDialog.Builder(act)
.setTitle("Update Tersedia")
.setMessage(message)
.setPositiveButton("Unduh & Pasang", (d, w) -> downloadAndInstall(apkUrl, apkName))
.setNegativeButton("Nanti", null)
.setCancelable(true)
.show();
}
private static final String KEY_PENDING_APK = "pending_apk_path";
private static final String KEY_PENDING_URL = "pending_apk_url";
private static final String KEY_PENDING_NAME = "pending_apk_name";
/** Dipanggil dari App saat Activity resume — lanjut pasang setelah izin diberikan. */
public static void tryInstallPending(Context ctx) {
try {
SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
String path = sp.getString(KEY_PENDING_APK, "");
if (path == null || path.isEmpty()) return;
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
if (!ctx.getPackageManager().canRequestPackageInstalls()) return;
}
File f = new File(path);
if (!f.exists() || f.length() < 1000) {
// File hilang — coba unduh ulang jika URL masih ada
String url = sp.getString(KEY_PENDING_URL, "");
String name = sp.getString(KEY_PENDING_NAME, "update.apk");
if (url != null && !url.isEmpty()) {
new UpdateChecker(ctx).downloadAndInstall(url, name);
}
return;
}
sp.edit().remove(KEY_PENDING_APK).apply();
new UpdateChecker(ctx).installApk(f);
} catch (Exception e) {
Log.e(TAG, "tryInstallPending", e);
}
}
private void savePending(String path, String url, String name) {
appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
.putString(KEY_PENDING_APK, path != null ? path : "")
.putString(KEY_PENDING_URL, url != null ? url : "")
.putString(KEY_PENDING_NAME, name != null ? name : "update.apk")
.apply();
}
private void clearPending() {
appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
.remove(KEY_PENDING_APK)
.remove(KEY_PENDING_URL)
.remove(KEY_PENDING_NAME)
.apply();
}
private File apkTargetFile(String apkName) {
File dir = appContext.getExternalCacheDir();
if (dir == null) dir = appContext.getCacheDir();
if (!dir.exists()) dir.mkdirs();
String name = (apkName != null && apkName.toLowerCase().endsWith(".apk")) ? apkName : "update.apk";
// Hindari karakter aneh di nama file
name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
return new File(dir, name);
}
private void downloadAndInstall(String apkUrl, String apkName) {
Activity act = currentActivity();
if (act == null) {
Toast.makeText(appContext, "Buka aplikasi dulu untuk memasang update.", Toast.LENGTH_LONG).show();
return;
}
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
if (!act.getPackageManager().canRequestPackageInstalls()) {
// Simpan URL agar bisa dilanjutkan setelah izin aktif
savePending("", apkUrl, apkName);
new AlertDialog.Builder(act)
.setTitle("Izin Diperlukan")
.setMessage("Izinkan \"Pasang aplikasi tidak dikenal\" untuk aplikasi ini.\n\nSetelah diaktifkan, kembali ke aplikasi — pemasangan akan dilanjutkan otomatis.")
.setPositiveButton("Buka Pengaturan", (d, w) -> {
try {
Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
i.setData(Uri.parse("package:" + act.getPackageName()));
i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
act.startActivity(i);
} catch (Exception e) {
Intent i = new Intent(Settings.ACTION_SECURITY_SETTINGS);
i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
act.startActivity(i);
}
})
.setNegativeButton("Batal", (d, w) -> clearPending())
.show();
return;
}
}
ProgressDialog pd = new ProgressDialog(act);
pd.setTitle("Mengunduh Update");
pd.setMessage("Sedang mengunduh APK...");
pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
pd.setMax(100);
pd.setCancelable(false);
pd.show();
executor.execute(() -> {
File out = null;
try {
URL url = new URL(apkUrl);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setInstanceFollowRedirects(true);
conn.setConnectTimeout(20000);
conn.setReadTimeout(120000);
conn.setRequestProperty("User-Agent", "App-Updater");
conn.connect();
int code = conn.getResponseCode();
// Ikuti redirect manual jika perlu
if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
|| code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
String loc = conn.getHeaderField("Location");
conn.disconnect();
conn = (HttpURLConnection) new URL(loc).openConnection();
conn.setConnectTimeout(20000);
conn.setReadTimeout(120000);
conn.setRequestProperty("User-Agent", "App-Updater");
conn.connect();
}
int total = conn.getContentLength();
InputStream in = conn.getInputStream();
out = apkTargetFile(apkName);
if (out.exists()) out.delete();
FileOutputStream fos = new FileOutputStream(out);
byte[] buf = new byte[8192];
long downloaded = 0;
int n;
while ((n = in.read(buf)) != -1) {
fos.write(buf, 0, n);
downloaded += n;
if (total > 0) {
final int pct = (int) (downloaded * 100 / total);
ui.post(() -> pd.setProgress(Math.min(pct, 100)));
}
}
fos.flush();
fos.close();
in.close();
conn.disconnect();
// Pastikan file bisa dibaca installer
out.setReadable(true, false);
final File apkFile = out;
savePending(apkFile.getAbsolutePath(), apkUrl, apkName);
ui.post(() -> {
pd.dismiss();
installApk(apkFile);
});
} catch (Exception e) {
Log.e(TAG, "download failed", e);
if (out != null) out.delete();
ui.post(() -> {
pd.dismiss();
Toast.makeText(appContext, "Gagal unduh: " + e.getMessage(), Toast.LENGTH_LONG).show();
});
}
});
}
private void installApk(File apk) {
if (apk == null || !apk.exists()) {
Toast.makeText(appContext, "Berkas APK tidak ditemukan.", Toast.LENGTH_LONG).show();
return;
}
Activity act = currentActivity();
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
if (!appContext.getPackageManager().canRequestPackageInstalls()) {
savePending(apk.getAbsolutePath(), "", apk.getName());
if (act != null && !act.isFinishing()) {
new AlertDialog.Builder(act)
.setTitle("Izin Diperlukan")
.setMessage("Aktifkan izin pasang aplikasi, lalu kembali ke sini.")
.setPositiveButton("Buka Pengaturan", (d, w) -> {
Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
i.setData(Uri.parse("package:" + appContext.getPackageName()));
i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
appContext.startActivity(i);
})
.setNegativeButton("Batal", null)
.show();
}
return;
}
}
try {
apk.setReadable(true, false);
String authority = appContext.getPackageName() + ".fileprovider";
Uri uri = FileProvider.getUriForFile(appContext, authority, apk);
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setDataAndType(uri, "application/vnd.android.package-archive");
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
// Beberapa OEM butuh ClipData agar URI terbaca installer
intent.setClipData(ClipData.newRawUri("", uri));
// Beri izin eksplisit ke package installer (jika ada)
try {
appContext.grantUriPermission("com.android.packageinstaller", uri,
Intent.FLAG_GRANT_READ_URI_PERMISSION);
} catch (Exception ignored) {}
try {
appContext.grantUriPermission("com.google.android.packageinstaller", uri,
Intent.FLAG_GRANT_READ_URI_PERMISSION);
} catch (Exception ignored) {}
if (act != null) {
act.startActivity(intent);
} else {
appContext.startActivity(intent);
}
// Jangan hapus pending dulu — user bisa batal di layar install
} catch (Exception e) {
Log.e(TAG, "install failed", e);
String msg = e.getMessage() != null ? e.getMessage() : "unknown";
// Fallback: coba lewat file:// (Android lama)
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
try {
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
appContext.startActivity(intent);
return;
} catch (Exception e2) {
msg = e2.getMessage();
}
}
Activity a = currentActivity();
String full = "Gagal memasang update.\n\n"
+ msg
+ "\n\nTips: pastikan izin \"Pasang aplikasi tidak dikenal\" aktif. "
+ "Jika tetap gagal, kemungkinan tanda tangan APK berbeda — hapus app lama lalu pasang ulang.";
if (a != null && !a.isFinishing()) {
new AlertDialog.Builder(a)
.setTitle("Gagal Memasang")
.setMessage(full)
.setPositiveButton("Oke", null)
.show();
} else {
Toast.makeText(appContext, "Gagal memasang: " + msg, Toast.LENGTH_LONG).show();
}
}
}
private int getLocalVersionCode() {
try {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
return (int) appContext.getPackageManager()
.getPackageInfo(appContext.getPackageName(), 0).getLongVersionCode();
} else {
return appContext.getPackageManager()
.getPackageInfo(appContext.getPackageName(), 0).versionCode;
}
} catch (PackageManager.NameNotFoundException e) {
return 0;
}
}
private String getLocalVersionName() {
try {
return appContext.getPackageManager()
.getPackageInfo(appContext.getPackageName(), 0).versionName;
} catch (Exception e) {
return "?";
}
}
private int parseVersionCode(String tag) {
if (tag == null || tag.isEmpty()) return 0;
// Tag resmi: v1, v2, v10 (mengikuti versionCode di build.gradle)
String t = tag.trim();
if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
// Ambil angka di depan saja (abaikan sufiks seperti -debug)
int i = 0;
while (i < t.length() && t.charAt(i) >= '0' && t.charAt(i) <= '9') i++;
if (i == 0) return 0;
try {
String num = t.substring(0, i);
if (num.length() > 9) num = num.substring(0, 9);
return Integer.parseInt(num);
} catch (Exception e) {
return 0;
}
}
private String httpGet(String urlStr) throws Exception {
URL url = new URL(urlStr);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setConnectTimeout(15000);
conn.setReadTimeout(15000);
conn.setRequestProperty("Accept", "application/vnd.github+json");
conn.setRequestProperty("User-Agent", "App-Updater");
int code = conn.getResponseCode();
InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
if (is == null) return null;
BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
StringBuilder sb = new StringBuilder();
String line;
while ((line = br.readLine()) != null) sb.append(line);
br.close();
conn.disconnect();
return sb.toString();
}
private void notifyFail(String msg) {
if (!silent) {
ui.post(() -> Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show());
}
}
}
