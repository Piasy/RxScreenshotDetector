# RxScreenshotDetector
Android screenshot detector with ContentObserver and Rx.

Note that this library only work as best effort, it won't (and can't I think) cover corner cases. Good luck with it :)

## ScreenShot

![screenshot-detector-demo.gif](art/screenshot-detector-demo.gif)

## Usage
Add to gradle dependency of your module build.gradle:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'com.github.piasy:rxscreenshotdetector:1.2.0'
}
```

Use in code:

```java
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
```

See [full example](https://github.com/Piasy/RxScreenshotDetector/tree/master/app) for more details.

## Acknowledgements
+  Thanks for [RxPermissions](https://github.com/tbruyelle/RxPermissions), for request permission in reactive way.
