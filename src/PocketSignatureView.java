package com.batzeesappstudio.pocketsignatureview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Amalan Dhananjayan on 4/19/2016.
 */
public class PocketSignatureView extends View {

    private Context viewContext;
    private DisplayMetrics displayMetrics;
    private Path path;
    private Paint paint;
    private PocketSignatureSettings settings;
    private ArrayList<Path> pathContainer;
    private WindowManager windowmanager;
    private Canvas bitmapCanvas;
    private RectF signatureBoundRect;
    private Bitmap mBitmap;

    private int orientationState;
    private int strokeColor;
    private int backgroundColor;
    private int paddingSize;
    private int signatureStrokeWidth;

    private float lastTouchX;
    private float lastTouchY;
    private float screenWidth;
    private float previousWidth;
    private float widthRatio;
    private float scalePointX;
    private float scalePointY;
    private float newPositionOfX;
    private float newPositionOfY;

    private String signaturePath;
    private String signatureFilePath;
    private String vectorData;

    private boolean pathContainerOpen;
    private boolean pathContainerInUse;
    private boolean clearingCanvas;
    private boolean autoTouchtriggered;
    private boolean touchReleased;

    public PocketSignatureView(Context context) {
        super(context);

        viewContext = context;
        windowmanager = (WindowManager) viewContext.getSystemService(Context.WINDOW_SERVICE);

        initializeVariables();
        calculateRatioForOrientation();
        InitializelayoutProperties();
        InitializePaint();
    }
    private void initializeSignatureSettings(){
        settings.setSTOKE_STYLE(Paint.Style.STROKE);
        settings.setSTROKE_ANTI_ALIAS(true);
        settings.setSTROKE_WIDTH(5f);
        settings.setSTROKE_COLOR(Color.BLACK);
        settings.setSTROKE_JOIN(Paint.Join.ROUND);
        settings.setPADDING_AROUND(50);
        settings.setBACKGROUND_COLOR(Color.parseColor("#9dd6d6"));
    }

    private void initializeVariables() {
        settings = new PocketSignatureSettings();
        initializeSignatureSettings();
        pathContainer = new ArrayList<>();
        displayMetrics = new DisplayMetrics();
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
        path = new Path();
        paint = new Paint();
        vectorData = "";
        screenWidth = (displayMetrics.widthPixels);
        scalePointX = 0;
        scalePointY = 0;
        pathContainerOpen = true;
        pathContainerInUse = false;
        autoTouchtriggered = false;
        touchReleased = true;
        clearingCanvas = false;
        orientationState = getResources().getConfiguration().orientation;
        signatureStrokeWidth = (int) settings.getSTROKE_WIDTH();
        strokeColor = settings.getSTROKE_COLOR();
        backgroundColor = settings.getBACKGROUND_COLOR();
        paddingSize = settings.getPADDING_AROUND();
        setWillNotDraw(false);
        setSaveEnabled(true);
    }

    private void calculateRatioForOrientation() {
        if (orientationState == 2) {
            landscapeRatio();
        } else {
            widthRatio = 1;
        }
    }

    private void InitializelayoutProperties() {
        signatureBoundRect = new RectF(0, 0, screenWidth, screenWidth / 2);
        setBackgroundColor(backgroundColor);
    }

    private void InitializePaint() {
        paint.setAntiAlias(settings.isSTROKE_ANTI_ALIAS());
        paint.setColor(settings.getSTROKE_COLOR());
        paint.setStyle(settings.getSTOKE_STYLE());
        paint.setStrokeJoin(settings.getSTROKE_JOIN());
        paint.setStrokeWidth(settings.getSTROKE_WIDTH());
    }

    //Works only for JellyBeans and Up
    /*
    public void drawSignature(String imagePath) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inMutable = true;
        Bitmap drawnbitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
        //this.setBackground(new BitmapDrawable(getResources(), drawnbitmap));
    }
    */

    public void saveVectorImage(String imageFoldePath, String imageName) {

        signatureFilePath = imageFoldePath;
        String filename;

        if (!imageName.equals("")) {
            filename = imageName + ".svg";
        } else {
            filename = "signatureImage.svg";
        }

        if (checkAndCreateFolder(imageFoldePath)) {
            File dest = new File(imageFoldePath, filename);
            signaturePath = dest.getPath();
        } else {
            String tempPath = Environment.getExternalStorageDirectory()+"";
            File dest = new File(tempPath, filename);
            signaturePath = dest.getPath();
        }

        if (!vectorData.equals("")) {
            createSVG(vectorData);
        }
    }

    private void loadVectoreImage() {
        pathContainerOpen = false;
        pathContainerInUse = true;
        invalidate();
    }

    public void loadVectoreImage(String pathDataString) {
        if (pathDataString != null) {
            vectorData = pathDataString;
            createPathFromString();
            triggerTouch();
        }
    }

    public String getPathDataString() {
        return vectorData;
    }

    public void saveSignatureAsBitmap(String imageFilePath, String imageName) {
        View v = PocketSignatureView.this;
        if (mBitmap == null) {
            mBitmap = createBitmap(v);
        }

        String filename = imageName + ".png";
        File dest = new File(imageFilePath, filename);

        bitmapCanvas = new Canvas(mBitmap);
        try {
            FileOutputStream mFileOutStream = new FileOutputStream(dest.getPath());
            v.draw(bitmapCanvas);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
            mFileOutStream.flush();
            mFileOutStream.close();
            String url = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), mBitmap, "title", null);
            Log.v("PocketSignatureView_Log", "url: " + url);
        } catch (Exception e) {
            Log.v("PocketSignatureView_Log", e.toString());
        }
    }

    private Bitmap createBitmap(View vContent) {
        return mBitmap = Bitmap.createBitmap(vContent.getWidth(), vContent.getHeight(), Bitmap.Config.RGB_565);
    }

    public void clear() {
        pathContainerOpen = false;
        pathContainerInUse = true;
        clearingCanvas = true;
        vectorData = "";
        invalidate();
    }

    private void landscapeRatio() {
        screenWidth = (displayMetrics.widthPixels);
        previousWidth = (displayMetrics.heightPixels);
    }

    private boolean checkAndCreateFolder(String folderPath) {
        File dest = new File(folderPath);
        if (!dest.exists()) {
            if (dest.mkdir()) {
                Toast.makeText(viewContext, "Folder Created and Files Saved", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Log.d("PocketSignatureView_Log", "Could not Create the Folder");
                return false;
            }
        } else {
            return true;
        }
    }

    private void createPathFromString() {

        pathContainer = new ArrayList<>();
        String textTemp;

        //every path starts with 'M' so we split by them to get all the paths serparated
        String[] pathArray = vectorData.split("M");

        for (int x = 1; x < pathArray.length; x++) {
            if (x == pathArray.length - 1) {
                textTemp = pathArray[x];
            } else {
                textTemp = pathArray[x].substring(0, pathArray[x].length() - 60);
            }

            //every corrdinates in Path starts with 'L' so we split by them to get all the coordinates serparated
            String[] coOrdinateArray = textTemp.split(" L ");

            Path newPath = new Path();

            for (int y = 0; y < coOrdinateArray.length; y++) {

                //each coordinate's X and Y points are separated by empty spaces
                String[] xY = coOrdinateArray[y].split(" ");
                if (y == 0) {
                    newPath.moveTo(Float.parseFloat(xY[1]), Float.parseFloat(xY[2]));
                } else {
                    try {
                        newPath.lineTo(Float.parseFloat(xY[0]), Float.parseFloat(xY[1]));
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Log.d("PocketSignatureView_Log", ex.toString());
                    }
                }
            }
            pathContainer.add(newPath);
        }
        loadVectoreImage();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(strokeColor);

        if (orientationState == 2) {
            float ratioWidth = screenWidth / previousWidth;
            canvas.scale(ratioWidth, ratioWidth, scalePointX, scalePointY);
            canvas.translate(newPositionOfX, newPositionOfY);
        }
        if (pathContainerInUse) {

            if (clearingCanvas) {
                paint.setColor(backgroundColor);
                Log.d("PocketSignatureView_Log", "Path_Count - "+pathContainer.size() + "");
                path.reset();
                drawPathActions(canvas);
                pathContainer.clear();
                path.reset();
                clearingCanvas = false;
            } else {
                drawPathActions(canvas);
            }
            pathContainerInUse = false;
        }

        if (pathContainerOpen) {
            canvas.drawPath(path, paint);
            pathContainer.add(path);
            drawPathActions(canvas);
        }
    }

    private void drawPathActions(Canvas canvas) {
        if (touchReleased) {
            for (int x = 0; x < pathContainer.size(); x++) {
                path = pathContainer.get(x);
                canvas.drawPath(path, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        touchReleased = false;

        float eventX;
        float eventY;
        if (previousWidth != 0.0) {
            widthRatio = (screenWidth / previousWidth);
        }
        if (orientationState == 2) {

            float x = (event.getX()) / widthRatio;
            float y = (event.getY()) / widthRatio;

            eventX = x - newPositionOfX;
            eventY = y - newPositionOfY;

        } else {
            eventX = event.getX();
            eventY = event.getY();
        }

        pathContainerOpen = true;

        if (!autoTouchtriggered) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);

                    if (!autoTouchtriggered) {
                        if (vectorData.contains("M")) {
                            vectorData += " \" fill=\"none\" stroke=\"black\" stroke-width=\"1\"/>\n\"  <path d=\"";
                            vectorData += "M " + eventX + " " + eventY;
                        } else {
                            vectorData = "M " + eventX + " " + eventY;
                        }
                    }

                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetSignatureBoundRect(eventX, eventY);
                    int historySize = event.getHistorySize();

                    for (int i = 0; i < historySize; i++) {

                        if (orientationState == 2) {

                            float historicalX = (event.getHistoricalX(i) / widthRatio) - newPositionOfX;
                            float historicalY = (event.getHistoricalY(i) / widthRatio) - newPositionOfY;

                            expandSignatureBoundRect(historicalX, historicalY);
                            path.lineTo(historicalX, historicalY);
                            vectorData += " L " + event.getHistoricalX(i) / (widthRatio) + " " + event.getHistoricalY(i) / (widthRatio);
                        } else {
                            float historicalX = (event.getHistoricalX(i)) - newPositionOfX;
                            float historicalY = (event.getHistoricalY(i)) - newPositionOfY;

                            expandSignatureBoundRect(historicalX, historicalY);
                            path.lineTo(historicalX, historicalY);
                            vectorData += " L " + event.getHistoricalX(i) + " " + event.getHistoricalY(i);
                        }
                    }
                    path.lineTo(eventX, eventY);
                    vectorData += " L " + eventX + " " + eventY;

                    break;

                default:
                    return false;
            }
        } else {
            autoTouchtriggered = false;
        }

        touchReleased = true;

        invalidate((int) (signatureBoundRect.left - (signatureStrokeWidth / 2)),
                (int) (signatureBoundRect.top - (signatureStrokeWidth / 2)),
                (int) (signatureBoundRect.right + (signatureStrokeWidth / 2)),
                (int) (signatureBoundRect.bottom + (signatureStrokeWidth / 2)));

        lastTouchX = eventX;
        lastTouchY = eventY;

        return true;
    }

    private void createSVG(String pathData) {

        String svgStart = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + screenWidth + "\" height=\"" + screenWidth / 2 + "\" version=\"1.1\">\n" + "  <path d=\"";
        String svgEnd =  "\" fill=\"none\" stroke=\"black\" stroke-width=\"1\"/>\n" + "</svg>";
        String resultSVG = svgStart + pathData + svgEnd;

        try {
            File root = new File(signatureFilePath);
            if (!root.exists()) {
                root.mkdirs();
            }
            FileWriter writer = new FileWriter(signaturePath);
            writer.append(resultSVG);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void expandSignatureBoundRect(float historicalX, float historicalY) {
        if (historicalX < signatureBoundRect.left) {
            signatureBoundRect.left = historicalX;
        } else if (historicalX > signatureBoundRect.right) {
            signatureBoundRect.right = historicalX;
        }
        if (historicalY < signatureBoundRect.top) {
            signatureBoundRect.top = historicalY;
        } else if (historicalY > signatureBoundRect.bottom) {
            signatureBoundRect.bottom = historicalY;
        }
    }

    private void resetSignatureBoundRect(float eventX, float eventY) {
        signatureBoundRect.left = Math.min(lastTouchX, eventX);
        signatureBoundRect.right = Math.max(lastTouchX, eventX);
        signatureBoundRect.top = Math.min(lastTouchY, eventY);
        signatureBoundRect.bottom = Math.max(lastTouchY, eventY);
    }

    private void triggerTouch() {
        autoTouchtriggered = true;
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        float x = 0.0f;
        float y = 0.0f;

        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );
        this.dispatchTouchEvent(motionEvent);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putFloat("PreviousWidth", screenWidth);
        bundle.putString("Vectordata", vectorData);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable("superState");
            previousWidth = bundle.getFloat("PreviousWidth", screenWidth);
            orientationState = getResources().getConfiguration().orientation;
            if (orientationState == 2) {
                landscapeRatio();
            }
            vectorData = bundle.getString("Vectordata");
            loadVectoreImage(vectorData);
        }
        super.onRestoreInstanceState(state);
    }
}
