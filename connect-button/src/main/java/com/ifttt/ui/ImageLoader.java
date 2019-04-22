package com.ifttt.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import androidx.annotation.MainThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * A simple image downloader with image resizing and a 3MB in-memory cache.
 */
final class ImageLoader {

    private static ImageLoader INSTANCE;

    // 3MB Bitmap cache.
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(3 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            if (SDK_INT >= KITKAT) {
                return value.getAllocationByteCount();
            }

            return value.getByteCount();
        }
    };

    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final Handler handler = new Handler(Looper.getMainLooper());

    static ImageLoader get() {
        if (INSTANCE == null) {
            INSTANCE = new ImageLoader();
        }

        return INSTANCE;
    }

    private ImageLoader() {
    }

    @Nullable
    Call load(Lifecycle lifecycle, String url, OnBitmapLoadedListener listener) {
        Bitmap cached = cache.get(url);
        if (cached != null) {
            listener.onComplete(cached);
            return null;
        }

        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        lifecycle.addObserver(new CallLifecycleObserver(call));
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> listener.onComplete(null));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handler.post(() -> listener.onComplete(null));
                    return;
                }

                InputStream inputStream = response.body().byteStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                cache.put(url, bitmap);
                handler.post(() -> listener.onComplete(bitmap));
            }
        });

        return call;
    }

    interface OnBitmapLoadedListener {
        @MainThread
        void onComplete(@Nullable Bitmap bitmap);
    }

    private static final class CallLifecycleObserver implements LifecycleObserver {
        private final Call call;

        private CallLifecycleObserver(Call call) {
            this.call = call;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        void onStop() {
            call.cancel();
        }
    }
}
