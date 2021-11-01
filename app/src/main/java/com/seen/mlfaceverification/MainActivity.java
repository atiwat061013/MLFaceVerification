package com.seen.mlfaceverification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.faceverify.MLFaceTemplateResult;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzer;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzerFactory;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzerSetting;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    Button btnCompare;
    TextView resultTextView;
    MLFaceVerificationAnalyzer FaceVerificationAnalyzer;

    ImageView templatePreview;

    ImageView comparePreview;


    private Bitmap templateBitmap;
    private Bitmap compareBitmap;
    private Bitmap templateBitmapCopy;
    private Bitmap compareBitmapCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MLFaceVerificationAnalyzerSetting.Factory factory = new MLFaceVerificationAnalyzerSetting.Factory().setMaxFaceDetected(3);
        MLFaceVerificationAnalyzerSetting setting = factory.create();
        FaceVerificationAnalyzer = MLFaceVerificationAnalyzerFactory
                .getInstance()
                .getFaceVerificationAnalyzer(setting);

        templatePreview = this.findViewById(R.id.tempPreview);
        comparePreview = this.findViewById(R.id.compPreview);
        resultTextView = findViewById(R.id.edit_text);


        btnCompare = findViewById(R.id.btn_compare);
        btnCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                recycleBitmap(templateBitmap);
//                recycleBitmap(templateBitmapCopy);
                Bitmap bitmapTemplate = BitmapFactory.decodeResource(getResources(), R.drawable.facetem);
                ByteArrayOutputStream byteTemplateStream = new ByteArrayOutputStream();
                bitmapTemplate.compress(Bitmap.CompressFormat.JPEG, 100, byteTemplateStream);
                byte[] byteTemplateArray = byteTemplateStream.toByteArray();
                String baseTemplateString = Base64.encodeToString(byteTemplateArray,Base64.DEFAULT);

                byte[] decodedTemplateString = Base64.decode(baseTemplateString, Base64.DEFAULT);
                Bitmap decodedTemplateByte = BitmapFactory.decodeByteArray(decodedTemplateString, 0, decodedTemplateString.length);
//                templatePreview.setImageBitmap(decodedByte);
//                templateBitmap = decodedTemplateByte;
                templateBitmap = loadPic(decodedTemplateByte,templatePreview);
                templateBitmapCopy = templateBitmap.copy(Bitmap.Config.ARGB_8888, true);



                if (templateBitmap == null) {
                    return;
                }
                long startTemplateTime = System.currentTimeMillis();
                List<MLFaceTemplateResult> resultsTemplate = FaceVerificationAnalyzer.setTemplateFace(MLFrame.fromBitmap(templateBitmap));
                long endTimeTemplate = System.currentTimeMillis();
                StringBuilder sbTemplate = new StringBuilder();
                sbTemplate.append("##setTemplateFace|COST[");
                sbTemplate.append(endTimeTemplate - startTemplateTime);
                sbTemplate.append("]");
                if (resultsTemplate.isEmpty()) {
                    sbTemplate.append("Failure!");
                } else {
                    sbTemplate.append("Success!");
                }
                for (MLFaceTemplateResult template : resultsTemplate) {
                    int idTemplate = template.getTemplateId();
                    Rect location = template.getFaceInfo().getFaceRect();
                    Canvas canvas = new Canvas(templateBitmapCopy);
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Paint.Style.STROKE);// Not Filled
                    paint.setStrokeWidth((location.right - location.left) / 50f);  // Line width
                    canvas.drawRect(location, paint);// framed
                    templatePreview.setImageBitmap(templateBitmapCopy);
                    sbTemplate.append("|Face[");
                    sbTemplate.append(location);
                    sbTemplate.append("]ID[");
                    sbTemplate.append(idTemplate);
                    sbTemplate.append("]");
                }
                sbTemplate.append("\n");
                resultTextView.setText(sbTemplate.toString());


                ///compare
//                recycleBitmap(compareBitmap);
//                recycleBitmap(compareBitmapCopy);
                Bitmap bitmapCompare = BitmapFactory.decodeResource(getResources(), R.drawable.facecom);
                ByteArrayOutputStream byteCompareStream = new ByteArrayOutputStream();
                bitmapCompare.compress(Bitmap.CompressFormat.JPEG, 100, byteCompareStream);
                byte[] byteCompareArray = byteCompareStream.toByteArray();
                String baseCompareString = Base64.encodeToString(byteCompareArray,Base64.DEFAULT);

                byte[] decodedCompareString = Base64.decode(baseCompareString, Base64.DEFAULT);
                Bitmap decodedCompareByte = BitmapFactory.decodeByteArray(decodedCompareString, 0, decodedCompareString.length);
//                templatePreview.setImageBitmap(decodedByte);
                compareBitmap = loadPic(decodedCompareByte, comparePreview);
                compareBitmapCopy = compareBitmap.copy(Bitmap.Config.ARGB_8888, true);

                if (compareBitmap == null) {
                    return;
                }
                final long startCompareTime = System.currentTimeMillis();
                try {
                    Task<List<MLFaceVerificationResult>> task = FaceVerificationAnalyzer.asyncAnalyseFrame(MLFrame.fromBitmap(compareBitmap));
                    final StringBuilder sbCompare = new StringBuilder();
                    sbCompare.append("##getFaceSimilarity|");
                    task.addOnSuccessListener(new OnSuccessListener<List<MLFaceVerificationResult>>() {
                        @Override
                        public void onSuccess(List<MLFaceVerificationResult> mlCompareList) {
                            long endCompareTime = System.currentTimeMillis();
                            sbCompare.append("COST[");
                            sbCompare.append(endCompareTime - startCompareTime);
                            sbCompare.append("]|Success!");
                            for (MLFaceVerificationResult template : mlCompareList) {
                                Rect location = template.getFaceInfo().getFaceRect();
                                Canvas canvas = new Canvas(compareBitmapCopy);
                                Paint paint = new Paint();
                                paint.setColor(Color.RED);
                                paint.setStyle(Paint.Style.STROKE);// Not Filled
                                paint.setStrokeWidth((location.right - location.left) / 50f);  // Line width
                                canvas.drawRect(location, paint);// framed
                                int id = template.getTemplateId();
                                float similarity = template.getSimilarity();
                                comparePreview.setImageBitmap(compareBitmapCopy);
                                sbCompare.append("|Face[");
                                sbCompare.append(location);
                                sbCompare.append("]Id[");
                                sbCompare.append(id);
                                sbCompare.append("]Similarity[");
                                sbCompare.append(similarity);
                                sbCompare.append("]");
                            }
                            sbCompare.append("\n");
                            resultTextView.append(sbCompare.toString());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            long endCompareTime = System.currentTimeMillis();
                            sbCompare.append("COST[");
                            sbCompare.append(endCompareTime - startCompareTime);
                            sbCompare.append("]|Failure!");
                            if (e instanceof MLException) {
                                MLException mlException = (MLException) e;
                                // Obtain error codes. Developers can process the error codes and display differentiated messages based on the error codes.
                                int errorCode = mlException.getErrCode();
                                // Obtain error information. Developers can quickly locate faults based on the error code.
                                String errorMessage = mlException.getMessage();
                                sbCompare.append("|ErrorCode[");
                                sbCompare.append(errorCode);
                                sbCompare.append("]Msg[");
                                sbCompare.append(errorMessage);
                                sbCompare.append("]");
                            } else {
                                sbCompare.append("|Error[");
                                sbCompare.append(e.getMessage());
                                sbCompare.append("]");
                            }
                            sbCompare.append("\n");
                            resultTextView.append(sbCompare.toString());
                        }
                    });
                }catch (RuntimeException e){
                    Log.e(TAG,"Set the image containing the face for comparison.");
                }
            }
        });

    }

    public static void recycleBitmap(Bitmap... bitmaps) {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }

    private Bitmap loadPic(Bitmap picBitmap, ImageView view) {
        Log.d("loadPicURI", String.valueOf(picBitmap));
        Bitmap pic = null;
        pic = loadFromPath( picBitmap, ((View) view.getParent()).getWidth(),
                ((View) view.getParent()).getHeight()).copy(Bitmap.Config.ARGB_8888, true);

//        pic = loadFromPath( picBitmap, 720,
//                1280);

        if (pic == null) {
//            Toast.makeText(this.getApplicationContext(), R.string.please_select_picture, Toast.LENGTH_SHORT).show();
        }
        view.setImageBitmap(pic);
        return pic;
    }

    public static Bitmap loadFromPath(Bitmap picBitmap, int width, int height) {
        Bitmap bitmap = zoomImage(picBitmap, width, height);

        return rotateBitmap(bitmap, 0);
    }

    private static Bitmap zoomImage(Bitmap imageBitmap, int targetWidth, int maxHeight) {
        float scaleFactor =
                Math.max(
                        (float) imageBitmap.getWidth() / (float) targetWidth,
                        (float) imageBitmap.getHeight() / (float) maxHeight);
        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(
                        imageBitmap,
                        (int) (imageBitmap.getWidth() / scaleFactor),
                        (int) (imageBitmap.getHeight() / scaleFactor),
                        true);

        return resizedBitmap;
    }

    public static int getRotationAngle(String path) {
        int rotation = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get rotation: " + e.getMessage());
        }
        return rotation;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap result = null;
        try {
            result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Failed to rotate bitmap: " + e.getMessage());
        }
        if (result == null) {
            return bitmap;
        }
        return result;
    }

    public static File bitmapToFile(Bitmap bitmap, String fileNameToSave) { // File name like "image.png"
        //create a file to write bitmap data
        File file = null;
        try {
            file = new File(Environment.getExternalStorageDirectory() + File.separator + fileNameToSave);
            file.createNewFile();

//Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 , bos); // YOU can also save it in JPEG
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            return file;
        }catch (Exception e){
            e.printStackTrace();
            return file; // it will return null
        }
    }

    public static String getImagePath(Activity activity, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(columnIndex);
    }







}