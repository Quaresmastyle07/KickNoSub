package de.fm.taktikboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 501;
    private static final String LOCAL_URL = "file:///android_asset/index.html";

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            webView = new WebView(this);
            webView.setBackgroundColor(Color.rgb(7, 16, 28));
            webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            setContentView(webView);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setSupportZoom(false);
            settings.setMediaPlaybackRequiresUserGesture(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setUserAgentString(settings.getUserAgentString() + " FMTaktikboardAndroid/1.7");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    if ("file".equalsIgnoreCase(uri.getScheme())) return false;
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException error) {
                        Toast.makeText(MainActivity.this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                    if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                    fileChooserCallback = callback;
                    try {
                        Intent intent = params.createIntent();
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                        return true;
                    } catch (Exception firstError) {
                        try {
                            Intent fallback = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            fallback.addCategory(Intent.CATEGORY_OPENABLE);
                            fallback.setType("*/*");
                            startActivityForResult(fallback, FILE_CHOOSER_REQUEST);
                            return true;
                        } catch (Exception secondError) {
                            fileChooserCallback = null;
                            Toast.makeText(MainActivity.this, "Dateiauswahl ist auf diesem Gerät nicht verfügbar.", Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                }
            });

            if (savedInstanceState == null) {
                webView.loadUrl(LOCAL_URL);
            } else {
                webView.restoreState(savedInstanceState);
                if (webView.getUrl() == null) webView.loadUrl(LOCAL_URL);
            }
        } catch (Throwable startupError) {
            showSafeErrorScreen(startupError);
        }
    }

    private void showSafeErrorScreen(Throwable error) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(Color.rgb(7, 16, 28));

        TextView title = new TextView(this);
        title.setText("FM Taktikboard konnte nicht gestartet werden");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22f);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("Android System WebView oder Chrome ist auf diesem Gerät beschädigt oder deaktiviert. Bitte beide Apps aktualisieren und FM Taktikboard neu starten.\n\nTechnischer Hinweis: " + error.getClass().getSimpleName());
        message.setTextColor(Color.rgb(180, 198, 218));
        message.setTextSize(15f);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 28, 0, 0);

        layout.addView(title);
        layout.addView(message);
        setContentView(layout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileChooserCallback == null) return;

        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            } else if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                result = new Uri[]{data.getClipData().getItemAt(0).getUri()};
            }
        }

        fileChooserCallback.onReceiveValue(result);
        fileChooserCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}