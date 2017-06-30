# ZoomLayout

ZoomLayout is a ViewGroup, that enables pinch-zoom and panning on child-views.

## Dependency

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Then, add the library to your module `build.gradle`
```gradle
dependencies {
    compile 'com.shopgun.android:zoomlayout:0.8.0'
}
```

## Features
- Zoom and pan on any view, using multi-touch.
- Support for scrolling parents (ViewPager e.t.c.)
- Notifications on zoom, pan, tap e.t.c.

## Usage
There is a [sample](https://github.com/shopgun/zoomlayout/tree/master/sample) 

## Issues
Currently childviews doesn't receive touch events.