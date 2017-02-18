# RxScreenshotDetector
Android screenshot detector with ContentObserver and Rx.

Note that this library only work as best effort, it won't (and can't I think) cover all corner cases. Good luck with it :)

## ScreenShot

![screenshot-detector-demo.gif](art/screenshot-detector-demo.gif)

## Usage
Add to gradle dependency of your module build.gradle:

```gradle
repositories {
    maven {
        url  "http://dl.bintray.com/piasy/maven"
    }
}

dependencies {
    compile 'com.github.piasy:rxscreenshotdetector:1.2.0'
}
```

Use in code:

```java
RxScreenshotDetector.start(this)
        .compose(bindToLifecycle())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(path -> mTextView.setText(mTextView.getText() + "\nScreenshot: " + path),
                Throwable::printStackTrace);
```

To use with RxJava 1.x, see [RxJava2Interop](https://github.com/akarnokd/RxJava2Interop).

See [full example](https://github.com/Piasy/RxScreenshotDetector/tree/master/app) for more details.

## Acknowledgements
+  Thanks for [RxPermissions](https://github.com/tbruyelle/RxPermissions), for request permission in reactive way.
