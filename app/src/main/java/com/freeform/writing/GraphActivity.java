package com.freeform.writing;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.freeform.writing.Adapter.GraphViewAdapter;
import com.freeform.writing.Model.DataSet;
import com.freeform.writing.Model.GraphView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class GraphActivity extends AppCompatActivity {

    private GraphViewAdapter adapter;
    ListView listView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_list);

        adapter = new GraphViewAdapter(this,new ArrayList<GraphView>());
        String fileName = getIntent().getExtras().getString("name");
        Log.e("GraphActivity",fileName);
        addAdapterValues(fileName);

        listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }

    private void addAdapterValues(String fileName) {
        File file = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/" + fileName);
        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String inputLine="";
            ArrayList<Entry> entriesX = new ArrayList<>();
            ArrayList<Entry> entriesY = new ArrayList<>();
            ArrayList<Entry> entriesZ = new ArrayList<>();
            String initialTime="";
            while ((inputLine = bufferedReader.readLine())!=null){
                //Split the data by ','
                String[] tokens = inputLine.split(",");
                if(initialTime=="")initialTime = tokens[0];
                Long time = Long.parseLong(tokens[0])-Long.parseLong(initialTime);
                double d= ((double)time)/1000.0;
                float f = (float)d;
                //Read the data
                DataSet dataSet = new DataSet("0",0,0,0);
                dataSet.setTimeStamp(tokens[0]);
                if(tokens.length>=2 && tokens[1].length()>0){
                    entriesX.add(new Entry(f,(float)Double.parseDouble(tokens[1])));
                }
                else entriesX.add(new Entry(f,0));
                if(tokens.length>=3 && tokens[2].length()>0){
                    entriesY.add(new Entry(f,(float)Double.parseDouble(tokens[2])));
                }
                else entriesY.add(new Entry(f,0));
                if(tokens.length>=4 && tokens[3].length()>0){
                    entriesZ.add(new Entry(f,(float)Double.parseDouble(tokens[3])));
                }
                else entriesZ.add(new Entry(f,0));
            }
            LineDataSet lineDataSetX = new LineDataSet(entriesX,"X-axis");
            lineDataSetX.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            lineDataSetX.setValueTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            lineDataSetX.setDrawCircles(false);
            GraphView graphViewX = new GraphView(new LineData(lineDataSetX));

            LineDataSet lineDataSetY = new LineDataSet(entriesY,"Y-axis");
            lineDataSetY.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            lineDataSetY.setValueTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            lineDataSetY.setDrawCircles(false);
            GraphView graphViewY = new GraphView(new LineData(lineDataSetY));

            LineDataSet lineDataSetZ = new LineDataSet(entriesZ,"Z-axis");
            lineDataSetZ.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            lineDataSetZ.setValueTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            lineDataSetZ.setDrawCircles(false);
            GraphView graphViewZ = new GraphView(new LineData(lineDataSetZ));
            adapter.add(graphViewX);
            adapter.add(graphViewY);
            adapter.add(graphViewZ);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
