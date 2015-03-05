package com.beyowi.hellopuppy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;


public class MainActivity extends ActionBarActivity {

    //DEBUG
    private static final Boolean FORCE_DOWNLOAD = Boolean.TRUE;

    // API constants
    private static final String API_KEY = "4bc55852a8f873978d26832b8cc4f5aa";
    private static final String LIST_QUERY_URL = "https://api.flickr.com/services/rest/?method=flickr.photos.search";
    private static final String GET_QUERY_URL = "https://api.flickr.com/services/rest/?method=flickr.photos.getSizes";
    private static final String SUBJECT = "puppy";
    private static final String MEDIA_TYPE = "photos";
    private static final String FORMAT = "&format=json&nojsoncallback=?";
    private static final String PER_PAGE = "10";

    //PICASSO
    private static final String PICASSO_CACHE = "picasso-cache";

    //Data storage
    private static final String PREFS = "photoInfo";
    private static final String PORTRAIT_SOURCE = "portraitSource";
    private static final String LANDSCAPE_SOURCE = "landscapeSource";
    SharedPreferences mSharedPreferences;

    ImageView imageMain;
    Point windowSize;
    AsyncHttpClient client;
    ProgressDialog mDialog;
    Integer orientation;
    Configuration config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get layout elements
        imageMain = (ImageView) findViewById(R.id.main_image);

        //Get screen sizes
        windowSize = new Point();
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            defaultDisplay.getSize(windowSize);
        } else {
            windowSize.x = defaultDisplay.getWidth();
            windowSize.y = defaultDisplay.getHeight();
        };

        //Get orientation
        config = getResources().getConfiguration();
        orientation = config.orientation;

        //Set progress dialog
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Wait, he is coming !");
        mDialog.setCancelable(false);

        //Set shared preferences
        mSharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        //Load image
        checkCache();
    }


    public void checkCache(){
        //Display progress
        mDialog.show();
        //Get saved sources
        String source;
        if (orientation == config.ORIENTATION_PORTRAIT){
            source = mSharedPreferences.getString(PORTRAIT_SOURCE, "");
        } else {
            source = mSharedPreferences.getString(LANDSCAPE_SOURCE, "");
        }
        if ((source.length() > 0)&&(!FORCE_DOWNLOAD)) {
            //Load image in cache
            displayPhoto(source);
        } else {
            //Check internet connection
            if (checkInternetAvailable()){
                //Get new image
                getPhotosList();
            } else {
                createNetErrorDialog();
            }
        };
    }

    public boolean checkInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    protected void createNetErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You need a network connection to get a new puppy. Please turn on mobile network or Wi-Fi in Settings.")
                .setTitle("Unable to connect")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(Settings.ACTION_SETTINGS);
                                startActivity(i);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MainActivity.this.finish();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void getPhotosList(){
        // Create a client to perform networking
        client = new AsyncHttpClient();

        /* TODO GET A RANDOM LIST */
        String urlString = LIST_QUERY_URL + "&api_key=" + API_KEY + "&media=" + MEDIA_TYPE + "&tags=" + SUBJECT +
                "&per_page=" + PER_PAGE + FORMAT;

        client.get(urlString, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                getPhotoSizes(jsonObject);
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                // Display a "Toast" message
                // to announce the failure
                Toast.makeText(getApplicationContext(), "Error: " + statusCode + " " + throwable.getMessage(), Toast.LENGTH_LONG).show();

                // Log error message
                // to help solve any problems
                Log.e("GET_PHOTO", statusCode + " " + throwable.getMessage());
            }
        });
    }

    public void getPhotoSizes(JSONObject PhotosObject){

        JSONObject PhotoObject = PhotosObject.optJSONObject("photos");
        JSONArray photoList = PhotoObject.optJSONArray("photo");

        Integer position = (int) Math.floor( Math.random() * photoList.length());
        JSONObject photo = photoList.optJSONObject(position);
        String photoId = photo.optString("id");

        String urlString = GET_QUERY_URL + "&api_key=" + API_KEY + "&photo_id=" + photoId + FORMAT;
        Log.d("GET PHOTO SIZE", urlString);
        // Have the client get a JSONArray of data
        // and define how to respond
        client.get(urlString,
                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        getPhoto(jsonObject);
                    }

                    @Override
                    public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                        // Display a "Toast" message
                        // to announce the failure
                        Toast.makeText(getApplicationContext(), "Error: " + statusCode + " " + throwable.getMessage(), Toast.LENGTH_LONG).show();

                        // Log error message
                        // to help solve any problems
                        Log.e("GET_PHOTO", statusCode + " " + throwable.getMessage());
                    }
                });
    }

    public void getPhoto(JSONObject photoObject){
        JSONObject jsonObject = photoObject.optJSONObject("sizes");
        JSONArray photoSizes = jsonObject.optJSONArray("size");

        String source = "";
        String portraitSource = "";
        String landscapeSource = "";

        for (int i=0; i < photoSizes.length();i++) {
            JSONObject photo = photoSizes.optJSONObject(i);
            Integer photoWidth = photo.optInt("width");
            Integer photoHeight = photo.optInt("height");

            if (orientation == config.ORIENTATION_PORTRAIT) {
                if ((photoWidth < windowSize.x) && (photoHeight < windowSize.y)) {
                    source = portraitSource = photo.optString("source");
                }
                ;
                if ((photoWidth < windowSize.y) && (photoHeight < windowSize.x)) {
                    landscapeSource = photo.optString("source");
                }
                ;
            } else {
                if ((photoWidth < windowSize.x) && (photoHeight < windowSize.y)) {
                    source = landscapeSource = photo.optString("source");
                }
                ;
                if ((photoWidth < windowSize.y) && (photoHeight < windowSize.x)) {
                    portraitSource = photo.optString("source");
                }
                ;
            }
        };
        Log.d("SCREENSIZE", windowSize.x + ", " + windowSize.y);
        Log.d("SOURCE_PHOTO", portraitSource + "," + landscapeSource);

        // Access the device's key-value storage and save sources
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PORTRAIT_SOURCE, portraitSource);
        editor.putString(LANDSCAPE_SOURCE, landscapeSource);
        editor.commit();

        displayPhoto(source);
    }

    public void displayPhoto(String source){
        Log.d("DISPLAY IMAGE SOURCE", source);
        Picasso picasso = Picasso.with(this);
        picasso.setDebugging(true);
        Picasso.with(this).load(source).into(imageMain, new Callback.EmptyCallback(){
            @Override public void onSuccess() {
                mDialog.dismiss();
            }
            @Override
            public void onError() {
                mDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void clearPreferences(){
        mSharedPreferences.edit().clear().commit();
    }

    public static void clearCache(Context context) {
        final File cache = new File(
                context.getApplicationContext().getCacheDir(),
                PICASSO_CACHE);
        if (cache.exists()) {
            deleteFolder(cache);
        }
    }

    private static void deleteFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles())
                deleteFolder(child);
        }
        fileOrDirectory.delete();
    }
}
