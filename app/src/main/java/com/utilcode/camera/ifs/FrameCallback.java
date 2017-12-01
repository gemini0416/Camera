package com.utilcode.camera.ifs;

/**
 * Description:
 */
public interface FrameCallback {

    void onFrame(byte[] bytes, long time);

}
