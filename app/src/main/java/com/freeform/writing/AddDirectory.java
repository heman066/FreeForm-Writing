package com.freeform.writing;

import android.os.Environment;

import java.io.File;

public class AddDirectory {

    private static File freeFormWriting= Environment.getExternalStoragePublicDirectory("FreeForm-Writing");
    private static File graphs=Environment.getExternalStoragePublicDirectory("FreeForm-Writing/.FFWList");
    private static File indata=Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Input Data");
    private static File ouput=Environment.getExternalStoragePublicDirectory("FreeForm-Writing/Output");
    private static File log=Environment.getExternalStoragePublicDirectory("FreeForm-Writing/AppLog");

    public static void addDirectory(){
        if(!freeFormWriting.exists()) freeFormWriting.mkdir();
        if(!indata.exists()) indata.mkdir();
        if(!graphs.exists()) graphs.mkdir();
        if(!ouput.exists()) ouput.mkdir();
        if(!log.exists()) log.mkdir();
    }
}
