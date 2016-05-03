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
import android.widget.Toast;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by Amalan Dhananjayan on 4/19/2016.
 * v0.1.5
 */
public class PocketSignatureView extends View {

    private Path path;
    private Paint paint;
    private ArrayList<Path> pathContainer;
    private Canvas bitmapCanvas;
    private RectF signatureBoundRect;
    private Bitmap mBitmap;

    //View Properties
    private int strokeColor;
    private int canvasColor;
    private int strokeWidth;
    public Paint.Style strokeStyle;
    public boolean strokeAntiAlias;
    public Paint.Join strokeJoin;

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

    String svgEnd = "\" fill=\"none\" stroke=\"black\" stroke-width=\"1\"/></svg>";

    public PocketSignatureView(Context context) {
        super(context);

        initializeVariables();
        calculateRatioForOrientation();
        InitializelayoutProperties();
        InitializePaint();
    }

    private void initializeVariables() {
        initializeSignatureSettings();
        pathContainer = new ArrayList<>();
        path = new Path();
        paint = new Paint();
        vectorStringData = "";
        screenWidth = (getResources().getDisplayMetrics().widthPixels);
        scalePointX = 0;
        scalePointY = 0;
        pathContainerOpen = true;
        touchReleased = true;
        autoTouchtriggered = false;
        pathContainerInUse = false;
        clearingCanvas = false;
    }

    private void initializeSignatureSettings() {

        strokeColor = Color.BLACK;
        canvasColor = Color.parseColor("#9dd6d6");
        strokeWidth = (int) 5f;
        strokeStyle = Paint.Style.STROKE;
        strokeAntiAlias = true;
        strokeJoin = Paint.Join.ROUND;
    }

    private void calculateRatioForOrientation() {
        if (getResources().getConfiguration().orientation == 2) {
            landscapeRatio();
        } else {
            widthRatio = 1;
        }
    }

    private void InitializelayoutProperties() {
        signatureBoundRect = new RectF(0, 0, screenWidth, screenWidth / 2);
        setBackgroundColor(canvasColor);
    }

    private void InitializePaint() {
        paint.setAntiAlias(strokeAntiAlias);
        paint.setColor(strokeColor);
        paint.setStyle(strokeStyle);
        paint.setStrokeJoin(strokeJoin);
        paint.setStrokeWidth(strokeWidth);
    }

    public void saveVectorImage(String imageFoldePath, String imageName) {

        signatureFilePath = imageFoldePath;
        String filename;

        if (imageName == null || imageName.isEmpty()) {
            filename = "signatureImage.svg";
        } else {
            filename = imageName + ".svg";
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
            mBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
        }

        bitmapCanvas = new Canvas(mBitmap);
        try {
            FileOutputStream mFileOutStream = new FileOutputStream(new File(imageFilePath, imageName + ".png").getPath());
            view.draw(bitmapCanvas);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
            mFileOutStream.flush();
            mFileOutStream.close();
            String url = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), mBitmap, "title", null);
            mBitmap.recycle();
            Log.v("PocketSignatureView_Log", "url: " + url);
        } catch (Exception e) {
            Log.v("PocketSignatureView_Log", e.toString());
        }
    }

    public void clear() {
        pathContainerOpen = false;
        pathContainerInUse = true;
        clearingCanvas = true;
        vectorStringData = "";
        invalidate();
    }

    private void landscapeRatio() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidth = (displayMetrics.widthPixels);
        previousWidth = (displayMetrics.heightPixels);
    }

    private boolean checkAndCreateFolder(String folderPath) {
        File dest = new File(folderPath);
        if (!dest.exists()) {
            if (dest.mkdir()) {
                Toast.makeText(getContext(), "Folder Created", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Log.d("PocketSignatureView_Log", "Could not Create the Folder");
                return false;
            }
        } else {
            return true;
        }
    }

    // TODO : use  A xml parser -> much safer
    private void createPathFromVectorString() {

        pathContainer = new ArrayList<>();
        String tempStringStore;

        //every path starts with 'M' so we split by them to get all the paths serparated
        //String[] pathArray = vectorStringData.split("M");

        List<String> pathArray =  new ArrayList<>();
        pathArray = parsedPathList(createModifiedString(vectorStringData));

        for (int x = 0; x < pathArray.size(); x++) {
            if (x == pathArray.size() - 1) {
                tempStringStore = pathArray.get(x);
            } else {
                tempStringStore = pathArray.get(x).substring(0, pathArray.get(x).length() - 60);
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

    private String createModifiedString(String toModifyString){
        String stringValue = toModifyString;

        String svgStart = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + screenWidth + "\" height=\"" + screenWidth / 2 + "\" version=\"1.1\">\n" + "  <path d=\"";

        if(!stringValue.contains("<svg")) {
            stringValue = svgStart + stringValue;
        }
        if(!stringValue.contains("</svg>")) {
            stringValue = stringValue + svgEnd;
        }
        else {
            stringValue = stringValue.replaceAll(svgEnd,"");
            stringValue = stringValue + svgEnd;
        }
        return  stringValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //On Landscape the draw has to be magnified
        if (getResources().getConfiguration().orientation == 2) {
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

        if (getResources().getConfiguration().orientation == 2) {
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
                            if(vectorStringData.contains(svgEnd)) {
                                vectorStringData = vectorStringData.replaceAll(svgEnd, "");
                            }
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
                        if (getResources().getConfiguration().orientation == 2) {
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

        invalidate((int) (signatureBoundRect.left - (strokeWidth / 2)),
                (int) (signatureBoundRect.top - (strokeWidth / 2)),
                (int) (signatureBoundRect.right + (strokeWidth / 2)),
                (int) (signatureBoundRect.bottom + (strokeWidth / 2)));

        lastTouchX = eventX;
        lastTouchY = eventY;

        return true;
    }

    private void createSVG(String pathData) {
        String svgStart = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + screenWidth + "\" height=\"" + screenWidth / 2 + "\" version=\"1.1\">\n" + "  <path d=\"";
        String resultSVG;
        if(pathData.contains("<svg")){
            resultSVG = pathData;
        }
        else{
            resultSVG = svgStart + pathData;
        }
        if(resultSVG.contains(svgEnd)){
            resultSVG.replaceAll(svgEnd,"");
            if(!resultSVG.contains(svgEnd)){
              resultSVG =resultSVG + svgEnd;
            }
        }
        else {

            resultSVG =resultSVG + svgEnd;
        }
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
        bundle.putFloat("previousWidth", screenWidth);
        bundle.putString("vectorStringData", vectorStringData);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable("superState");
            previousWidth = bundle.getFloat("previousWidth", screenWidth);
            if (getResources().getConfiguration().orientation == 2) {
                landscapeRatio();
            }
            vectorStringData = bundle.getString("vectorStringData");
            loadVectoreImage(vectorStringData);
        }
        super.onRestoreInstanceState(state);
    }

    private List<String> parsedPathList(String rawXml){
        List<String> pathList = null;

        vectorStringData = rawXml;
        BufferedReader br=new BufferedReader(new StringReader(vectorStringData));
        InputSource is=new InputSource(br);

        try {
            XMLParser parser = new XMLParser();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser sp = factory.newSAXParser();
            XMLReader reader = sp.getXMLReader();
            reader.setContentHandler(parser);
            reader.parse(is);

            pathList=parser.list;
        }
        catch (Exception ex){
            Log.d("XML Parser Exception", ex.toString());
        }

        return pathList;
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        paint.setColor(strokeColor);
    }

    public void setCanvasColor(int canvasColor) {
        this.canvasColor = canvasColor;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        paint.setStrokeWidth(strokeWidth);
    }

    public void setStrokeStyle(Paint.Style strokeStyle) {
        this.strokeStyle = strokeStyle;
        paint.setStyle(strokeStyle);
    }

    public void setStrokeAntiAlias(boolean strokeAntiAlias) {
        this.strokeAntiAlias = strokeAntiAlias;
        paint.setAntiAlias(strokeAntiAlias);
    }

    public void setStrokeJoin(Paint.Join strokeJoin) {
        this.strokeJoin = strokeJoin;
        paint.setStrokeJoin(strokeJoin);
    }
}
