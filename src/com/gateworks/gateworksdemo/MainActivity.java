/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gateworks.gateworksdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

public class MainActivity extends Activity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    volatile boolean abort = false;
    static volatile ArrayList<String> pausedThreads = new ArrayList<String>();
    HashMap<String, List<String>> listDataChild;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // If gpios are available, execute polling update thread
        if (listDataChild.containsKey("GPIO"))
            new MonitorTimer("GPIO", 500).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // If hwmons are available, execute polling update thread
        if (listDataChild.containsKey("HWMON"))
            new MonitorTimer("HWMON", 1000).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void onDestroy() {
        abort = true;
        super.onDestroy();
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();
        String[] categories = new String[]{"LED", "GPIO", "HWMON", "PWM"};

        // Adding category headers
        listDataHeader.addAll(Arrays.asList(categories));

        // Adding child data
        int i = 0;
        for (String category: categories)
            listDataChild.put(listDataHeader.get(i++), propReader(category.toLowerCase()+"."));

        clearEmptyHeaders();
    }

    // Remove all headers that do not contain any children
    private void clearEmptyHeaders() {
        ArrayList<String> emptyList = new ArrayList<String>();
        for(Map.Entry<String, List<String>> entry: listDataChild.entrySet())
            if(entry.getValue().isEmpty())
                emptyList.add(entry.getKey());

        for(String item : emptyList) {
            listDataChild.remove(item);
            listDataHeader.remove(item);
        }
    }

    // Constructs list children via getprop call
    private ArrayList<String> propReader(String typePrefix) {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec("/system/bin/getprop");
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        ArrayList<String> propsFound = new ArrayList<String>();
        String line;
        try {
            // Read each line of getprop output
            while ((line = br.readLine()) != null) {
                // Add to the list if matching prefix is found
                if (line.contains(typePrefix))
                    propsFound.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return propsFound;
    }

    // Thread to update sysfs values at a set interval
    private class MonitorTimer extends AsyncTask<Integer, Integer, Integer> {
        ArrayList<ExpandableListAdapter.ViewHolder> holders;
        String category;
        int sleepTime, groupNumber;

        public MonitorTimer (String category, int sleepTime) {
            this.category = category;
            this.sleepTime = sleepTime;
            this.groupNumber = listDataHeader.indexOf(category);
            this.holders = listAdapter.holders.get(this.groupNumber);
        }

        protected Integer doInBackground(Integer... thing) {
            while(!abort)
                try {
                    // Sleep to avoid spinning...
                    Thread.sleep(sleepTime);
                    if (!pausedThreads.contains(category))
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // update values
                                for (ExpandableListAdapter.ViewHolder view : holders) {
                                    // Check if view exists, is correct type, and is visible
                                    if (view.view != null && view.groupPosition == groupNumber &&
                                            view.view.getVisibility() == View.VISIBLE) {
                                        if (category.equals("GPIO")) {
                                            listAdapter.updateGPIO(view);
                                        }
                                        else {
                                            listAdapter.updateHWMON(view);
                                        }
                                    }
                                }
                            }
                        });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return 0;
        }
    }
}

