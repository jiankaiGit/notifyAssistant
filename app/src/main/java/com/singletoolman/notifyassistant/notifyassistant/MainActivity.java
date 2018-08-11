package com.singletoolman.notifyassistant.notifyassistant;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.leolin.shortcutbadger.ShortcutBadger;

public class MainActivity extends AppCompatActivity {
    public static String INTENT_ACTION_NOTIFICATION = "it.gmariotti.notification";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private Switch cancelAllNotify;
    private ListView notifyContentList;
    private ArrayList<String> notifyInfoBackup = new ArrayList<>();
    private SharedPreferences mySharePreference;
    private SharedPreferences.Editor editor;
    private ArrayAdapter adapter;
    private Handler refreshInfo = new Handler();
    private Runnable refreshInfoWork;
    private Button clearBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mySharePreference = getSharedPreferences("UserSetting",0);
        editor = mySharePreference.edit();
        adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, notifyInfoBackup);
        clearBtn = (Button)findViewById(R.id.clearBtn);
        notifyContentList = (ListView)findViewById(R.id.notifyInfoListView);
        notifyContentList.setAdapter(adapter);
        cancelAllNotify = (Switch)findViewById(R.id.cancelNotify);
        cancelAllNotify.setChecked(mySharePreference.getBoolean("clearNotify",false));
        cancelAllNotify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b == true){

                    editor.putBoolean("clearNotify",true);
                    editor.commit();
                }else{
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("關閉功能")
                            .setMessage("之後將不會再自動清除通知欄與記錄通知在此\n確定關閉?")
                            .setCancelable(false)
                            .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    editor.putBoolean("clearNotify",false);
                                    editor.commit();
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    cancelAllNotify.setChecked(true);
                                }
                            })
                            .show();
                }
            }
        });
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("清除本次通知")
                        .setMessage("將清除本次紀錄的通知\n是否要清除？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(!mySharePreference.getString("notifyBackupMessage","").equals("")){
                                    editor.putString("notifyBackupMessage","");
                                    editor.commit();
                                    ShortcutBadger.applyCount(getApplicationContext(),0);
                                }
                            }
                        })
                        .show();

            }
        });

        if (!isNotificationListenerServiceEnabled(this)){
            openNotificationAccess();
        }

        refreshInfoWork = new Runnable() {
            @Override
            public void run() {
                getNotifyBackupMessage();
                refreshInfo.postDelayed(refreshInfoWork,1000);
            }
        };

//        if(!mySharePreference.getString("notifyBackupMessage","").equals("")){
//            String jsonStr = mySharePreference.getString("notifyBackupMessage","");
//            try {
//                JSONArray jsonArray = new JSONArray(jsonStr);
//                for (int i = 0; i < jsonArray.length(); i++) {
//                    notifyInfoBackup.add((jsonArray.get(i).toString()));
//                }
//                adapter.notifyDataSetChanged();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//        }else{
//            notifyInfoBackup.clear();
//            adapter.notifyDataSetChanged();
//        }

    }

    @Override
    protected void onResume() {
        refreshInfo.postDelayed(refreshInfoWork,1000);
        //getNotifyBackupMessage();
        super.onResume();
    }

    @Override
    protected void onPause() {
        refreshInfo.removeCallbacks(refreshInfoWork);
        super.onPause();
    }

    private void getNotifyBackupMessage(){
        notifyInfoBackup.clear();
        if(!mySharePreference.getString("notifyBackupMessage","").equals("")){
            String jsonStr = mySharePreference.getString("notifyBackupMessage","");
            try {
                JSONArray jsonArray = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArray.length(); i++) {
                    notifyInfoBackup.add((jsonArray.get(i).toString()));
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }else{
            adapter.notifyDataSetChanged();
        }

        if(notifyInfoBackup.size() > 0){
            ShortcutBadger.applyCount(getApplicationContext(),notifyInfoBackup.size());
        } else{
            ShortcutBadger.applyCount(getApplicationContext(),0);
        }
    }

    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent

        }
    };



    private void openNotificationAccess() {
        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private static boolean isNotificationListenerServiceEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }
}
