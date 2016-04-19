package com.batzeesappstudio.pocketsignatureview;

import android.graphics.Paint;

/**
 * Created by Amalan Dhananjayan on 4/19/2016.
 */
public class PocketSignatureSettings {

    public Paint.Style STOKE_STYLE;
    public boolean STROKE_ANTI_ALIAS;
    public float STROKE_WIDTH;
    public int STROKE_COLOR;
    public Paint.Join STROKE_JOIN;
    public int PADDING_AROUND;
    public int BACKGROUND_COLOR;

    public boolean isSTROKE_ANTI_ALIAS() {
        return STROKE_ANTI_ALIAS;
    }

    public void setSTROKE_ANTI_ALIAS(boolean STROKE_ANTI_ALIAS) {
        this.STROKE_ANTI_ALIAS = STROKE_ANTI_ALIAS;
    }

    public Paint.Style getSTOKE_STYLE() {
        return STOKE_STYLE;
    }

    public void setSTOKE_STYLE(Paint.Style STOKE_STYLE) {
        this.STOKE_STYLE = STOKE_STYLE;
    }

    public float getSTROKE_WIDTH() {
        return STROKE_WIDTH;
    }

    public void setSTROKE_WIDTH(float STROKE_WIDTH) {
        this.STROKE_WIDTH = STROKE_WIDTH;
    }

    public int getSTROKE_COLOR() {
        return STROKE_COLOR;
    }

    public void setSTROKE_COLOR(int STROKE_COLOR) {
        this.STROKE_COLOR = STROKE_COLOR;
    }

    public Paint.Join getSTROKE_JOIN() {
        return STROKE_JOIN;
    }

    public void setSTROKE_JOIN(Paint.Join STROKE_JOIN) {
        this.STROKE_JOIN = STROKE_JOIN;
    }

    public int getPADDING_AROUND() {
        return PADDING_AROUND;
    }

    public void setPADDING_AROUND(int PADDING_AROUND) {
        this.PADDING_AROUND = PADDING_AROUND;
    }

    public int getBACKGROUND_COLOR() {
        return BACKGROUND_COLOR;
    }

    public void setBACKGROUND_COLOR(int BACKGROUND_COLOR) {
        this.BACKGROUND_COLOR = BACKGROUND_COLOR;
    }

}

