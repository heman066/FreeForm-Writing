package com.freeform.writing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.freeform.writing.Adapter.FilesListAdapter;

import java.io.File;
import java.util.ArrayList;

public class FilesAvailableAcitivity extends AppCompatActivity {

    private FilesListAdapter adapter;
    ListView listView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_list);
        adapter = new FilesListAdapter(this,new ArrayList<String>());
        File fileList = Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList/");
        try {
            for(File file : fileList.listFiles()){
                adapter.add(file.getName());
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }

        listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(FilesAvailableAcitivity.this,GraphActivity.class);
                String name = (String) parent.getItemAtPosition(position);
                //Log.e("FilesAvailableActivity", name);
                intent.putExtra("name", name);
                startActivity(intent);
                //finish();
            }
        });
    }
}
