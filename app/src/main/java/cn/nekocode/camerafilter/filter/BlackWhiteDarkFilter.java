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
package cn.nekocode.camerafilter.filter;

import android.content.Context;
import android.opengl.GLES20;

import cn.nekocode.camerafilter.R;
import cn.nekocode.camerafilter.util.MyGLUtils;


/**
 * @author nekocode (nekocode.cn@gmail.com)
 * 어두운 흑백 필터
 * 쉐이더 안에서 rgb값과 적절한 상수들의 내적을 통해 구현한다.
 * vec의 xyz값이 곧 rgb값이다.
 * 이 결과에 어두운 brightness를 곱해준다.
 */
public class BlackWhiteDarkFilter extends CameraFilter {
    private int program;

    public BlackWhiteDarkFilter(Context context) {
        super(context);

        // Build shaders
        program = MyGLUtils.buildProgram(context, R.raw.vertext, R.raw.black_white_dark);
    }

    @Override
    public void onDraw(int cameraTexId, int canvasWidth, int canvasHeight) {
        setupShaderInputs(program,
                new int[]{canvasWidth, canvasHeight},
                new int[]{cameraTexId},
                new int[][]{});
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
