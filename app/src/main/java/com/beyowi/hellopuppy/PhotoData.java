package com.beyowi.hellopuppy;

import android.content.res.Configuration;

/**
 * Created by User on 11/03/2015.
 */
public class PhotoData {
    public Integer orientation;
    public Integer windowX;
    public Integer windowY;

    public String source;
    public String portraitSource;
    public String landscapeSource;
    public Integer portraitHeight = 0;
    public Integer portraitWidth = 0;
    public Integer landscapeHeight = 0;
    public Integer landscapeWidth = 0;

    public String owner;
    public String title;

    public PhotoData(Integer ori, Integer width, Integer height){
        orientation = ori;
        windowX = width;
        windowY = height;
    }

    public void getSources(String url, Integer width, Integer height){
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (((portraitWidth < width) && (width < windowX)) && ((portraitHeight < height) && (height < windowY))) {
                source = portraitSource = url;
                portraitWidth = width;
                portraitHeight = height;
            }
            if (((landscapeWidth < width) && (width < windowY)) && ((landscapeHeight < height) && (height < windowX))) {
                landscapeSource = url;
                landscapeWidth = width;
                landscapeHeight = height;
            }
        } else {
            if (((landscapeWidth < width) && (width < windowX)) && ((landscapeHeight < height) && (height < windowY))) {
                source = landscapeSource = url;
                landscapeWidth = width;
                landscapeHeight = height;
            }
            if (((portraitWidth < width) && (width < windowY)) && ((portraitHeight < height) && (height < windowX))) {
                portraitSource = url;
                portraitWidth = width;
                portraitHeight = height;
            }
        }
    }

    public void setCredits(String name, String text){
        owner = name;
        title = text;
    }
}
