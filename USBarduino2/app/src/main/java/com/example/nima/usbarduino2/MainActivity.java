package com.example.nima.usbarduino2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
//import com.example.nima.usbarduino2.SensorActivity;

import static com.example.nima.usbarduino2.R.id.textView;

public class MainActivity extends Activity implements SensorEventListener {
    public final String ACTION_USB_PERMISSION = "com.example.nima.usbarduino2.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    //BatteryManager batteryStat;
    //to see current battery status

    //USED for output formatting
    Sensor light;
    String light_name;

    Sensor gyro;
    String gyro_name;

    Sensor accel;
    String accel_name;

    Sensor mfield;
    String mfield_name;

    SensorManager sMgr;
    String str = ""; //the output string, this is sent through serial

    //sensor running limitations. Useful for testing, each sensor will only add the apptopriate number
    //      of data elements. There is no other way of limiting output.
    int light_events = 0;
    int light_limit = 20;

    int gyro_events = 0;
    int gyro_limit = 20;

    int accel_events = 0;
    int accel_limit = 20;

    int mfield_events = 0;
    int mfield_limit = 20;

    //SensorActivity sensors;


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView, "Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);
            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        setUiEnabled(false);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(Intent.ACTION_VIEW);
        registerReceiver(broadcastReceiver, filter);
        textView.append(usbManager.toString());

        //call sensors
        //getSensorInfo(20);//sample size

        //sensor implementation
        sMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        //light sensor
        light = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        light_name = light.getName();
        sMgr.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);

        //gyroscope
        gyro = sMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyro_name = gyro.getName();
        sMgr.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);

        //accelerometer
        accel = sMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accel_name = accel.getName();
        sMgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);

        //magnetic field (compass)
        mfield = sMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mfield_name = mfield.getName();
        sMgr.registerListener(this, mfield, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);


    }

    public void onClickStart(View view) {

        //textView.append("onClickStar");

        //sensors.onSensorChanged();

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        textView.append("Devices:" + Integer.toString(usbDevices.size()) + "\n");
        for (String key : usbDevices.keySet()) {
            textView.append(usbDevices.get(key).getDeviceName() + "\n");
        }

        if (!usbDevices.isEmpty()) {
            textView.append("1");
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                textView.append(Integer.toString(deviceVID));
                if (deviceVID != 0)//Arduino Vendor ID
                {
                    textView.append("requesting");
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = true;
                } else {
                    textView.append("request failed");
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }


    }

    //decide if you need this damn function

/*    public void getSensorInfo(int times) {
        //sensor implementation
        sMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        //light sensor
        light = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        light_name = light.getName();
        sMgr.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);

        //gyroscope
        gyro = sMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyro_name = gyro.getName();
        sMgr.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);

        //accelerometer
        accel = sMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accel_name = accel.getName();
        sMgr.registerListener(this, accel, 2000000000);
    }
*/
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public final void onSensorChanged(SensorEvent event) {// The light sensor returns a single value. 
        if (event.sensor.getName().equals(light_name)) //light data
        {
            if(light_events < light_limit) {
                light_events++;
                float lux = event.values[0];
                //tvAppend(textView, "\nlux: " + lux);
                str += "Light: " + String.valueOf(lux) + "\n";
            }
        }

        else if (event.sensor.getName().equals(gyro_name)) //gyro data
        {
            if (gyro_events < gyro_limit) {
                gyro_events++;

                str += "Gyro: " + String.valueOf(event.values[0]) + ", " + String.valueOf(event.values[1]) + ", " + String.valueOf(event.values[2]) + "\n";

            }
        }

        else if (event.sensor.getName().equals(accel_name)) //accelerameter data
        {
            if (accel_events < accel_limit) {
                accel_events++;
                str += "Accel: " + String.valueOf(event.values[0]) + ", " + String.valueOf(event.values[1]) + ", " + String.valueOf(event.values[2]) + "\n";
            }
        }

        else if (event.sensor.getName().equals(mfield_name)) //accelerameter data
        {
            if (mfield_events < mfield_limit) {
                mfield_events++;
                str += "MField: " + String.valueOf(event.values[0]) + ", " + String.valueOf(event.values[1]) + ", " + String.valueOf(event.values[2]) + "\n";
            }
        }
    }


    public void onClickSend(View view) {
        //resets the sensor data collection limit triggers.
        light_events = 0;
        accel_events = 0;
        gyro_events = 0;
        mfield_events = 0;

        str = str + "TV: " + editText.getText().toString();
        serialPort.write(str.getBytes());
        tvAppend(textView, "\nData Sent\n" + str + "\n");
        str="";

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView, "\nSerial Connection Closed! \n");

    }

    protected void onResume() {
        super.onResume();
        sMgr.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sMgr.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        sMgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
       }

    protected void onPause() {
        super.onPause();
        sMgr.unregisterListener(this);
       }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    public void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

}
