/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.nekocode.camerafilter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.TextureView;

import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import cn.nekocode.camerafilter.filter.AsciiArtFilter;
import cn.nekocode.camerafilter.filter.BasicDeformFilter;
import cn.nekocode.camerafilter.filter.BlackWhiteBrightFilter;
import cn.nekocode.camerafilter.filter.BlackWhiteDarkFilter;
import cn.nekocode.camerafilter.filter.BlackWhiteFilter;
import cn.nekocode.camerafilter.filter.BlueorangeFilter;
import cn.nekocode.camerafilter.filter.CameraFilter;
import cn.nekocode.camerafilter.filter.ChromaticAberrationFilter;
import cn.nekocode.camerafilter.filter.ContrastFilter;
import cn.nekocode.camerafilter.filter.CrackedFilter;
import cn.nekocode.camerafilter.filter.CrosshatchFilter;
import cn.nekocode.camerafilter.filter.EMInterferenceFilter;
import cn.nekocode.camerafilter.filter.EdgeDetectionFilter;
import cn.nekocode.camerafilter.filter.JFAVoronoiFilter;
import cn.nekocode.camerafilter.filter.LegofiedFilter;
import cn.nekocode.camerafilter.filter.LichtensteinEsqueFilter;
import cn.nekocode.camerafilter.filter.MappingFilter;
import cn.nekocode.camerafilter.filter.MoneyFilter;
import cn.nekocode.camerafilter.filter.NoiseWarpFilter;
import cn.nekocode.camerafilter.filter.OriginalFilter;
import cn.nekocode.camerafilter.filter.PixelizeFilter;
import cn.nekocode.camerafilter.filter.PolygonizationFilter;
import cn.nekocode.camerafilter.filter.RefractionFilter;
import cn.nekocode.camerafilter.filter.TileMosaicFilter;
import cn.nekocode.camerafilter.filter.TrianglesMosaicFilter;
import cn.nekocode.camerafilter.util.MyGLUtils;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class CameraRenderer implements Runnable, TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraRenderer";
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int DRAW_INTERVAL = 1000 / 30;

    private Thread renderThread;
    private Context context;
    private SurfaceTexture surfaceTexture;
    private int gwidth, gheight;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;

    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;
    private EGL10 egl10;

    private Camera camera;
    private SurfaceTexture cameraSurfaceTexture;
    private int cameraTextureId;
    private CameraFilter selectedFilter;
    private int selectedFilterId = R.id.filter_original;
    private SparseArray<CameraFilter> cameraFilterMap = new SparseArray<>();

    public CameraRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        gwidth = -width;
        gheight = -height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
        CameraFilter.release();

        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
        renderThread = new Thread(this);

        surfaceTexture = surface;
        gwidth = -width;
        gheight = -height;

        // Open camera
        Pair<Camera.CameraInfo, Integer> cameraInfo = getCamera(mCameraFacing);
        final int backCameraId = cameraInfo.second;
        camera = Camera.open(backCameraId);

        // Start rendering
        renderThread.start();
    }

    /*
        자동으로 포커싱 객체를 잡는다
     */
    public void focusing() {
        try {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
        }catch(RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void setCameraFacing(int cameraFacing) {
        mCameraFacing = cameraFacing;
    }

    public void setSelectedFilter(int id) {
        selectedFilterId = id;
        selectedFilter = cameraFilterMap.get(id);
        if (selectedFilter != null)
            selectedFilter.onAttach();
    }

    @Override
    public void run() {
        initGL(surfaceTexture);

        // Setup camera filters map
        cameraFilterMap.append(R.id.filter_original, new OriginalFilter(context));
        cameraFilterMap.append(R.id.black_white_default, new BlackWhiteFilter(context));
        cameraFilterMap.append(R.id.black_white_bright, new BlackWhiteBrightFilter(context));
        cameraFilterMap.append(R.id.black_white_dark, new BlackWhiteDarkFilter(context));
        cameraFilterMap.append(R.id.filter_blue_orange, new BlueorangeFilter(context));
        cameraFilterMap.append(R.id.filter_edge_detection, new EdgeDetectionFilter(context));
        cameraFilterMap.append(R.id.filter_pixelize, new PixelizeFilter(context));
        cameraFilterMap.append(R.id.filter_em_interference, new EMInterferenceFilter(context));
        cameraFilterMap.append(R.id.filter_triangles_mosaic, new TrianglesMosaicFilter(context));
        cameraFilterMap.append(R.id.filter_legofied, new LegofiedFilter(context));
        cameraFilterMap.append(R.id.filter_tile_mosaic, new TileMosaicFilter(context));
        cameraFilterMap.append(R.id.filter_chromatic_aberration, new ChromaticAberrationFilter(context));
        cameraFilterMap.append(R.id.filter_basic_deform, new BasicDeformFilter(context));
        cameraFilterMap.append(R.id.filter_contrast, new ContrastFilter(context));
        cameraFilterMap.append(R.id.filter_noise_warp, new NoiseWarpFilter(context));
        cameraFilterMap.append(R.id.filter_refraction, new RefractionFilter(context));
        cameraFilterMap.append(R.id.filter_mapping, new MappingFilter(context));
        cameraFilterMap.append(R.id.filter_crosshatch, new CrosshatchFilter(context));
        cameraFilterMap.append(R.id.filter_lichtenstein_esque, new LichtensteinEsqueFilter(context));
        cameraFilterMap.append(R.id.filter_ascii_art, new AsciiArtFilter(context));
        cameraFilterMap.append(R.id.filter_money_filter, new MoneyFilter(context));
        cameraFilterMap.append(R.id.filter_cracked, new CrackedFilter(context));
        cameraFilterMap.append(R.id.filter_polygonization, new PolygonizationFilter(context));
        cameraFilterMap.append(R.id.filter_jfa_voronoi, new JFAVoronoiFilter(context));
        setSelectedFilter(selectedFilterId);

        // Create texture for camera preview
        cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        cameraSurfaceTexture = new SurfaceTexture(cameraTextureId);

        // Start camera preview
        try {
            camera.setPreviewTexture(cameraSurfaceTexture);
            camera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }

        // Render loop
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (gwidth < 0 && gheight < 0)
                    GLES20.glViewport(0, 0, gwidth = -gwidth, gheight = -gheight);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Update the camera preview texture
                synchronized (this) {
                    cameraSurfaceTexture.updateTexImage();
                }

                // Draw camera preview
                boolean isFacingFront = mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
                selectedFilter.draw(cameraTextureId, gwidth, gheight, isFacingFront);

                // Flush
                GLES20.glFlush();
                egl10.eglSwapBuffers(eglDisplay, eglSurface);

                Thread.sleep(DRAW_INTERVAL);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        cameraSurfaceTexture.release();
        GLES20.glDeleteTextures(1, new int[]{cameraTextureId}, 0);
    }

    private void initGL(SurfaceTexture texture) {
        egl10 = (EGL10) EGLContext.getEGL();

        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] version = new int[2];
        if (!egl10.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!egl10.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        eglSurface = egl10.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            int error = egl10.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException("eglCreateWindowSurface failed " +
                    android.opengl.GLUtils.getEGLErrorString(error));
        }

        if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }
    }

    private Pair<Camera.CameraInfo, Integer> getCamera(int facing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return new Pair<>(cameraInfo, i);
            }
        }
        return null;
    }
}