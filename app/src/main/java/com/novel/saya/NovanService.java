package com.novel.saya;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
/**
* Service update otomatis.
* Dipanggil saat app dibuka lewat App.java — TIDAK bergantung MainActivity.
* MainActivity boleh diganti total; update tetap jalan.
*/
public class NovanService extends Service {
private static boolean sudahCek = false;
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
if (!sudahCek) {
sudahCek = true;
// Sedikit jeda agar Activity sempat tampil (untuk dialog update)
new Handler(Looper.getMainLooper()).postDelayed(() -> {
try {
new UpdateChecker(getApplicationContext()).checkSilent();
} catch (Exception ignored) {}
}, 2000);
}
return START_STICKY;
}
@Override
public IBinder onBind(Intent intent) { return null; }
}
