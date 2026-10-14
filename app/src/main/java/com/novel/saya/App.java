package com.novel.saya;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
/**
* Application class — jalan saat app dibuka, TERLEPAS dari MainActivity.
* Menjalankan NovanService untuk cek update otomatis.
*/
public class App extends Application {
private static App instance;
private static Activity current;
@Override
public void onCreate() {
super.onCreate();
instance = this;
registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
@Override public void onActivityCreated(Activity a, Bundle s) { current = a; }
@Override public void onActivityStarted(Activity a) { current = a; }
@Override public void onActivityResumed(Activity a) {
current = a;
try { UpdateChecker.tryInstallPending(a); } catch (Exception ignored) {}
try { mintaIzinRuntime(a); } catch (Exception ignored) {}
}
@Override public void onActivityPaused(Activity a) { if (current == a) current = null; }
@Override public void onActivityStopped(Activity a) {}
@Override public void onActivitySaveInstanceState(Activity a, Bundle o) {}
@Override public void onActivityDestroyed(Activity a) { if (current == a) current = null; }
});
// Mulai service update (tidak bergantung isi MainActivity)
try {
startService(new Intent(this, NovanService.class));
} catch (Exception ignored) {}
}
public static App get() { return instance; }
public static Activity getCurrentActivity() { return current; }
private static boolean sudahMintaIzin = false;
private static void mintaIzinRuntime(Activity act) {
if (sudahMintaIzin || act == null) return;
if (android.os.Build.VERSION.SDK_INT < 23) return;
try {
android.content.pm.PackageInfo pi = act.getPackageManager().getPackageInfo(
act.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS);
String[] diminta = pi.requestedPermissions;
if (diminta == null || diminta.length == 0) return;
java.util.ArrayList<String> perlu = new java.util.ArrayList<>();
for (String p : diminta) {
try {
android.content.pm.PermissionInfo info = act.getPackageManager().getPermissionInfo(p, 0);
int level = info.protectionLevel & 0xf;
if (level == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS) {
if (act.checkSelfPermission(p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
perlu.add(p);
}
}
} catch (Exception ignored) {}
}
if (!perlu.isEmpty()) {
sudahMintaIzin = true;
act.requestPermissions(perlu.toArray(new String[0]), 9911);
} else {
sudahMintaIzin = true;
}
} catch (Exception ignored) {}
}
}
