package com.novel.saya;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
public class MainActivity extends Activity {
@Override public void onCreate(Bundle state) {
super.onCreate(state);
LinearLayout box = new LinearLayout(this);
box.setOrientation(LinearLayout.VERTICAL);
box.setGravity(Gravity.CENTER);
box.setPadding(32,32,32,32);
TextView title = new TextView(this);
title.setText("nofela");
title.setTextSize(26);
title.setTextColor(Color.BLACK);
box.addView(title);
TextView info = new TextView(this);
info.setText("Aplikasi Android native siap dikembangkan.");
box.addView(info);

setContentView(box);
mintaIzinRuntime();

}
private void mintaIzinRuntime() {
if (android.os.Build.VERSION.SDK_INT < 23) return;
try {
android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(
getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS);
String[] diminta = pi.requestedPermissions;
if (diminta == null || diminta.length == 0) return;
java.util.ArrayList<String> perlu = new java.util.ArrayList<>();
for (String p : diminta) {
try {
android.content.pm.PermissionInfo info = getPackageManager().getPermissionInfo(p, 0);
int level = info.protectionLevel & 0xf;
if (level == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS) {
if (checkSelfPermission(p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
perlu.add(p);
}
}
} catch (Exception ignored) {}
}
if (!perlu.isEmpty()) {
requestPermissions(perlu.toArray(new String[0]), 9911);
}
} catch (Exception ignored) {}
}
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
super.onRequestPermissionsResult(requestCode, permissions, grantResults);
}
}
