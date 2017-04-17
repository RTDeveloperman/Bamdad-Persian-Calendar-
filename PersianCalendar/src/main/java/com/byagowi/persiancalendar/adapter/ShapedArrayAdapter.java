package com.byagowi.persiancalendar.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.byagowi.persiancalendar.util.Utils;

import java.util.List;

/**
 * Created by ebraminio on 2/17/16.
 */
public class ShapedArrayAdapter<T> extends ArrayAdapter<T> {
    private Utils utils;

    public ShapedArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
        utils = Utils.getInstance(context);
    }

    public ShapedArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, objects);
        utils = Utils.getInstance(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view instanceof TextView) utils.setFontShapeAndGravity((TextView) view);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        if (view instanceof TextView) utils.setFontShapeAndGravity((TextView) view);
        return view;
    }
}
