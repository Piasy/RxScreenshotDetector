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
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import com.tbruyelle.rxpermissions.RxPermissions;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

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
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Observable<String> start(final Context context) {
        return RxPermissions.getInstance(context)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .flatMap(new Func1<Boolean, Observable<String>>() {
                    @Override
                    public Observable<String> call(Boolean granted) {
                        if (granted) {
                            return startAfterPermissionGranted(context);
                        } else {
                            return Observable.error(
                                    new SecurityException("Permission not granted"));
                        }
                    }
                });
    }

    private static Observable<String> startAfterPermissionGranted(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                final ContentObserver contentObserver = new ContentObserver(null) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        Log.d(TAG, "onChange: " + selfChange + ", " + uri.toString());
                        if (uri.toString().matches(EXTERNAL_CONTENT_URI_MATCHER)) {
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
                                    if (path.toLowerCase().contains("screenshot") &&
                                            Math.abs(currentTime - dateAdded) <=
                                                    DEFAULT_DETECT_WINDOW_SECONDS) {
                                        subscriber.onNext(path);
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

                subscriber.add(unsubscribeInUiThread(new Action0() {
                    @Override
                    public void call() {
                        contentResolver.unregisterContentObserver(contentObserver);
                    }
                }));
            }
        });
    }

    /**
     * This function is grab from
     * from https://github.com/pwittchen/ReactiveNetwork/blob/master/library%2Fsrc%2Fmain%2Fjava
     * %2Fcom%2Fgithub%2Fpwittchen%2Freactivenetwork%2Flibrary%2FReactiveNetwork.java
     *
     * Copyright (C) 2015 Piotr Wittchen
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    private static Subscription unsubscribeInUiThread(final Action0 unsubscribe) {
        return Subscriptions.create(new Action0() {

            @Override
            public void call() {
                if (Looper.getMainLooper() == Looper.myLooper()) {
                    unsubscribe.call();
                } else {
                    final Scheduler.Worker inner = AndroidSchedulers.mainThread().createWorker();
                    inner.schedule(new Action0() {
                        @Override
                        public void call() {
                            unsubscribe.call();
                            inner.unsubscribe();
                        }
                    });
                }
            }
        });
    }
}
