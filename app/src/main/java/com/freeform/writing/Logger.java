package com.freeform.writing;

import android.os.Environment;
import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private FileWriter fileWriter = null;

    public Logger(){
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        try {
            String logDir = String.valueOf(Environment.getExternalStoragePublicDirectory("FreeForm-Writing/AppLog/" + "appLog-" + timeStamp + ".txt"));
            this.fileWriter = new FileWriter(logDir ,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String Tag,String logMessage){
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String logmsg= timeStamp + ":- " + logMessage;
        try {
            //System.out.println("LOG:" + logmsg);
            fileWriter.write(logmsg + "\n");
            fileWriter.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        //Log.d(Tag,logMessage);
    }
}
