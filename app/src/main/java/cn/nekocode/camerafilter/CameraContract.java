package cn.nekocode.camerafilter;

import android.app.Activity;
import android.graphics.Bitmap;

/**
 * Created by yebonkim on 2017. 10. 28..
 * 카메라 화면과 함수 클래스 정의 인터페이스
 */

public interface CameraContract {
    //화면에 필요한 함수 정의
    interface View {
        void setCameraView(CameraRenderer renderer);
        void showLoadingDialog();
        void dismissLoadingDialog();
    }

    //데이터와 함께 필요한 함수 정의
    interface Presenter {
        void setView(Activity activity, View view);
        void getPermission();
        void setCameraView();
        void focusing();
        void capture(Bitmap bitmap);
        void setFilter(int filterId);
    }
}
