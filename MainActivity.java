package com.spreadcomposer.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

        /** 클립보드의 이미지(content URI)를 [{type, data(base64)}] JSON 문자열로 반환 */
        @JavascriptInterface
        public String readClipboardImages() {
            try {
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm == null || !cm.hasPrimaryClip()) return "[]";
                ClipData cd = cm.getPrimaryClip();
                if (cd == null) return "[]";
                ContentResolver resolver = activity.getContentResolver();
                JSONArray arr = new JSONArray();
                for (int i = 0; i < cd.getItemCount(); i++) {
                    Uri u = cd.getItemAt(i).getUri();
                    if (u == null) continue;
                    String type = resolver.getType(u);
                    if (type == null || !type.startsWith("image/")) continue;
                    try (InputStream in = resolver.openInputStream(u)) {
                        if (in == null) continue;
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                        JSONObject o = new JSONObject();
                        o.put("type", type);
                        o.put("data", Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP));
                        arr.put(o);
                    }
                }
                return arr.toString();
            } catch (Exception e) {
                return "[]";
            }
        }
    }
}
