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

        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(PdfUpdateWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "pdf_update_nwt",
                ExistingPeriodicWorkPolicy.KEEP,
                req
        );
    }
}
