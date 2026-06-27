package com.swiftlite.keyboard.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ClipboardShareActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) { finish(); return; }

        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    String[] mimes = { type, "image/png", "image/jpeg", "image/*" };
                    ClipData clip = new ClipData("Image", mimes, new ClipData.Item(imageUri));
                    cm.setPrimaryClip(clip);
                    Toast.makeText(this, "Image copied to Typer", Toast.LENGTH_SHORT).show();
                }
            } else if ("text/plain".equals(type)) {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Text", text));
                    Toast.makeText(this, "Text copied to Typer", Toast.LENGTH_SHORT).show();
                }
            }
        }
        finish();
    }
}
