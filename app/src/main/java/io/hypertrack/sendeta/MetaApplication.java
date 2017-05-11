/*
The MIT License (MIT)

Copyright (c) 2015-2017 HyperTrack (http://hypertrack.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package io.hypertrack.sendeta;

import android.app.Application;
import android.util.Log;

import com.hypertrack.lib.HyperTrack;

import io.hypertrack.sendeta.util.DevDebugUtils;

/**
 * Created by suhas on 11/11/15.
 */
public class MetaApplication extends Application {

    private static MetaApplication mInstance;
    private static boolean activityVisible;

    public static synchronized MetaApplication getInstance() {
        return mInstance;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        // Initialize HyperTrack SDK
        HyperTrack.initialize(this.getApplicationContext(), BuildConfig.HYPERTRACK_PK);
        HyperTrack.enableMockLocations(false);

        // Enable HyperTrack Debug Logging
        DevDebugUtils.setHTLogLevel(Log.ERROR);
        // Log HyperTrack SDK Version
        DevDebugUtils.sdkVersionMessage();
    }
}
