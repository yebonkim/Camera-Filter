package cn.nekocode.camerafilter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity implements CameraContract.View {
    private final static String[] PERMISSION_LIST = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @BindView(R.id.cameraView)
    TextureView cameraView;

    CameraPresenter presenter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermission();
        }

        presenter = new CameraPresenter(getBaseContext());
        presenter.setView(this);
        presenter.setCameraView();
    }

    /*
        캡쳐 버튼 클릭시 동작함수
        현재 화면에 표시되고 있는 뷰 이미지를 읽어 저장
     */
    @OnClick(R.id.captureBtn)
    public void capture() {
        presenter.capture(cameraView.getBitmap());
    }

    /*
        카메라 뷰 클릭시 동작함수
        터치된 대상에 포커싱
     */
    @OnClick(R.id.frameLayout)
    public void focus() {
        presenter.focusing();
    }

    /*
        카메라 뷰에 카메라 관련 소스 연결
     */
    @Override
    public void setCameraView(CameraRenderer renderer) {
        cameraView.setSurfaceTextureListener(renderer);
    }

    /*
        카메라 촬영 후 로딩 다이알로그 띄움
     */
    @Override
    public void showLoadingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.wait));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /*
        사진 저장 완료 후 다이알로그 취소
     */
    @Override
    public void dismissLoadingDialog() {
        progressDialog.dismiss();
    }

    @Override
    public void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    /*
            menu_filter.xml에 지정된 item들을 통해 메뉴 구현
         */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_filter, menu);
        return true;
    }

    /*
        메뉴 아이템 클릭시 해당 기능 실행
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int filterId = item.getItemId();

        presenter.setFilter(filterId);

        return true;
    }

    /*
        허용되지 않은 권한을 사용자에게 요청
     */
    private void checkAndRequestPermission() {
        int result;

        for (int i = 0; i < PERMISSION_LIST.length; i++) {
            result = ContextCompat.checkSelfPermission(this, PERMISSION_LIST[i]);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSION_LIST, 1);
                break;
            }
        }
    }
}