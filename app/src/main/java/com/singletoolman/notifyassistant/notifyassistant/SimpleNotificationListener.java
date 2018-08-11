package com.singletoolman.notifyassistant.notifyassistant;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ai.olami.android.tts.ITtsPlayerListener;
import ai.olami.android.tts.TtsPlayer;
import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Created by EasonChang on 2018/7/26.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SimpleNotificationListener extends NotificationListenerService{
    private String TAG = "service";
    private boolean Debug = false;
    private String applicationName, notifyTtitle, notifyText, notifyDateTime;
    private SharedPreferences mySharePreference ;
    private SharedPreferences.Editor editor ;
    private ArrayList<String> notifyInfoBackup = new ArrayList<>();
    // Write a message to the database
    //FirebaseDatabase database = FirebaseDatabase.getInstance();
    //DatabaseReference myRef = database.getReference(Build.DEVICE);



    private float mSpeed = 1.0f;
    @Override
    public void onCreate() {
        super.onCreate();
        toggleNotificationListenerService();
        mySharePreference = getSharedPreferences("UserSetting",0);
        editor = mySharePreference.edit();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
       applicationName = getAppName(sbn);
       notifyDateTime = getNowDateAndTime();
        Notification mNotification=sbn.getNotification();

        if (mNotification!=null){
            try{
                if (Debug){
                    Log.e(TAG,"Bundle: "+mNotification.extras);
                }
                Bundle extras = mNotification.extras;
                notifyTtitle  = String.valueOf(extras.get("android.title"));
                notifyText = String.valueOf(extras.get("android.text"));
                NOTIFY notifyInfo = new NOTIFY(applicationName,notifyTtitle,notifyDateTime,notifyText);
                //myRef.push().setValue(notifyInfo);
                if(mySharePreference.getBoolean("clearNotify",false)){
                    if (mySharePreference.getString("notifyBackupMessage","").equals("")){
                        notifyInfoBackup.clear();
                    }
                    notifyInfoBackup.add(0,applicationName+" "+notifyTtitle+"說: "+notifyText+"\n時間: "+notifyDateTime);
                    String jsonStr = new JSONArray(notifyInfoBackup).toString();
                    editor.putString("notifyBackupMessage",jsonStr);
                    editor.commit();
                    if(notifyInfoBackup.size() > 0){
                        ShortcutBadger.applyCount(getApplicationContext(),notifyInfoBackup.size());
                    } else{
                        ShortcutBadger.applyCount(getApplicationContext(),0);
                    }
                    cancelAllNotifications();
                }

            }catch (Exception e){
                NOTIFY notifyInfo = new NOTIFY(applicationName,notifyTtitle,notifyDateTime,e.toString());
                //myRef.push().setValue(notifyInfo);
            }

        }
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (Debug){
            Log.e(TAG,"Clear: "+sbn.getPackageName());
        }


        super.onNotificationRemoved(sbn);
    }

    private void toggleNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, com.singletoolman.notifyassistant.notifyassistant.SimpleNotificationListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, com.singletoolman.notifyassistant.notifyassistant.SimpleNotificationListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private String getAppName(StatusBarNotification sbn){
        String appName = "unknow";
        List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            if(p.packageName.equals(sbn.getPackageName())){
                appName = getPackageManager().getApplicationLabel(p.applicationInfo).toString();
                if (Debug){
                    Log.e(TAG,getPackageManager().getApplicationLabel(p.applicationInfo).toString()+"");
                }

            }
        }

        return appName;
    }

    private String getNowDateAndTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis()) ; // 獲取當前時間
        String timeStr = formatter.format(curDate);
        return timeStr;
    }

    public class NOTIFY {
        private String appName ;
        private String notifyTitle;
        private String notifyTime ;
        private String notifyContent;

        public NOTIFY() {
        }

        public NOTIFY(String appName,String notifyTitle ,String notifyTime, String notifyContent) {
            this.appName = appName;
            this.notifyTitle = notifyTitle;
            this.notifyTime = notifyTime;
            this.notifyContent = notifyContent;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String AppName) {
            this.appName = AppName;
        }

        public String getNotifyTitle() {
            return notifyTitle;
        }

        public void setNotifyTitle(String notifyTime) {
            this.notifyTitle = notifyTitle;
        }

        public String getNotifyTime() {
            return notifyTime;
        }

        public void setNotifyTime(String notifyTime) {
            this.notifyTime = notifyTime;
        }

        public String getNotifyContent() {
            return notifyContent;
        }

        public void setNotifyContent(String notifyContent) {
            this.notifyContent = notifyContent;
        }
    }
}
