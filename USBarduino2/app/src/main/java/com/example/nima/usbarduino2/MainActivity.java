package com.example.nima.usbarduino2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import com.motorola.mod.ModDevice;
import com.motorola.mod.ModManager;

import android.os.Handler;
import android.os.Message;

public class MainActivity extends Activity implements SensorEventListener  {
    public final String ACTION_USB_PERMISSION = "com.example.nima.usbarduino2.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

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


    //MOTO MOD implementatino

    public static final String MOD_UID = "mod_uid";

    private static final int RAW_PERMISSION_REQUEST_CODE = 100;
    /**
     * Instance of MDK Personality Card interface
     */
    private Personality personality;

    int temp_events = 0;
    int temp_limit = 20;


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

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Personality.MSG_MOD_DEVICE:
                    /** Mod attach/detach */
                    ModDevice device = personality.getModDevice();
                    tvAppend(textView, "\nDevice attached/detached");
                    break;
                case Personality.MSG_RAW_DATA:
                    /** Mod raw data */
                    //tvAppend(textView,"\nRecieved Mod data\n");
                    byte[] buff = (byte[]) msg.obj;
                    int length = msg.arg1;
                    onRawData(buff, length);
                    break;
                case Personality.MSG_RAW_IO_READY:
                    /** Mod RAW I/O ready to use */
                    tvAppend(textView,"\nMod interface ready");

                    onRawInterfaceReady();
                    break;
                case Personality.MSG_RAW_IO_EXCEPTION:
                    /** Mod RAW I/O exception */
                    tvAppend(textView,"\nMod interface exception");
                    onIOException();
                    break;
                case Personality.MSG_RAW_REQUEST_PERMISSION:
                    /** Request grant RAW_PROTOCOL permission */
                    tvAppend(textView,"\nMod reqesting permission");
                    //personality.getRaw().checkRawInterface();
                    onRequestRawPermission();

                default:
                    Log.i(Constants.TAG, "MainActivity - Un-handle events: " + msg.what);
                    //break;
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
            }

            else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    onClickStart(startButton);
            }
            else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    onClickStop(stopButton);
            }
            }
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
        //sample size is defined above in the individual variable declarations

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

        //MOTO MOD IMPLEMENTATION
        //initPersonality();
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
    }

    public void onClickStart(View view) {

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
                    textView.append("Requesting");
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = true;
                } else {
                    textView.append("Request failed");
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public final void onSensorChanged(SensorEvent event) {// The light sensor returns a single value. 
        if (event.sensor.getName().equals(light_name)) //light data
        {
            if(light_events < light_limit) {
                light_events++;
                float lux = event.values[0];
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
        temp_events = 0;
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        int batVal = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        str = str + "Bat: " + String.valueOf(batVal) + "%";


        str = str + "\nTV: " + editText.getText().toString();
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
        sMgr.registerListener(this, mfield, SensorManager.SENSOR_DELAY_NORMAL);


        /** Initial MDK Personality interface */
        initPersonality();
       }

    protected void onPause() {
        super.onPause();
        sMgr.unregisterListener(this);
        personality.getRaw().executeRaw(Constants.RAW_CMD_STOP);
       }


    /** Initial MDK Personality interface */
    private void initPersonality() {
        if (null == personality) {
            personality = new RawPersonality(this, Constants.VID_MDK, Constants.PID_TEMPERATURE);
            personality.registerListener(handler);
        }
    }

    public void onRequestRawPermission() {
        requestPermissions(new String[]{ModManager.PERMISSION_USE_RAW_PROTOCOL},
                RAW_PERMISSION_REQUEST_CODE);
    }

    /** Handle permission request result */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RAW_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (null != personality) {
                    /** Permission grant, try to check RAW I/O of mod device */
                    personality.getRaw().checkRawInterface();
                }
            } else {
                // TODO: user declined for RAW accessing permission.
                // You may need pop up a description dialog or other prompts to explain
                // the app cannot work without the permission granted.
            }
        }
    }

    /** Handle the IO issue when write / read */
    public void onIOException() {tvAppend(textView,"\nIO Exception, MOD is not properly connected\n");
    }

    public void onRawData(byte[] buffer, int length) {
        /** Parse raw data to header and payload */
        int cmd = buffer[Constants.CMD_OFFSET] & ~Constants.TEMP_RAW_COMMAND_RESP_MASK & 0xFF;
        int payloadLength = buffer[Constants.SIZE_OFFSET];

        /** Checking the size of buffer we got to ensure sufficient bytes */
        if (payloadLength + Constants.CMD_LENGTH + Constants.SIZE_LENGTH != length) {
            return;
        }

        /** Parser payload data */
        //tvAppend(textView,"\nParsing data\n");
        byte[] payload = new byte[payloadLength];
        System.arraycopy(buffer, Constants.PAYLOAD_OFFSET, payload, 0, payloadLength);
        parseResponse(cmd, payloadLength, payload);
    }

    /** RAW I/O of attached mod device is ready to use */
    public void onRawInterfaceReady() {
        /**
         *  Personality has the RAW interface, query the information data via RAW command, the data
         *  will send back from MDK with flag TEMP_RAW_COMMAND_INFO and TEMP_RAW_COMMAND_CHALLENGE.
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                personality.getRaw().executeRaw(Constants.RAW_CMD_INFO);
            }
        }, 500);
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

    /** Parse the data from mod device */
    private void parseResponse(int cmd, int size, byte[] payload) {
        //textView.append("IN PARSE");
        if (cmd == Constants.TEMP_RAW_COMMAND_INFO) {
            //textView.append("\nGot raw command info\n");
            /** Got information data from personality board */

            /**
             * Checking the size of payload before parse it to ensure sufficient bytes.
             * Payload array shall at least include the command head data, and exactly
             * same as expected size.
             */
            if (payload == null
                    || payload.length != size
                    || payload.length < Constants.CMD_INFO_HEAD_SIZE) {
                return;
            }

            int version = payload[Constants.CMD_INFO_VERSION_OFFSET];
            int reserved = payload[Constants.CMD_INFO_RESERVED_OFFSET];
            int latencyLow = payload[Constants.CMD_INFO_LATENCYLOW_OFFSET] & 0xFF;
            int latencyHigh = payload[Constants.CMD_INFO_LATENCYHIGH_OFFSET] & 0xFF;
            int max_latency = latencyHigh << 8 | latencyLow;

            StringBuilder name = new StringBuilder();
            for (int i = Constants.CMD_INFO_NAME_OFFSET; i < size - Constants.CMD_INFO_HEAD_SIZE; i++) {
                if (payload[i] != 0) {
                    name.append((char) payload[i]);
                } else {
                    break;
                }
            }
            Log.i(Constants.TAG, "command: " + cmd
                    + " size: " + size
                    + " version: " + version
                    + " reserved: " + reserved
                    + " name: " + name.toString()
                    + " latency: " + max_latency);
        } else if (cmd == Constants.TEMP_RAW_COMMAND_DATA) {
            //textView.append("\nGot raw data\n");
            /** Got sensor data from personality board */

            /** Checking the size of payload before parse it to ensure sufficient bytes. */
            if (payload == null
                    || payload.length != size
                    || payload.length != Constants.CMD_DATA_SIZE) {
                return;
            }

            int dataLow = payload[Constants.CMD_DATA_LOWDATA_OFFSET] & 0xFF;
            int dataHigh = payload[Constants.CMD_DATA_HIGHDATA_OFFSET] & 0xFF;

            /** The raw temperature sensor data */
            int data = dataHigh << 8 | dataLow;

            /** The temperature */
            double temp = ((0 - 0.03) * data) + 128;

            if (temp_events < temp_limit) { //handles the volume of data sent to the arduino
                temp_events++;
                textView.append("\n*Mod Temp: " + String.valueOf(temp));
                str += "Temp: " + String.valueOf(temp) + "\n";
            }


        } else if (cmd == Constants.TEMP_RAW_COMMAND_CHALLENGE) {
            //textView.append("\nGot Challenge\n");
            /** Got CHALLENGE command from personality board */

            /** Checking the size of payload before parse it to ensure sufficient bytes. */
            if (payload == null
                    || payload.length != size
                    || payload.length != Constants.CMD_CHALLENGE_SIZE) {
                return;
            }

            byte[] resp = Constants.getAESECBDecryptor(Constants.AES_ECB_KEY, payload);
            if (resp != null) {
                /** Got decoded CHALLENGE payload */
                ByteBuffer buffer = ByteBuffer.wrap(resp);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // lsb -> msb
                long littleLong = buffer.getLong();
                littleLong += Constants.CHALLENGE_ADDATION;

                ByteBuffer buf = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN);
                buf.putLong(littleLong);
                byte[] respData = buf.array();

                /** Send challenge response back to mod device */
                byte[] aes = Constants.getAESECBEncryptor(Constants.AES_ECB_KEY, respData);
                if (aes != null) {
                    byte[] challenge = new byte[aes.length + 2];
                    challenge[0] = Constants.TEMP_RAW_COMMAND_CHLGE_RESP;
                    challenge[1] = (byte) aes.length;
                    System.arraycopy(aes, 0, challenge, 2, aes.length);
                    personality.getRaw().executeRaw(challenge);
                } else {
                    Log.e(Constants.TAG, "AES encrypt failed.");
                }
            } else {
                Log.e(Constants.TAG, "AES decrypt failed.");
            }
        } else if (cmd == Constants.TEMP_RAW_COMMAND_CHLGE_RESP) {
            //textView.append("\nResponding to challenge\n");
            /** Get challenge command response */

            /** Checking the size of payload before parse it to ensure sufficient bytes. */
            if (payload == null || payload.length != size || payload.length != Constants.CMD_CHLGE_RESP_SIZE) {
                return;
            }

            boolean challengePassed = payload[Constants.CMD_CHLGE_RESP_OFFSET] == 0;
            //textView.append("\nWe know the value of challangePassed is: " + challengePassed + "\n");
            int interval = 1000;
            byte intervalLow = (byte) (interval & 0x00FF);
            byte intervalHigh = (byte) (interval >> 8);
            byte[] cmd2 = {Constants.TEMP_RAW_COMMAND_ON, Constants.SENSOR_COMMAND_SIZE,
                    intervalLow, intervalHigh};

            personality.getRaw().executeRaw(cmd2);
        }
    }
}