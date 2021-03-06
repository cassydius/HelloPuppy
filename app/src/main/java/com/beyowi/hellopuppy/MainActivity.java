package com.beyowi.hellopuppy;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;


public class MainActivity extends ActionBarActivity {

    //DEBUG
    private static final Boolean FORCE_DOWNLOAD = Boolean.FALSE;

    // API constants
    private static final String API_KEY = "1e38f39fc6b0c731ec60ac1753325313";
    private static final String LIST_QUERY_URL = "https://api.flickr.com/services/rest/?method=flickr.photos.search";
    private static final String GROUP_ID = "26602919@N00";
    private static final String MEDIA_TYPE = "photos";
    private static final String FORMAT = "&format=json&nojsoncallback=?";
    private static final String PER_PAGE = "100";
    private static final String EXTRAS = "&extras=owner_name,description,url_sq,url_t,url_s,url_q,url_m,url_n,url_z,url_c,url_l,url_o";

    // API keys naming convention
    private static final String URL_PREFIX = "url";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";

    //PICASSO
    private static final String PICASSO_CACHE = "picasso-cache";

    //Data storage
    private static final String PREFS = "photoInfo";
    private static final String PORTRAIT_SOURCE = "portraitSource";
    private static final String LANDSCAPE_SOURCE = "landscapeSource";
    private static final String RENEWAL_DATE = "renewalDate";
    private static final String OWNER = "ownername";
    private static final String TITLE = "title";
    private static final String REFRESH_TIME_PREF = "notification_refresh_time";
    private static final String NOTIFICATION_NEW_PICTURE = "notifications_new_picture";
    SharedPreferences mSharedPreferences;

    //Other constants
    private static final Integer VALIDITY_DAY = 1;
    private static final String DEFAULT_REFRESH_TIME = "9:00";

    ImageView imageMain;
    Point windowSize;
    AsyncHttpClient client;
    ProgressDialog mDialog;
    Integer orientation;
    Configuration config;
    ConnectivityManager connecManager;
    Calendar cal;
    SharedPreferences defaultSharedPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    Tracker tracker;

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
        }

        //Get orientation
        config = getResources().getConfiguration();
        orientation = config.orientation;

        //Set progress dialog
        mDialog = new ProgressDialog(this);
        mDialog.setMessage(getString(R.string.loading_title));
        mDialog.setCancelable(false);

        //Init tracker analytics
        tracker = ((GlobalApp) this.getApplication()).getTracker(GlobalApp.TrackerName.APP_TRACKER);

        //Set shared preferences
        mSharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(REFRESH_TIME_PREF)){
                    //Track requests
                    tracker.send(new HitBuilders.ScreenViewBuilder()
                            .setCustomDimension(1,sharedPreferences.getString(key, DEFAULT_REFRESH_TIME))
                            .build());
                    setNotification();

                } else if (key.equals(NOTIFICATION_NEW_PICTURE)){
                    Boolean notification = sharedPreferences.getBoolean(key, true);
                    if (notification){
                        setNotification();
                    } else {
                        cancelAlarm();
                    }
                }

            }
        };

        defaultSharedPrefs.registerOnSharedPreferenceChangeListener(listener);

        connecManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cal = Calendar.getInstance();

        //Load image
        checkCache();
    }


    public void checkCache(){
        //Display progress
        mDialog.show();
        //Get saved sources
        String source;
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            source = mSharedPreferences.getString(PORTRAIT_SOURCE, "");
        } else {
            source = mSharedPreferences.getString(LANDSCAPE_SOURCE, "");
        }
        //Get renewal date
        long longDate = mSharedPreferences.getLong(RENEWAL_DATE, 0L);
        Date renewalDate = new Date(longDate);
        //Get now date
        Date now = cal.getTime();
        if ((source.length() > 0) && (now.before(renewalDate)) && (!FORCE_DOWNLOAD)) {
            //Load image in cache
            displayPhoto(source);
        } else {
            //Clear old datas
            clearCache(getBaseContext());
            clearPreferences();
            //Check internet connection
            if (checkInternetAvailable(connecManager)){
                //Get new image
                getPhotosList();
            } else {
                createNetErrorDialog();
            }
        }
    }

    public boolean checkInternetAvailable(ConnectivityManager cm) {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    protected void createNetErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.alert_connection_text))
                .setTitle(getString(R.string.alert_connection_title))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.settings),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(Settings.ACTION_SETTINGS);
                                startActivity(i);
                            }
                        }
                )
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MainActivity.this.finish();
                            }
                        }
                )
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        MainActivity.this.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void getPhotosList(){
        //Track requests
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("API Connection")
                .setAction("Request")
                .setLabel("Photos list")
                .setValue(1)
                .build());

        // Create a client to perform networking
        client = new AsyncHttpClient();

        /* TODO GET A RANDOM LIST */
        String urlString = LIST_QUERY_URL + "&api_key=" + API_KEY + "&media=" + MEDIA_TYPE + "&group_id=" + GROUP_ID +
                "&per_page=" + PER_PAGE + EXTRAS + FORMAT;

        client.get(urlString, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (jsonObject.has("stat") && (jsonObject.optString("stat").contains("fail"))) {
                    //Track API errors
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("API Connection")
                            .setAction("Get failed")
                            .setLabel("Photos list")
                            .setValue(1)
                            .build());
                    //Throw API error alert
                    String message = getString(R.string.api_error_message) + jsonObject.optString("message");
                    displayErrorAlert(getString(R.string.alert_api_title), message);
                } else {
                    //Track success
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("API Connection")
                            .setAction("Get success")
                            .setLabel("Photos list")
                            .setValue(1)
                            .build());
                    //Select a photo and get the correct sources
                    PhotoData photoObj = getPhoto(jsonObject);
                    setNotification();
                    savePreferences(photoObj);
                    displayPhoto(photoObj.source);
                }
            }

            @Override
            public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                displayErrorAlert(getString(R.string.alert_api_title), throwable.getMessage());
            }
        });
    }

    public PhotoData getPhoto(JSONObject PhotosObject){
        PhotoData photoObj = new PhotoData(orientation, windowSize.x, windowSize.y);
        try {
            JSONObject PhotoObject = PhotosObject.optJSONObject("photos");
            JSONArray photoList = PhotoObject.optJSONArray("photo");

            if (photoList.length() == 0) {
                displayErrorAlert(getString(R.string.alert_api_title), getString(R.string.api_result_error));
            } else {
                Integer position = (int) Math.floor(Math.random() * photoList.length());
                JSONObject photo = photoList.optJSONObject(position);

                Iterator<?> keys = photo.keys();
                photoObj.setCredits(photo.optString("ownername", ""), photo.optString("title", ""));

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.startsWith(URL_PREFIX)) {
                        String suffix = key.replace(URL_PREFIX, "");
                        String url = photo.optString(key, "");
                        Integer height = photo.optInt(HEIGHT + suffix, 0);
                        Integer width = photo.optInt(WIDTH + suffix, 0);
                        photoObj.getSources(url, width, height);
                    }
                }
            }
        }
        catch (NullPointerException jsonError){
            displayErrorAlert(getString(R.string.alert_api_title), getString(R.string.api_format_error));
        }
        return photoObj;
    }

    public void savePreferences(PhotoData photo){
        // Access the device's key-value storage and save sources
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PORTRAIT_SOURCE, photo.portraitSource);
        editor.putString(LANDSCAPE_SOURCE, photo.landscapeSource);
        editor.putString(OWNER, photo.owner);
        editor.putString(TITLE, photo.title);
        editor.apply();
    }

    private void setNotification(){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String refreshTime = prefs.getString(REFRESH_TIME_PREF, DEFAULT_REFRESH_TIME);
        String[] refreshdetails = refreshTime.split(":");

        //Get renewal Date
        Calendar refreshCalendar = Calendar.getInstance();
        refreshCalendar.add(Calendar.DATE, VALIDITY_DAY);
        refreshCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(refreshdetails[0]));
        refreshCalendar.set(Calendar.MINUTE, Integer.parseInt(refreshdetails[1]));
        Date renewalDate = refreshCalendar.getTime();

        if (prefs.getBoolean(NOTIFICATION_NEW_PICTURE, true)){startAlarm(renewalDate);}

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(RENEWAL_DATE, renewalDate.getTime());
        editor.apply();
    }

    private void startAlarm(Date renewalDate) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar =  Calendar.getInstance();
        calendar.setTime(renewalDate);
        long when = calendar.getTimeInMillis();         // notification time
        Intent intent = new Intent(this, ReminderService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC, when, pendingIntent);
    }

    private void cancelAlarm(){
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.cancel(pendingIntent);
    }

    public void displayPhoto(String source){
        Picasso.with(this).load(source).into(imageMain, new Callback.EmptyCallback(){
            @Override public void onSuccess() {
                mDialog.dismiss();
            }
            @Override
            public void onError() {
                clearPreferences();
                displayErrorAlert(getString(R.string.picasso_error_title), getString(R.string.picasso_error_text));
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
        switch( item.getItemId()) {
            case R.id.action_photo_credits:
                String owner = mSharedPreferences.getString(OWNER, "");
                String title = mSharedPreferences.getString(TITLE, "");
                String message = getString(R.string.author) + owner + "\n" + getString(R.string.title) + title;
                displayInfoAlert(getString(R.string.photo_credits), message);
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_disclaimer:
                displayInfoAlert(getString(R.string.disclaimer), getString(R.string.disclaimer_text));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void displayInfoAlert(String title, String message){
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    public void displayErrorAlert(String title, String message){
        mDialog.dismiss();
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.quit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        MainActivity.this.finish();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    public void clearPreferences(){
        mSharedPreferences.edit().clear().apply();
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