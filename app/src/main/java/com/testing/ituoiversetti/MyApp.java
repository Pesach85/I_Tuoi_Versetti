package com.testing.ituoiversetti;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        PdfParser.init(this);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();


        OneTimeWorkRequest indexReq =
                new OneTimeWorkRequest.Builder(BibleIndexWorker.class).build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "bible_index",
                ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(BibleIndexWorker.class).build()
        );

    }
}



