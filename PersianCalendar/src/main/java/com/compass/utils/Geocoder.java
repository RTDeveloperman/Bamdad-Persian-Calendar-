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

package com.compass.utils;

import com.compass.App;
import com.compass.compass.Main;
import com.compass.settings.Prefs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Geocoder {

    public static List<Address> from(String address, int limit) {
        List<Address> geo = new ArrayList<>();
        Locale locale = new Locale(Prefs.getLanguage());
        android.location.Geocoder geocoder = new android.location.Geocoder(Main.getContext(), locale);
        try {
            List<android.location.Address> resp;
            if (address.contains(";")) {
                try {
                    double lat = Double.parseDouble(address.substring(0, address.indexOf(";")));
                    double lng = Double.parseDouble(address.substring(address.indexOf(";") + 1));
                    resp = geocoder.getFromLocation(lat, lng, limit);
                } catch (NumberFormatException ignore) {
                    resp = geocoder.getFromLocationName(address, limit);
                }
            } else {
                resp = geocoder.getFromLocationName(address, limit);
            }
            for (android.location.Address a : resp) {
                Address g = new Address();
                g.lat = a.getLatitude();
                g.lng = a.getLongitude();
                g.country = a.getCountryName();
                g.state = a.getAdminArea();
                g.city = a.getLocality();
                if (g.city == null) {
                    g.city = a.getSubAdminArea();
                }
                if (g.city == null) {
                    g.city = a.getFeatureName();
                }
                geo.add(g);
            }
        } catch (IOException e) {
        }
        return geo;
    }


    public static class Address {
        public String city;
        public String state;
        public String country;
        public double lat;
        public double lng;
    }

}