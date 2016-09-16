package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * Created by jmorgan on 9/6/2016.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private String mForecastStr;
    static final String DETAIL_URI = "URI";
    private ShareActionProvider mShareActionProvider;
    private Uri mUri;

    private static final int DETAIL_LOADER = 0;
    private static final String[] DETAIL_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,

            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    static final int COL_LOCATION_SETTING = 10;

    private ImageView mIconView;
    private TextView mDateView;
    private TextView mDayNameView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private Compass mCompass;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
        }
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mDayNameView = (TextView) rootView.findViewById(R.id.detail_day_name_textview);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_temp_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_temp_textview);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon_imageview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mCompass = (Compass) rootView.findViewById(R.id.my_custom_view);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // Attach an intent to this ShareActionProvider.  You can update this at any time,
        // like when the user selects a new piece of data they might like to share.
        if (mForecastStr != null ) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                mForecastStr + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    public void onLocationChanged(String newLocation) {
        Uri uri = mUri;
        if (uri != null) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mUri == null) {
            return null;
        }
        return new CursorLoader(getActivity(), mUri, DETAIL_COLUMNS, null, null, null);
    }

    public void onLoaderReset(Loader loader) {  }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (!cursor.moveToFirst()) { return; }

        long dateInMillis = cursor.getLong(COL_WEATHER_DATE);
        String dayName = Utility.getDayName(getActivity(), dateInMillis);
        mDayNameView.setText(dayName);
        String date = Utility.getFormattedMonthDay(getActivity(), dateInMillis);
        mDateView.setText(date);

        boolean isMetric = Utility.isMetric(getActivity());

        String high = Utility.formatTemperature(
                getActivity(),
                cursor.getLong(COL_WEATHER_MAX_TEMP),
                isMetric);
        mHighTempView.setText(high);

        String low = Utility.formatTemperature(
                getActivity(),
                cursor.getLong(COL_WEATHER_MIN_TEMP),
                isMetric);
        mLowTempView.setText(low);

        int weatherId = cursor.getInt(COL_WEATHER_CONDITION_ID);
        int weatherResourceId = Utility.getWeatherConditionImage(weatherId, false, getActivity());
        mIconView.setImageResource(weatherResourceId);

        String weatherDescription =
                cursor.getString(COL_WEATHER_DESC);
        mDescriptionView.setText(weatherDescription);

        String humidity = getActivity().getString(R.string.format_humidity, cursor.getFloat(COL_WEATHER_HUMIDITY));
        mHumidityView.setText(humidity);

        float windDir = cursor.getFloat(COL_WEATHER_DEGREES);
        String wind = Utility.getFormattedWind(getActivity(), cursor.getFloat(COL_WEATHER_WIND_SPEED), windDir);
        mWindView.setText(wind);

        mCompass.update(windDir);

        String pressure = getActivity().getString(R.string.format_pressure, cursor.getFloat(COL_WEATHER_PRESSURE));
        mPressureView.setText(pressure);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }


}