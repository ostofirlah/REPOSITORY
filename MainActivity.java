package org.netatlas.monitor;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root, header, content, tabBar;
    private WebView mapView;
    private TextView status;
    private LocationManager locationManager;
    private boolean mapLoaded = false;
    private static final int LOCATION_REQUEST = 41;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17, 24, 39));
        getWindow().setNavigationBarColor(Color.rgb(17, 24, 39));
        buildShell();
        showMap();
        requestLocationIfNeeded();
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(245,247,250));
        header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(dp(18),dp(15),dp(18),dp(12)); header.setBackgroundColor(Color.rgb(17,24,39));
        TextView title = text("NET ATLAS", 21, Color.WHITE, true);
        TextView subtitle = text("Device network map · Android 16", 12, Color.rgb(203,213,225), false);
        header.addView(title); header.addView(subtitle); root.addView(header);

        tabBar = new LinearLayout(this); tabBar.setOrientation(LinearLayout.HORIZONTAL); tabBar.setPadding(dp(12),dp(8),dp(12),dp(8)); tabBar.setBackgroundColor(Color.WHITE);
        Button mapTab = button("MAP"); mapTab.setOnClickListener(v -> showMap());
        Button listTab = button("LIST"); listTab.setOnClickListener(v -> showApps());
        tabBar.addView(mapTab, new LinearLayout.LayoutParams(0,dp(44),1));
        tabBar.addView(listTab, new LinearLayout.LayoutParams(0,dp(44),1)); root.addView(tabBar);

        status = text("Monitoring engine not connected — no live connections are being reported.", 12, Color.rgb(124,45,18), false);
        status.setPadding(dp(14),dp(10),dp(14),dp(10)); status.setBackgroundColor(Color.rgb(255,247,237)); root.addView(status);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void showMap() {
        content.removeAllViews();
        if (mapView == null) {
            mapView = new WebView(this);
            mapView.getSettings().setJavaScriptEnabled(true);
            mapView.getSettings().setDomStorageEnabled(true);
            mapView.setWebChromeClient(new WebChromeClient());
            mapView.setWebViewClient(new WebViewClient());
            mapView.addJavascriptInterface(new MapBridge(), "AndroidBridge");
            mapView.loadUrl("file:///android_asset/map.html"); mapLoaded = true;
        }
        if (mapView.getParent() != null) ((android.view.ViewGroup)mapView.getParent()).removeView(mapView);
        content.addView(mapView, new LinearLayout.LayoutParams(-1,0,1));
        TextView note = text("Map base: OpenStreetMap. Country lines appear when a real connection-monitoring backend is added.", 12, Color.DKGRAY, false);
        note.setPadding(dp(12),dp(8),dp(12),dp(8)); content.addView(note);
    }

    private void showApps() {
        content.removeAllViews();
        TextView intro = text("APPS INSTALLED ON THIS DEVICE", 14, Color.rgb(31,41,55), true);
        intro.setPadding(dp(16),dp(14),dp(16),dp(8)); content.addView(intro);
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(12),0,dp(12),dp(12));
        List<ApplicationInfo> apps = new ArrayList<>();
        try {
            apps.addAll(getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA));
        } catch (Exception e) { status.setText("Could not read installed apps: " + e.getMessage()); }
        Collections.sort(apps, Comparator.comparing(a -> String.valueOf(getPackageManager().getApplicationLabel(a)).toLowerCase(Locale.ROOT)));
        int count = 0;
        for (ApplicationInfo app : apps) {
            String label;
            try { label = String.valueOf(getPackageManager().getApplicationLabel(app)); } catch (Exception ex) { label = app.packageName; }
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(11),dp(14),dp(11)); card.setBackgroundColor(Color.WHITE);
            TextView name = text(label, 15, Color.rgb(17,24,39), true);
            TextView pkg = text(app.packageName, 11, Color.rgb(107,114,128), false);
            card.addView(name); card.addView(pkg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin = dp(7); list.addView(card,lp); count++;
        }
        scroll.addView(list); content.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        status.setText(count + " apps visible. Per-app destination monitoring is not yet active.");
    }

    private void requestLocationIfNeeded() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST); return;
        }
        try {
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location last = null;
            for (String provider : locationManager.getProviders(true)) {
                try { Location l = locationManager.getLastKnownLocation(provider); if (l != null && (last == null || l.getTime() > last.getTime())) last = l; } catch (SecurityException ignored) {}
            }
            if (last != null) sendLocation(last);
            LocationListener listener = new LocationListener() { @Override public void onLocationChanged(Location l) { sendLocation(l); } };
            for (String provider : locationManager.getProviders(true)) {
                try { locationManager.requestLocationUpdates(provider, 15000, 25, listener); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
    private void sendLocation(Location l) {
        if (mapView != null) {
            final String js = String.format(Locale.US,"window.setDeviceLocation && window.setDeviceLocation(%.7f,%.7f)",l.getLatitude(),l.getLongitude());
            runOnUiThread(() -> mapView.evaluateJavascript(js,null));
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if (requestCode == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) requestLocationIfNeeded();
    }
    @Override protected void onDestroy() { if (locationManager != null) { try { locationManager.removeUpdates((LocationListener)null); } catch (Exception ignored) {} } if (mapView != null) mapView.destroy(); super.onDestroy(); }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(null,1); return t; }
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setTextSize(13); b.setAllCaps(false); return b; }
    private class MapBridge { @JavascriptInterface public void mapReady() { runOnUiThread(() -> { status.setText("Map loaded. Live connection lines are unavailable until the monitoring engine is implemented."); }); } }
}
