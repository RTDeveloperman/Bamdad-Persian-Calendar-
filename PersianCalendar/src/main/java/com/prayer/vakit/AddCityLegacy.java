/*
 * Copyright (c) 2016 Metin Kale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prayer.vakit;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.byagowi.persiancalendar.R;;
import com.prayer.BaseActivity;
import com.prayer.vakit.times.other.Cities;
import com.prayer.vakit.times.other.Source;

import java.util.List;

public class AddCityLegacy extends BaseActivity implements OnItemClickListener {

    private ListView mListView;
    private MyAdapter mAdapter;
    private String mSource;
    private String mCountry;
    private String mState;
    private String mCity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vakit_addcity_prayer);
        findViewById(R.id.search).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary));
            window.setNavigationBarColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary));
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        TextView legacy = (TextView) findViewById(R.id.legacySwitch);
        legacy.setText(R.string.newAddCity);
        legacy.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(AddCityLegacy.this, AddCity.class));

            }

        });

        mListView = (ListView) findViewById(R.id.listView);
        mListView.setFastScrollEnabled(true);
        mListView.setOnItemClickListener(this);
        mAdapter = new MyAdapter(this);
        mListView.setAdapter(mAdapter);

        get("", "", "", "");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    public void get(final String source, final String country, final String state, final String city) {
        Cities.list(source, country, state, city, new Cities.Callback() {
            @Override
            public void onResult(List result) {
                if (!result.isEmpty()) {
                    mSource = source;
                    mCountry = country;
                    mState = state;
                    mCity = city;
                    mAdapter.clear();
                    mAdapter.addAll(result);
                    mListView.scrollTo(0, 0);
                }
                if (result.size() == 1) {
                    onItemClick(mListView, null, 0, 0);
                }
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mCity != "") {
            get(mSource, mCountry, mState, "");
        } else if (mState != "") {
            get(mSource, mCountry, "", "");
        } else if (mCountry != "") {
            get(mSource, "", "", "");
        } else if (mSource != "") {
            get("", "", "", "");
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long index) {
        if (mAdapter.getFullText(pos).contains(";")) {
            Cities.Item i = new Cities.Item();
            i.city = mAdapter.getItem(pos);
            i.country = mCountry;
            String[] s = mAdapter.getFullText(pos).split(";");
            i.id = s[1];
            i.lat = Double.parseDouble(s[2]);
            i.lng = Double.parseDouble(s[3]);
            i.source = Source.valueOf(mSource);

            finish();
            return;
        }

        if (mSource == "") {
            get(mAdapter.getItem(pos), "", "", "");
        } else if (mCountry == "") {
            get(mSource, mAdapter.getItem(pos), "", "");
        } else if (mState == "") {
            get(mSource, mCountry, mAdapter.getItem(pos), "");
        } else if (mCity == "") {
            get(mSource, mCountry, mState, mAdapter.getItem(pos));
        } else {
            get(mSource, mCountry, mState, mCity);
        }
    }


    static class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, android.R.id.text1);

        }

        public String getFullText(int pos) {
            return super.getItem(pos);
        }

        @Override
        public String getItem(int pos) {
            String s = super.getItem(pos);
            if (s.contains(";")) {
                s = s.substring(0, s.indexOf(";"));
            }

            return s;
        }

    }

}
