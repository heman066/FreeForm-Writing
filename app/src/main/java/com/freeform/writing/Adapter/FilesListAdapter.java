package com.freeform.writing.Adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freeform.writing.R;

import java.util.List;

public class FilesListAdapter extends ArrayAdapter<String> {
    private Activity context;
    private List<String> names;

    public FilesListAdapter(Activity context, List<String>names){
        super(context, R.layout.file_layout,names);
        this.context=context;
        this.names=names;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String nam = names.get(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.file_layout, parent, false);
            holder.name = (TextView) convertView.findViewById(R.id.fileName);
            // associate the holder with the view for later lookup
            convertView.setTag(holder);
        }
        else {
            // view already exists, get the holder instance from the view
            holder = (ViewHolder) convertView.getTag();
        }
        holder.name.setText(nam);

        return convertView;
    }
    static class ViewHolder{
        TextView name;
    }
}
