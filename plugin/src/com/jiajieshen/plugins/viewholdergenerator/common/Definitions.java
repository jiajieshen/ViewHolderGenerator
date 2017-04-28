package com.jiajieshen.plugins.viewholdergenerator.common;

import java.util.HashMap;

public class Definitions {

    public static final HashMap<String, String> paths = new HashMap<String, String>();

    static {
        // special classes; default package is android.widget.*
        paths.put("WebView", "android.webkit.WebView");
        paths.put("View", "android.view.View");
        paths.put("ViewStub", "android.view.ViewStub");
        paths.put("SurfaceView", "android.view.SurfaceView");
        paths.put("TextureView", "android.view.TextureView");
    }
}
