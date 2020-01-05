package cn.nekocode.camerafilter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by yebonkim on 2017. 10. 30..
 */

public class CameraPresenter implements CameraContract.Presenter {
    private CameraContract.View view;
    private static final String FOLDER_NAME = "/filter_camera";

    CameraRenderer renderer;

    public CameraPresenter(Context context) {
        this.renderer = new CameraRenderer(context);
    }

    /*
        CameraActivity와 CameraPresenter와 CameraRenderer를 서로 연결
     */
    @Override
    public void setView(CameraContract.View view) {
        this.view = view;
    }

    /*
        카메라 뷰에 renderer 연결
     */
    @Override
    public void setCameraView() {
        view.setCameraView(renderer);
    }

    /*
        현재 선택된 필터 renderer에 전달
     */
    @Override
    public void setFilter(int filterId) {
        renderer.setSelectedFilter(filterId);
    }

    /*
        화면 focusing
     */
    @Override
    public void focusing() {
        renderer.focusing();
    }

    /*
        bitmap 이미지 파일을 png 형식의 byte 배열로 압축하여 변환후
        다이알로그를 띄운 사이에 파일 출력 함수 호출
     */
    @Override
    public void capture(Bitmap bitmap) {
        byte[] imgData;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        imgData = stream.toByteArray();

        view.showLoadingDialog();

        saveImage(imgData);
        view.dismissLoadingDialog();
    }

    /*
        filter_camera라는 하위 폴더 아래 이미지를 파일 출력
     */
    protected void saveImage(byte[] imgData) {
        FileOutputStream outStream = null;
        File outFile = null;

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + FOLDER_NAME);
        dir.mkdirs();

        String fileName = getNowTimeStr();
        outFile = new File(dir, fileName);

        try {
            outStream = new FileOutputStream(outFile);
            outStream.write(imgData);
            outStream.flush();
            outStream.close();
            view.refreshGallery(outFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        사진 중복 저장을 피하기 위한 초단위 파일 이름 생성 함수
     */
    protected String getNowTimeStr() {
        Calendar calendar = Calendar.getInstance();
        java.util.Date date = calendar.getTime();
        final String pattern = "yyyyMMddHHmmss";
        final String ext = ".png";

        String today = new SimpleDateFormat(pattern).format(date);

        return today+ext;
    }
}
