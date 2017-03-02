/*package com.example.nima.usbarduino2;

  import android.app.Activity;
         import android.content.Context;
         import android.hardware.Sensor;
         import android.hardware.SensorEvent;
         import android.hardware.SensorEventListener; 
        import android.hardware.SensorManager;
         import android.net.Uri;
         import android.os.Bundle;
          import com.google.android.gms.appindexing.Action; 
        import com.google.android.gms.appindexing.AppIndex; 
        import com.google.android.gms.appindexing.Thing; import com.google.android.gms.common.api.GoogleApiClient;  

public class SensorActivity extends MainActivity implements SensorEventListener {
      
    @Override  

    public final void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState); 
        sMgr.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL); 
    }

    @Override  

    public final void onAccuracyChanged(Sensor sensor, int accuracy) {         // Do something here if sensor accuracy changes. 
    }

      

    @Override  

    public final void onSensorChanged(SensorEvent event) {         // The light sensor returns a single value.         // Many sensors return 3 values, one for each axis. 
        float lux = event.values[0]; 
        tvAppend(textView, "\nlux: " + lux); 
        // Do something with this sensor value. 
    }

       
    @Override  

    protected void onResume() { 
        super.onResume(); 
        sMgr.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
         }

      
    @Override  

    protected void onPause() { 
        super.onPause(); sMgr.unregisterListener(this);
         }

}*/