/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.rxscreenshotdetector;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import com.tbruyelle.rxpermissions2.RxPermissions;
import io.reactivex.Observable;

/**
 * Created by Piasy{github.com/Piasy} on 16/1/29.
 */
public final class RxScreenshotDetector {

    private static final String TAG = "RxScreenshotDetector";
    private static final String EXTERNAL_CONTENT_URI_MATCHER =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString();
    private static final String[] PROJECTION = new String[] {
            MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
    };
    private static final String SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC";
    private static final long DEFAULT_DETECT_WINDOW_SECONDS = 10;

    private final Activity mActivity;
    private final RxPermissions mRxPermissions;

    private RxScreenshotDetector(final Activity activity) {
        mActivity = activity;
        mRxPermissions = new RxPermissions(activity);
    }

    /**
     * start screenshot detect, if permission not granted, the observable will terminated with
     * an onError event.
     *
     * <p>
     * <em>Warning:</em> The created observable keeps a strong reference to {@code context}.
     * Unsubscribe to free this reference.
     * <p>
     *
     * @return {@link Observable} that emits screenshot file path.
     */
    public static Observable<String> start(final Activity activity) {
        return new RxScreenshotDetector(activity)
                .start();
    }

    private static boolean matchPath(String path) {
        return path.toLowerCase().contains("screenshot") || path.contains("截屏") ||
               path.contains("截图");
    }

    private static boolean matchTime(long currentTime, long dateAdded) {
        return Math.abs(currentTime - dateAdded) <= DEFAULT_DETECT_WINDOW_SECONDS;
    }

    private Observable<String> start() {
        return mRxPermissions
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .flatMap(granted -> {
                    if (granted) {
                        return startAfterPermissionGranted(mActivity);
                    } else {
                        return Observable.error(new SecurityException("Permission not granted"));
                    }
                });
    }

    private Observable<String> startAfterPermissionGranted(final Context context) {
        final ContentResolver contentResolver = context.getContentResolver();

        return Observable.create(emitter -> {
            final ContentObserver contentObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    Log.d(TAG, "onChange: " + selfChange + ", " + uri.toString());
                    if (uri.toString().startsWith(EXTERNAL_CONTENT_URI_MATCHER)) {
                        Cursor cursor = null;
                        try {
                            cursor = contentResolver.query(uri, PROJECTION, null, null,
                                    SORT_ORDER);
                            if (cursor != null && cursor.moveToFirst()) {
                                String path = cursor.getString(
                                        cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                                long dateAdded = cursor.getLong(cursor.getColumnIndex(
                                        MediaStore.Images.Media.DATE_ADDED));
                                long currentTime = System.currentTimeMillis() / 1000;
                                Log.d(TAG, "path: " + path + ", dateAdded: " + dateAdded +
                                           ", currentTime: " + currentTime);
                                if (matchPath(path) && matchTime(currentTime, dateAdded)) {
                                    emitter.onNext(path);
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "open cursor fail");
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                    super.onChange(selfChange, uri);
                }
            };
            contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver);

            emitter.setCancellable(
                    () -> contentResolver.unregisterContentObserver(contentObserver));
        });
    }
}
