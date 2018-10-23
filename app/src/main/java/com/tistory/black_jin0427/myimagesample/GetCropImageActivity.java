package com.tistory.black_jin0427.myimagesample;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;
import com.tistory.black_jin0427.myimagesample.util.ImageResizeUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GetCropImageActivity extends AppCompatActivity {

    private static final String TAG = "blackjin";

    private static final int PICK_FROM_ALBUM = 1;
    private static final int PICK_FROM_CAMERA = 2;
    private static final int CROP_IMAGE = 3;

    private Boolean isCamera = false;
    private File tempFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_image);

        findViewById(R.id.btnGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToAlbum();
            }
        });

        findViewById(R.id.btnCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show();

            if(tempFile != null) {
                if(tempFile.exists()) {

                    if(tempFile.delete()) {
                        Log.e(TAG, tempFile.getAbsolutePath() + " 삭제 성공");
                        tempFile = null;
                    } else {
                        Log.e(TAG, "tempFile 삭제 실패");
                    }

                } else {
                    Log.e(TAG, "tempFile 존재하지 않음");
                }
            } else {
                Log.e(TAG, "tempFile is null");
            }

            return;
        }

        switch (requestCode) {
            case PICK_FROM_ALBUM: {

                Uri photoUri = data.getData();
                Log.d(TAG, "PICK_FROM_ALBUM photoUri : " + photoUri);

                cropImage(photoUri);

                break;
            }
            case PICK_FROM_CAMERA: {

                // 앨범에 있지만 카메라 에서는 data.getData()가 없음
                Uri photoUri = Uri.fromFile(tempFile);
                Log.d(TAG, "takePhoto photoUri : " + photoUri);

               cropImage(photoUri);

                break;
            }
            // Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            case CROP_IMAGE: {

                setImage();

                break;
            }
            // Build.VERSION.SDK_INT < Build.VERSION_CODES.N
            case Crop.REQUEST_CROP: {
                tempFile = new File(Crop.getOutput(data).getPath());
                setImage();
            }
        }
    }

    /**
     *  앨범에서 이미지 가져오기
     */
    private void goToAlbum() {
        isCamera = false;

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    /**
     *  카메라에서 이미지 가져오기
     */
    private void takePhoto() {
        isCamera = true;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            tempFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }
        if (tempFile != null) {

            /**
             *  안드로이드 OS 누가 버전 이후부터는 file:// URI 의 노출을 금지로 FileUriExposedException 발생
             *  Uri 를 FileProvider 도 감싸 주어야 합니다.
             *
             *  참고 자료 http://programmar.tistory.com/4 , http://programmar.tistory.com/5
             */
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {

                Uri photoUri = FileProvider.getUriForFile(this,
                        "com.tistory.black_jin0427.myimagesample.provider", tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, PICK_FROM_CAMERA);

            } else {

                Uri photoUri = Uri.fromFile(tempFile);
                Log.d(TAG, "takePhoto photoUri : " + photoUri);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, PICK_FROM_CAMERA);

            }
        }
    }

    /**
     *  Crop 기능
     */
    private void cropImage(Uri photoUri) {

        Log.d(TAG, "tempFile : " + tempFile);

        /**
         *  갤러리에서 선택한 경우에는 tempFile이 없으므로 새로 생성해줍니다.
         */
        if(tempFile == null) {
            try {
                tempFile = createImageFile();
            } catch (IOException e) {
                Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                finish();
                e.printStackTrace();
            }
        }

        //크롭에 후 저장할 Uri
        Uri savingUri = Uri.fromFile(tempFile);

        Crop.of(photoUri, savingUri).asSquare().start(this);
    }

    /**
     *  폴더 및 파일 만들기
     */
    private File createImageFile() throws IOException {

        // 이미지 파일 이름 ( blackJin_{시간}_ )
        String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
        String imageFileName = "blackJin_" + timeStamp + "_";

        // 이미지가 저장될 파일 이름 ( blackJin )
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/blackJin/");
        if (!storageDir.exists()) storageDir.mkdirs();

        // 빈 파일 생성
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        Log.d(TAG, "createImageFile : " + image.getAbsolutePath());

        return image;
    }

    /**
     *  tempFile 을 bitmap 으로 변환 후 ImageView 에 설정한다.
     */
    private void setImage() {

        ImageView imageView = findViewById(R.id.imageVeiew);

        ImageResizeUtils.resizeFile(tempFile, tempFile, 1280, isCamera);

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap originalBm = BitmapFactory.decodeFile(tempFile.getAbsolutePath(), options);
        Log.d(TAG, "setImage : " + tempFile.getAbsolutePath());

        imageView.setImageBitmap(originalBm);

        /**
         *  tempFile 사용 후 null 처리를 해줘야 합니다.
         *  (resultCode != RESULT_OK) 일 때 (tempFile != null)이면 해당 파일을 삭제하기 때문에
         *  기존에 데이터가 남아 있게 되면 원치 않은 삭제가 이뤄집니다.
         */
        tempFile = null;

    }

}
