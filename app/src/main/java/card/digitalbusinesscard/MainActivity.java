package card.digitalbusinesscard;
import android.os.Bundle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new MyJavaScriptInterface(this), "Android");
        webView.loadUrl("file:///android_asset/index.html");
        webView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Check if the user long-pressed on an image
                if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg")) {
                    DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(url));
                    downloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    downloadRequest.setAllowedOverRoaming(false);
                    downloadRequest.setTitle("Image download");
                    downloadRequest.setDescription("Downloading image file");
                    downloadRequest.setVisibleInDownloadsUi(true);
                    downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "image.png");

                    // Get the download manager and enqueue the download request
                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    downloadManager.enqueue(downloadRequest);

                    return true; // Tell the WebView that we handled the URL
                } else if (url.equals("javascript:downloadQRCode()")) {
                    // Call the downloadQRCode method to download the QR code image
                    webView.loadUrl("javascript:downloadQRCode()");
                    return true; // Tell the WebView that we handled the URL
                }

                return super.shouldOverrideUrlLoading(view, request);
            }


        });

    }

    public void downloadQRCode(String imageUrl) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
            return;
        }

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = URLUtil.guessFileName(imageUrl, null, null);
        File file = new File(directory, fileName);
        if (file.exists()) {
            Toast.makeText(this, "File already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
        request.setDescription("Downloading QR code image");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        Toast.makeText(this, "Downloading QR code image...", Toast.LENGTH_SHORT).show();
    }

    public class MyJavaScriptInterface {
        private Context context;

        public MyJavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void downloadImage(String base64Data) {
            // Convert the base64 data to a bitmap
            byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
            Bitmap qrCodeBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            // Save the bitmap to the Pictures directory
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "qrcode.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = context.getContentResolver();
            Uri imageUri = resolver.insert(externalContentUri, values);

            OutputStream outputStream = null;
            try {
                outputStream = resolver.openOutputStream(imageUri);
                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Show a toast message to indicate that the download is complete
            Toast.makeText(context, "QR code saved to Pictures directory", Toast.LENGTH_SHORT).show();
        }
    }



}
