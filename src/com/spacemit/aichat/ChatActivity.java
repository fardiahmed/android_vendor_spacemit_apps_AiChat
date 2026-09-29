package com.spacemit.aichat;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* Shows the local vendor.llama-server web UI, or how to add a model. */
public class ChatActivity extends Activity {
    private static final String TAG = "AiChat";
    private static final String SERVER = "http://127.0.0.1:8080";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView = new WebView(this);
        WebSettings s = mWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // the UI keeps conversations in IndexedDB/localStorage
        s.setMediaPlaybackRequiresUserGesture(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("app://retry")) {
                    check();
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    Log.w(TAG, "load failed: " + error.getDescription());
                    showHelp(false);
                }
            }
        });
        setContentView(mWebView);
        if (savedInstanceState == null || !restore(savedInstanceState)) check();
    }

    private boolean restore(Bundle state) {
        return mWebView.restoreState(state) != null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        mExecutor.shutdownNow();
        mWebView.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /** Opens the UI if the server answers and lists at least one model. */
    private void check() {
        mExecutor.execute(() -> {
            int models = -1;
            try {
                JSONObject json = new JSONObject(get(SERVER + "/v1/models"));
                models = json.getJSONArray("data").length();
            } catch (Exception e) {
                Log.i(TAG, "server not reachable: " + e);
            }
            final int count = models;
            mMain.post(() -> {
                if (isDestroyed()) return;
                if (count > 0) {
                    mWebView.loadUrl(SERVER + "/");
                } else {
                    showHelp(count == 0);
                }
            });
        });
    }

    private void showHelp(boolean serverUp) {
        String status = serverUp
                ? "The AI server is running but has no model yet."
                : "The AI server is not running.";
        String html = "<html><head><meta name='viewport' content='width=device-width'>"
                + "<style>body{font-family:sans-serif;background:#121212;color:#eee;padding:24px;"
                + "line-height:1.5}code{background:#2a2a2a;padding:2px 6px;border-radius:4px}"
                + "a{display:inline-block;margin-top:16px;padding:10px 20px;background:#7b1fa2;"
                + "color:#fff;border-radius:6px;text-decoration:none}</style></head><body>"
                + "<h2>AI Chat</h2><p>" + status + "</p>"
                + "<p>Copy a model in GGUF format (for example Qwen2.5-1.5B-Instruct Q4_K_M "
                + "from huggingface.co) to the board:</p>"
                + "<p><code>adb push model.gguf /data/vendor/llm/</code></p>"
                + (serverUp ? "" : "<p>Start the server with "
                        + "<code>adb shell setprop persist.vendor.llm.enable 1</code></p>")
                + "<p>Models run on the SpacemiT K1 AI cores (IME matrix instructions); "
                + "1-3B parameter models at Q4 fit comfortably.</p>"
                + "<a href='app://retry'>Retry</a></body></html>";
        mWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(2000);
        c.setReadTimeout(5000);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } finally {
            c.disconnect();
        }
    }
}
