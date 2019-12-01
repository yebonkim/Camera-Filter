package cn.nekocode.camerafilter;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity implements CameraContract.View {

    @BindView(R.id.cameraView)
    TextureView cameraView;

    CameraPresenter presenter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        presenter = new CameraPresenter();
        presenter.setView(this, this);
        presenter.getPermission();
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
}