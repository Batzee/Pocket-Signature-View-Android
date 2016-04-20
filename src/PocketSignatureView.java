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
    private WindowManager windowManager;
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
    private String vectorStringData;

    private boolean pathContainerOpen;
    private boolean pathContainerInUse;
    private boolean clearingCanvas;
    private boolean autoTouchtriggered;
    private boolean touchReleased;

    public PocketSignatureView(Context context) {
        super(context);

        viewContext = context;
        windowManager = (WindowManager) viewContext.getSystemService(Context.WINDOW_SERVICE);

        initializeVariables();
        calculateRatioForOrientation();
        InitializelayoutProperties();
        InitializePaint();
    }

    private void initializeVariables() {
        settings = new PocketSignatureSettings();
        initializeSignatureSettings();
        pathContainer = new ArrayList<>();
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        path = new Path();
        paint = new Paint();
        vectorStringData = "";
        screenWidth = (displayMetrics.widthPixels);
        scalePointX = 0;
        scalePointY = 0;
        pathContainerOpen = true;
        touchReleased = true;
        autoTouchtriggered = false;
        pathContainerInUse = false;
        clearingCanvas = false;
        orientationState = getResources().getConfiguration().orientation;
        signatureStrokeWidth = (int) settings.getSTROKE_WIDTH();
        strokeColor = settings.getSTROKE_COLOR();
        backgroundColor = settings.getBACKGROUND_COLOR();
        paddingSize = settings.getPADDING_AROUND();
    }

    private void initializeSignatureSettings() {
        settings.setSTOKE_STYLE(Paint.Style.STROKE);
        settings.setSTROKE_ANTI_ALIAS(true);
        settings.setSTROKE_WIDTH(5f);
        settings.setSTROKE_COLOR(Color.BLACK);
        settings.setSTROKE_JOIN(Paint.Join.ROUND);
        settings.setPADDING_AROUND(50);
        settings.setBACKGROUND_COLOR(Color.parseColor("#9dd6d6"));
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
            String tempPath = Environment.getExternalStorageDirectory() + "";
            File dest = new File(tempPath, filename);
            signaturePath = dest.getPath();
        }

        if (!vectorStringData.equals("")) {
            createSVG(vectorStringData);
        } else {
            Log.v("PocketSignatureView_Log", "No Data to Draw");
        }
    }

    private void loadVectoreImage() {
        pathContainerOpen = false;
        pathContainerInUse = true;
        invalidate();
    }

    public void loadVectoreImage(String pathDataString) {
        if (pathDataString != null) {
            vectorStringData = pathDataString;
            createPathFromVectorString();
            triggerTouch();
        }
    }

    public String getPathDataString() {
        return vectorStringData;
    }

    public void saveSignatureAsBitmap(String imageFilePath, String imageName) {
        View view = PocketSignatureView.this;
        if (mBitmap == null) {
            mBitmap = createBitmap(view);
        }

        String filename = imageName + ".png";
        File destinationFile = new File(imageFilePath, filename);

        bitmapCanvas = new Canvas(mBitmap);
        try {
            FileOutputStream mFileOutStream = new FileOutputStream(destinationFile.getPath());
            view.draw(bitmapCanvas);
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
        vectorStringData = "";
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
                Toast.makeText(viewContext, "Folder Created", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Log.d("PocketSignatureView_Log", "Could not Create the Folder");
                return false;
            }
        } else {
            return true;
        }
    }

    private void createPathFromVectorString() {

        pathContainer = new ArrayList<>();
        String tempStringStore;

        //every path starts with 'M' so we split by them to get all the paths serparated
        String[] pathArray = vectorStringData.split("M");

        for (int x = 1; x < pathArray.length; x++) {
            if (x == pathArray.length - 1) {
                tempStringStore = pathArray[x];
            } else {
                tempStringStore = pathArray[x].substring(0, pathArray[x].length() - 60);
            }

            //every corrdinates in Path starts with 'L' so we split by them to get all the coordinates serparated
            String[] arrayOfCoOrdinates = tempStringStore.split(" L ");

            Path newPath = new Path();

            for (int y = 0; y < arrayOfCoOrdinates.length; y++) {

                //each coordinate's X and Y points are separated by empty spaces
                String[] xY = arrayOfCoOrdinates[y].split(" ");
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

        //On Landscape the draw has to be magnified
        if (orientationState == 2) {
            float ratioWidth = screenWidth / previousWidth;
            canvas.scale(ratioWidth, ratioWidth, scalePointX, scalePointY);
            canvas.translate(newPositionOfX, newPositionOfY);
        }

        if (pathContainerInUse) {
            if (clearingCanvas) {
                path.reset();
                pathContainer.clear();
                clearingCanvas = false;
            } else {
                drawAllPaths(canvas);
            }
            pathContainerInUse = false;
        }

        if (pathContainerOpen) {
            canvas.drawPath(path, paint);
            pathContainer.add(path);
            drawAllPaths(canvas);
        }
    }

    private void drawAllPaths(Canvas canvas) {
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
        }
        else {
            eventX = event.getX();
            eventY = event.getY();
        }

        pathContainerOpen = true;

        if (!autoTouchtriggered) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);

                    if (!autoTouchtriggered) {
                        if (vectorStringData.contains("M")) {
                            vectorStringData += " \" fill=\"none\" stroke=\"black\" stroke-width=\"1\"/>\n\"  <path d=\"";
                            vectorStringData += "M " + eventX + " " + eventY;
                        } else {
                            vectorStringData = "M " + eventX + " " + eventY;
                        }
                    }
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetSignatureBoundRect(eventX, eventY);
                    int historySize = event.getHistorySize();

                    float historicalX;
                    float historicalY;

                    for (int i = 0; i < historySize; i++) {

                        if (orientationState == 2) {

                            historicalX = (event.getHistoricalX(i) / widthRatio) - newPositionOfX;
                            historicalY = (event.getHistoricalY(i) / widthRatio) - newPositionOfY;

                            vectorStringData += " L " + event.getHistoricalX(i) / (widthRatio) + " " + event.getHistoricalY(i) / (widthRatio);
                        }
                        else {
                            historicalX = (event.getHistoricalX(i)) - newPositionOfX;
                            historicalY = (event.getHistoricalY(i)) - newPositionOfY;

                            vectorStringData += " L " + event.getHistoricalX(i) + " " + event.getHistoricalY(i);
                        }
                        expandSignatureBoundRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }

                    path.lineTo(eventX, eventY);
                    vectorStringData += " L " + eventX + " " + eventY;
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
        String svgEnd = "\" fill=\"none\" stroke=\"black\" stroke-width=\"1\"/>\n" + "</svg>";
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
        bundle.putString("Vectordata", vectorStringData);
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
            vectorStringData = bundle.getString("Vectordata");
            loadVectoreImage(vectorStringData);
        }
        super.onRestoreInstanceState(state);
    }
}
