package com.beyowi.hellopuppy.tests;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageView;

import com.beyowi.hellopuppy.MainActivity;
import com.beyowi.hellopuppy.R;

import org.mockito.Mockito;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    Activity myActivity;
    ImageView image;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myActivity = getActivity();
        image = (ImageView) myActivity.findViewById(R.id.main_image);
    }

    @Override
    protected void tearDown() throws Exception{
        super.tearDown();
    }

    public void testPreconditions() {
        assertNotNull("myActivity is null", myActivity);
        assertNotNull("mFirstTestText is null", image);
    }

    public void testInternetAvailable(){
        // Setup
        final ConnectivityManager connectivityManager = Mockito.mock(ConnectivityManager.class);
        final NetworkInfo networkInfo = Mockito.mock( NetworkInfo.class );
        Mockito.when( connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        Mockito.when( networkInfo.isConnectedOrConnecting() ).thenReturn( true );
        // Exercise
        assertTrue("Connection is not true", getActivity().checkInternetAvailable(connectivityManager));
    }

    public void testInternetNotAvailable(){
        // Setup
        final ConnectivityManager connectivityManager = Mockito.mock(ConnectivityManager.class);
        final NetworkInfo networkInfo = Mockito.mock( NetworkInfo.class );
        Mockito.when( connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        Mockito.when( networkInfo.isConnectedOrConnecting() ).thenReturn( false );
        // Exercise
        assertFalse("Connection is not false", getActivity().checkInternetAvailable(connectivityManager));
    }

    public void testInternetNotAvailableNull(){
        // Setup
        final ConnectivityManager connectivityManager = Mockito.mock(ConnectivityManager.class);
        Mockito.when( connectivityManager.getActiveNetworkInfo()).thenReturn(null);
        // Exercise
        assertFalse("Connection is not false", getActivity().checkInternetAvailable(connectivityManager));
    }
}