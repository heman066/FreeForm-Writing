package com.freeform.writing.Adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freeform.writing.Model.GraphView;
import com.freeform.writing.R;
import com.github.mikephil.charting.charts.LineChart;

import java.util.List;

public class GraphViewAdapter extends ArrayAdapter<GraphView> {

    private Activity context;
    private List<GraphView> graphs;

    public GraphViewAdapter(Activity context,List<GraphView>graphs){
        super(context, R.layout.graph_layout,graphs);
        this.context=context;
        this.graphs=graphs;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        GraphView graphView = graphs.get(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.graph_layout, parent, false);
            holder.lineChart = (LineChart) convertView.findViewById(R.id.lineChart);
            // associate the holder with the view for later lookup
            convertView.setTag(holder);
        }
        else {
            // view already exists, get the holder instance from the view
            holder = (ViewHolder) convertView.getTag();
        }
        holder.lineChart.setData(graphView.getLineData());
        //lineChart.setOnChartGestureListener((OnChartGestureListener) this);
        //lineChart.setOnChartValueSelectedListener((OnChartValueSelectedListener) this);

        return convertView;
    }
    static class ViewHolder{
        LineChart lineChart;
    }
}
