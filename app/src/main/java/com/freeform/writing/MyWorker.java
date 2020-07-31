package com.freeform.writing;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MyWorker extends Worker {
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(getApplicationContext()));
        }
        return Result.success();
    }
}
