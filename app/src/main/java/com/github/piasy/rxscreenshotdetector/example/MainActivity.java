package com.github.piasy.rxscreenshotdetector.example;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.piasy.rxscreenshotdetector.RxScreenshotDetector;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String[] PROJECTION = new String[] {
            MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
    };
    private static final String SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC";
    @Bind(R.id.mText)
    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        RxScreenshotDetector.start(getApplicationContext())
                //.compose(bindToLifecycle()) // todo wait for RxLifeCycle 2.x
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String path) throws Exception {
                        mTextView.setText(mTextView.getText() + "\nScreenshot: " + path);
                    }
                });
    }

    @OnClick(R.id.mBtnTest)
    public void getAllMedia() {
        final ContentResolver contentResolver = getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(Uri.parse("content://media/external/images/media"),
                    PROJECTION, null, null, SORT_ORDER);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    long dateAdded = cursor.getLong(
                            cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
                    long currentTime = System.currentTimeMillis() / 1000;
                    Log.d(TAG, "path: " + path + ", dateAdded: " + dateAdded +
                               ", currentTime: " + currentTime);
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
}
