package com.spreadcomposer.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;

import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getBridge().getWebView().addJavascriptInterface(new Saver(this), "AndroidSaver");
    }

    /** JS에서 window.AndroidSaver.saveJpg(이름, base64) 로 호출 → 갤러리 Pictures/SpreadComposer 에 저장 */
    static class Saver {
        private final MainActivity activity;

        Saver(MainActivity activity) {
            this.activity = activity;
        }

        @JavascriptInterface
        public String saveJpg(String name, String base64) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                ContentResolver resolver = activity.getContentResolver();

                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, safe);
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SpreadComposer");
                v.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return "MediaStore 등록 실패";

                try (OutputStream os = resolver.openOutputStream(uri)) {
                    if (os == null) return "출력 스트림을 열 수 없음";
                    os.write(bytes);
                }

                v.clear();
                v.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, v, null, null);
                return "ok";
            } catch (Exception e) {
                return String.valueOf(e.getMessage());
            }
        }
    }
}
