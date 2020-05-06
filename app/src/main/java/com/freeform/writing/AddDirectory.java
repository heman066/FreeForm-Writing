package com.freeform.writing;

import android.os.Environment;

import java.io.File;

public class AddDirectory {

    private static File freeFormWriting= Environment.getExternalStoragePublicDirectory("FreeForm-Writing");
    private static File test= Environment.getExternalStoragePublicDirectory("FreeForm-Writing/test");
    private static File graphs=Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList");

    public static void addDirectory(){
        if(!freeFormWriting.exists()) freeFormWriting.mkdir();
        if(!test.exists()) test.mkdir();
        if(!graphs.exists()) graphs.mkdir();
    }
}
