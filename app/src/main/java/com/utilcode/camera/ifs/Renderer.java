package com.utilcode.camera.ifs;

import android.opengl.GLSurfaceView;

/**
 * Description:
 */
public interface Renderer extends GLSurfaceView.Renderer {

    void onDestroy();

    void open();
}
