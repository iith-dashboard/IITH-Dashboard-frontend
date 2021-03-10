package com.lambda.iith.dashboard.AcadNotifs;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lambda.iith.dashboard.MainActivity;
import com.lambda.iith.dashboard.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;

public class NotificationInitiator extends Worker {

    public NotificationInitiator(@NonNull Context context, @NonNull WorkerParameters workerParams){
        super(context, workerParams);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    @Override
    public Result doWork() {
        WorkManager.getInstance().cancelAllWorkByTag("ACADEVENTS");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String str = sharedPrefs.getString("AcadCalendar"," ");

        ICalendar ical = Biweekly.parse(str).first();
        VEvent event = ical.getEvents().get(0);
        System.out.println(event.getDateStart());
        List<VEvent> allEvents= ical.getEvents();
        allEvents.sort(new Comparator<VEvent>() {
            @Override
            public int compare(VEvent o1, VEvent o2) {
                Date d1 = o1.getDateStart().getValue();
                Date d2 = o2.getDateStart().getValue();
                return d1.compareTo(d2);
            }
        });

        //System.out.println(allEvents);
        Date now = new Date();

        while(now.after(allEvents.get(0).getDateEnd().getValue()))
        {//change this to include those events where we are in between.
            allEvents.remove(0);
        }
        /*
        for(Iterator<VEvent> iter = allEvents.iterator(); iter.hasNext();){
            VEvent temp  = iter.next();
            if(now.after(temp.getDateEnd().getValue()) || now.before(temp.getDateStart().getValue())) {
                allEvents.remove(temp);
            }
        }*/

        if(allEvents.isEmpty()) {
            System.out.println("No Events Scheduled Today");
            return Result.success();
        }

        for(int i=0;i<allEvents.size();i++){
            VEvent curr = allEvents.get(i);
            if(now.before(curr.getDateStart().getValue()))
                 continue;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MINUTE, 00);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            long diff =  cal.getTimeInMillis() - System.currentTimeMillis();
            if(diff<0)
                diff = 0;
            Data.Builder data = new Data.Builder().putString("Title",curr.getSummary().getValue()).putString("Content",curr.getSummary().getValue());
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .addTag("ACADEVENTS")
                    .setInputData(data.build())
                    .setInitialDelay(diff, TimeUnit.MILLISECONDS)
                    .build();
            WorkManager.getInstance()
                    .enqueue(oneTimeWorkRequest);
        }

        /*
        DateFormat df = new SimpleDateFormat("dd/mm/yyyy HH");
        Date nextEvent = allEvents.get(0).getDateEnd().getValue();
        String titleString =  allEvents.get(0).getSummary().getValue();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "AcadEventAlerts")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleString)
                .setContentText(df.format(nextEvent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        // notificationId is a unique int for each notification that you must define

        notificationManager.notify(1572, builder.build());
        */

        return Result.success();
    }
}