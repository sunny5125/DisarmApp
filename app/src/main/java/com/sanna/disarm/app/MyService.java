package com.sanna.disarm.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Sanna on 16-02-2016.
 */
public class MyService extends Service {

    WifiManager wifi;
    String wifis[]={"None"}, checkWifiState="0x";
    int level;
    WifiScanReceiver wifiReciever;
    boolean b,c;
    Timer myTimer,wifiTimer;
    WifiInfo wifiInfo;
    List<String> IpAddr;
    BufferedReader br = null;
    FileReader fr = null;
    int count=0,startwififirst = 1;
    Handler handler;
    double wifiState;
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            Log.e("test", String.valueOf(level) + "%");

        }
    };
    private class WifiScanReceiver extends BroadcastReceiver{

        public void onReceive(Context c, Intent intent) {

            List<ScanResult> wifiScanList = wifi.getScanResults();
            wifis = new String[wifiScanList.size()];

            for(int i = 0; i < wifiScanList.size(); i++){
                wifis[i] = ((wifiScanList.get(i)).SSID);

                Log.d("wifi", wifis[i]);
            }
           }
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiReciever = new WifiScanReceiver();
        registerReceiver(wifiReciever, filter);
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi.startScan();

        IntentFilter batfilter = new IntentFilter();
        batfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatInfoReceiver,batfilter);

        handler = new Handler();

        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }

        }, 0, 60 * 1000);
        
        wifiTimer = new Timer();
        wifiTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                WifiTimerMethod();
            }

        }, 0, 10*1000);
    }
    

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.

        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReciever);
        // unregister receiver
        //unregisterReceiver(this.mBatInfoReceiver);
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Toggle);
    }

    private void WifiTimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(WifiConnect);
    }

    private Runnable Timer_Toggle = new Runnable() {

        public void run() {


            wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifi.getConnectionInfo();

            checkWifiState =  wifiInfo.getSSID();
            Toast.makeText(MyService.this, "Ticking", Toast.LENGTH_SHORT).show();
            Toast.makeText(MyService.this, checkWifiState, Toast.LENGTH_SHORT).show();
            count++;


            if (checkWifiState.equals("<unknown ssid>")) {
                Toast.makeText(MyService.this, "Hotspot Mode Detected", Toast.LENGTH_SHORT).show();

                boolean isReachable = false;
                try {
                    int macCount = 0;
                    fr =  new FileReader("/proc/net/arp");
                    br = new BufferedReader(fr);
                    String line;
                    IpAddr = new ArrayList<String>();
                    c = false;
                    while ((line = br.readLine()) != null)
                    {
                        String[] splitted = line.split(" +");

                        if (splitted != null )
                        {
                            if (splitted[3].matches("..:..:..:..:..:..")) {
                                Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 -t 1 " + splitted[0]);
                                int returnVal = p1.waitFor();
                                isReachable = (returnVal==0);

                            }
                            if(isReachable)
                            {
                                c = true;
                                Toast.makeText(
                                        getApplicationContext(),
                                        "C IS TRUE !!! ", Toast.LENGTH_SHORT).show();

                            }

                            // Basic sanity check
                            String mac = splitted[3];
                            System.out.println("Mac : Outside If " + mac);

                            if (mac.matches("..:..:..:..:..:.."))
                            {
                                macCount++;

                                IpAddr.add(splitted[0]);

                                System.out.println("Mac : " + mac + " IP Address : " + splitted[0]);
                                System.out.println("Mac_Count  " + macCount + " MAC_ADDRESS  "+ mac);


                                Toast.makeText(
                                        getApplicationContext(),
                                        "IP Address  " + splitted[0] + "   MAC_ADDRESS  "
                                                + mac, Toast.LENGTH_SHORT).show();

                            }
                        }
                    }
                    if(c) {
                        Toast.makeText(
                                getApplicationContext(),
                                "Connected!!! ", Toast.LENGTH_SHORT).show();


                    }
                    else
                    {
                        Toast.makeText(
                                getApplicationContext(),
                                "Not Connected!!! ", Toast.LENGTH_SHORT).show();

                    }
                } catch(Exception e){
                    Log.e("MYAPP", "exception", e);
                } finally {
                    if (fr != null) {
                        try {
                            fr.close();
                            br.close();
                            IpAddr.clear();
                            /*Toast.makeText(
                                    getApplicationContext(),
                                    "FATAL FINALLY !! ", Toast.LENGTH_SHORT).show();*/
                        } catch (IOException e) {
                            // This is unrecoverable. Just report it and move on
                            e.printStackTrace();
                        }
                    }
                }

                if(!c){
                    toggle();
                }



            } //if Completed check


            else if (checkWifiState.contains("DisarmHotspot")) {
                Toast.makeText(MyService.this, "DisarmConnected Not Toggling", Toast.LENGTH_SHORT).show();

            }
            else{
                toggle();
            }
        }
    };


    private Runnable WifiConnect = new Runnable() {

        public void run() {
            Toast.makeText(getApplicationContext(), "Running Autoconnector", Toast.LENGTH_SHORT).show();

            wifiInfo = wifi.getConnectionInfo();
            String ssidName = wifiInfo.getSSID();
            Toast.makeText(getApplicationContext(), ssidName, Toast.LENGTH_SHORT).show();
            if(ssidName.contains("DisarmHotspot")) {
                Toast.makeText(getApplicationContext(), "Already Connected", Toast.LENGTH_SHORT).show();

            }
            else if(!ssidName.equals("<unknown ssid>")){
                Toast.makeText(getApplicationContext(), "Checking For Disarm Hotspot", Toast.LENGTH_SHORT).show();

                // Connecting to DisarmHotspot WIfi on Button Click

                List allScanResults = wifi.getScanResults();
                //Toast.makeText(getApplicationContext(), allScanResults.toString(), Toast.LENGTH_SHORT).show();
                if (allScanResults.toString().contains("DisarmHotspot")) {
                    Toast.makeText(getApplicationContext(), "Connecting Disarm", Toast.LENGTH_SHORT).show();

                    String ssid = "DisarmHotspot";
                    WifiConfiguration wc = new WifiConfiguration();
                    wc.SSID = "\"" + ssid + "\""; //IMPORTANT! This should be in Quotes!!
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int res = wifi.addNetwork(wc);
                    boolean b = wifi.enableNetwork(res, true);
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();

                }
                else{
                    Toast.makeText(getApplicationContext(), "Disarm Not Available", Toast.LENGTH_SHORT).show();

                }

            }
        }
    };

    public void toggle(){
        Toast.makeText(MyService.this, "Toggling randomly!!!", Toast.LENGTH_LONG).show();

        //This method runs in the same thread as the UI.
        //Do something to the UI thread here
        if (startwififirst == 1){
            wifiState= 0.0;
            startwififirst = 0;
        }else{
            wifiState = Math.random()*1.0;

            Toast.makeText(MyService.this, String.valueOf(wifiState), Toast.LENGTH_SHORT).show();
        }


        //wifiState = false;
        // WifiState - 1 (Is Hotspot) || 0 - (CheckHotspot)
        if(wifiState<=0.25 && wifiState>=0.0) {
           // wifi.setWifiEnabled(false);
            b = ApManager.isApOn(MyService.this);

            if (!b) {
                ApManager.configApState(MyService.this);
            }
            Toast.makeText(MyService.this, "Hotspot Active", Toast.LENGTH_SHORT).show();

        }
        else {
            b = ApManager.isApOn(MyService.this);
            if(b)
            {
                ApManager.configApState(MyService.this);

            }

            wifi.setWifiEnabled(true);
            Toast.makeText(MyService.this, "Wifi Active", Toast.LENGTH_SHORT).show();

            wifi.startScan();

        }

    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}