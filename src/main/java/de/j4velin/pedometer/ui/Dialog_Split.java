/*
 * Copyright 2014 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.j4velin.pedometer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.j4velin.pedometer.BuildConfig;
import de.j4velin.pedometer.Database;
import de.j4velin.pedometer.R;
import de.j4velin.pedometer.StepSensorAcceleration;
import de.j4velin.pedometer.util.API26Wrapper;
import de.j4velin.pedometer.util.Logger;
import de.j4velin.pedometer.util.Util;

import static android.widget.Toast.LENGTH_LONG;

public class Dialog_Split extends Fragment implements SensorEventListener {

    private TextView start_time_view, end_time_view, stepsview,kllview;
    private Button btn_set,btn_reset,btn_zero,btn_return;
    private int split_steps;
    private int todayOffset, total_start, goal, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private boolean showSteps = true;
    Database db=Database.getInstance(getActivity());



    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //安卓8.0要求在前台启动后台服务，其他版本可以直接在后台启动服务。8.0的API level为26
        if (Build.VERSION.SDK_INT >= 26) {
            API26Wrapper.startForegroundService(getActivity(),
                    new Intent(getActivity(), StepSensorAcceleration.class));
        } else {
            getActivity().startService(new Intent(getActivity(), StepSensorAcceleration.class));
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dialog_split, null);
        start_time_view=  v.findViewById(R.id.start_time);
        end_time_view =  v.findViewById(R.id.end_time);
        stepsview = v.findViewById(R.id.walknum);
        kllview=v.findViewById(R.id.kllnum);
        btn_set=v.findViewById(R.id.set);
        btn_reset=v.findViewById(R.id.reset);
        btn_zero=v.findViewById(R.id.zero);
        btn_return=v.findViewById(R.id.Return);



//        int totalSteps= db.getTotalWithoutToday()+Math.max(db.getSteps(Util.getToday()) + db.getCurrentSteps(), 0);

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss
//获取当前时间





        btn_set.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(final View view) {
                Database db=Database.getInstance(getActivity());
                SharedPreferences prefs = getActivity().getSharedPreferences("pedometer",Context.MODE_MULTI_PROCESS);
                int totalSteps= Math.max( db.getSteps(Util.getToday()) +db.getCurrentSteps(), 0);
                split_steps = prefs.getInt("split_steps",  totalSteps);
                Date date = new Date(System.currentTimeMillis());
                start_time_view.setText(simpleDateFormat.format(date));
                stepsview.setText(String.valueOf(split_steps));
            }
        });
        btn_reset.setOnClickListener(new OnClickListener(){


            @Override
            public void onClick(final View view) {

                int totalSteps= Math.max( db.getSteps(Util.getToday()) +db.getCurrentSteps(), 0);
                Date date = new Date(System.currentTimeMillis());
                if(split_steps==0) end_time_view.setText(("没点开始"));
                else {

                    float step = (totalSteps - split_steps);
                    String s=String.valueOf(totalSteps)+"-"+String.valueOf(split_steps)+"="+String.valueOf(step);
                    stepsview.setText(s);
                    end_time_view.setText(simpleDateFormat.format(date));
                    kllview.setText(String.valueOf(step * 0.6f * 60 * 1.036f / 1000));
                }
            }
        });
        btn_zero.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                start_time_view.setText("");
                end_time_view.setText("");
                stepsview.setText("");
                kllview.setText("");
                split_steps=0;
            }
        });
        btn_return.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment newFragment = new Fragment_Overview();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(android.R.id.content, newFragment);
                transaction.commit();
            }
        });
        return v;
    }




    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);

        Database db = Database.getInstance(getActivity());

        if (BuildConfig.DEBUG) db.logState();
        // 读取今天的偏差
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL);
        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        // 注册一个传感器监听器在步数变化时实时更新UI
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) {
            new AlertDialog.Builder(getActivity()).setTitle(R.string.no_sensor)
                    .setMessage(R.string.no_sensor_explain)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(final DialogInterface dialogInterface) {
                            getActivity().finish();
                        }
                    }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        } else {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        }

        since_boot -= pauseDifference;

        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();


    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Logger.log(e);
        }
        Database db = Database.getInstance(getActivity());
        db.saveCurrentSteps(since_boot);
        db.close();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        return ((Activity_Main) getActivity()).optionsItemSelected(item);
    }


    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // 不会发生
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (BuildConfig.DEBUG) Logger.log(
                "UI - sensorChanged | todayOffset: " + todayOffset + " since boot: " +
                        event.values[0]);
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            todayOffset = -(int) event.values[0];
            Database db = Database.getInstance(getActivity());
            db.insertNewDay(Util.getToday(), (int) event.values[0]);
            db.close();
        }
        since_boot = (int) event.values[0];
        db.saveCurrentSteps(since_boot);
    }


//
}
