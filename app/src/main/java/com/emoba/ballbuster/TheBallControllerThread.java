package com.emoba.ballbuster;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ch.fhnw.edu.emoba.spherolib.SpheroRobotFactory;
import ch.fhnw.edu.emoba.spherolib.SpheroRobotProxy;

/**
 * Created by jp on 03.04.18.
 */

public class TheBallControllerThread extends HandlerThread {

    public static final int BALL_CRUSE = 1;
    public static final int BALL_CALIBRATE = 2;
    public static final String HEADING = "heading";
    public static final String VELOCITY = "velocity";

    private final SpheroRobotProxy proxy;
    private Handler ballHandler;
    private float heading = 0;
    private float velocity = 0;
    private ScheduledFuture workerTask;

    public TheBallControllerThread(String name) {
        super(name);

        proxy = SpheroRobotFactory.getActualRobotProxy();


    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        ballHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == BALL_CRUSE) {
                    heading = msg.getData().getFloat(HEADING);
                    velocity = msg.getData().getFloat(VELOCITY);

                } else if (msg.what == BALL_CALIBRATE) {
                    proxy.drive(msg.getData().getFloat(HEADING),0);
                    proxy.setZeroHeading();
                    proxy.setLed(0,1,0);
                    proxy.setLed(0,0,1);
                    Log.i("TheBall", "Calibrated with: " + heading);
                }
            }
        };

        Thread talkToTheBallThread = new TalkToTheBallThread();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        workerTask = scheduler.scheduleAtFixedRate(talkToTheBallThread, 0, 30, TimeUnit.MILLISECONDS);

    }

    public Handler getBallThreadHandler() {
        return ballHandler;
    }

    private class TalkToTheBallThread extends Thread {
        private float headingbefore;
        private float velocitybefore;

        @Override
        public void run() {
            //check for invalidation
            if (heading != headingbefore || velocity != velocitybefore){
                proxy.drive(heading, velocity);
            }

//            Log.i("TheBall", "sent new vectors:" + heading +", "+velocity);

        }
    }

    public void stopTalkingToBall() {
        if (workerTask != null){
            workerTask.cancel(true);
        }
    }
}
