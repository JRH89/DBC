package card.digitalbusinesscard;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class MyJavaScriptInterface {
    private Context context;

    public MyJavaScriptInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void saveImage(String base64Image) {
        // Here you can implement the code to save the image to a file
    }
}
