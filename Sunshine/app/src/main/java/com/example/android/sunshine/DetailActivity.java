/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.data.WeatherContract.WeatherEntry;


public class DetailActivity extends Activity {

    public static final String DATE_KEY = "forecast_date";
    private static final String LOCATION_KEY = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderCallbacks<Cursor> {

        public static final String DATE_KEY = "forecast_date";
        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
        private static final int DETAIL_LOADER = 0;
        private static final String[] FORECAST_COLUMNS = {
                WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
                WeatherEntry.COLUMN_DATETEXT,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP,
        };
        private ShareActionProvider mShareActionProvider;
        private String mLocation;
        private String mForecast;

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString(LOCATION_KEY, mLocation);
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mLocation != null &&
                    !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
                getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_detail, container, false);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_detail, menu);

            MenuItem item = menu.findItem(R.id.action_share);

            mShareActionProvider = (ShareActionProvider) item.getActionProvider();

            setShareIntent(createShareForecastIntent());
        }

        private void setShareIntent(Intent shareIntent) {
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(shareIntent);
            } else {
                Log.d(LOG_TAG, "Share Action Provider is null?");
            }
        }

        private Intent createShareForecastIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
            return shareIntent;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            if (savedInstanceState != null) {
                mLocation = savedInstanceState.getString(LOCATION_KEY);
            }
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.v(LOG_TAG, "In onCreateLoader");
            Intent intent = getActivity().getIntent();
            if (intent == null || !intent.hasExtra(DATE_KEY)) {
                return null;
            }
            String forecastDate = intent.getStringExtra(DATE_KEY);

            // Sort order:  Ascending, by date.
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

            mLocation = Utility.getPreferredLocation(getActivity());
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    mLocation, forecastDate);
            Log.v(LOG_TAG, weatherForLocationUri.toString());

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    weatherForLocationUri,
                    FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.v(LOG_TAG, "In onLoadFinished");
            if (!data.moveToFirst()) {
                return;
            }

            String dateString = Utility.formatDate(
                    data.getString(data.getColumnIndex(WeatherEntry.COLUMN_DATETEXT)));
            ((TextView) getView().findViewById(R.id.detail_date_textview))
                    .setText(dateString);

            String weatherDescription =
                    data.getString(data.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC));
            ((TextView) getView().findViewById(R.id.detail_forecast_textview))
                    .setText(weatherDescription);

            boolean isMetric = Utility.isMetric(getActivity());

            String high = Utility.formatTemperature(
                    data.getDouble(data.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP)), isMetric);
            ((TextView) getView().findViewById(R.id.detail_high_textview)).setText(high);

            String low = Utility.formatTemperature(
                    data.getDouble(data.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP)), isMetric);
            ((TextView) getView().findViewById(R.id.detail_low_textview)).setText(low);

            // We still need this for the share intent
            mForecast = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);

            Log.v(LOG_TAG, "Forecast String: " + mForecast);

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }
}