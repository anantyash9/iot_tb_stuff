package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String THINGSBOARD_HOST = "demo.thingsboard.io";
    private static final String ACCESS_TOKEN = "p9XsWF1qVlci0xkWOPbJ";
    private MqttAsyncClient mThingsboardMqttClient;
    private String id="";
    private String name="";
    private String image="";
    private TextView id_view;
    private TextView name_view;
    private ImageView face_view;
    Bitmap face_image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        id_view = findViewById(R.id.id_no);
        name_view = findViewById(R.id.name);
        face_view = findViewById(R.id.imageView);

        try {
            mThingsboardMqttClient = new MqttAsyncClient("tcp://" + THINGSBOARD_HOST + ":1883", "thing", new MemoryPersistence());
        } catch (MqttException e) {
            Log.e(TAG, "Unable to create MQTT client", e);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        mqttConnect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mqttDisconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void mqttConnect() {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(ACCESS_TOKEN);
        mThingsboardMqttClient.setCallback(mMqttCallback);
        try {
            mThingsboardMqttClient.connect(connOpts, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT client connected!");
                    try {
                        mThingsboardMqttClient.subscribe("v1/devices/me/rpc/request/+", 0);
                    } catch (MqttException e) {
                        Log.e(TAG, "Unable to subscribe to rpc requests topic", e);
                    }
                    try {
                        mThingsboardMqttClient.publish("v1/devices/me/attributes", getIdStatusMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to publish GPIO status to Thingsboard server", e);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    if (e instanceof MqttException) {
                        MqttException mqttException = (MqttException) e;
                        Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s, code: %d", mqttException.getMessage(),
                                mqttException.getReasonCode()), e);
                    } else {
                        Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s", e.getMessage()), e);
                    }
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, String.format("Unable to connect to Thingsboard server: %s, code: %d", e.getMessage(), e.getReasonCode()), e);
        }
    }

    private void mqttDisconnect() {
        try {
            mThingsboardMqttClient.disconnect();
            Log.i(TAG, "MQTT client disconnected!");
        } catch (MqttException e) {
            Log.e(TAG, "Unable to disconnect from the Thingsboard server", e);
        }
    }

    private MqttCallback mMqttCallback = new MqttCallback() {

        @Override
        public void connectionLost(Throwable e) {
            Log.e(TAG, "Disconnected from Thingsboard server", e);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, String.format("Received message from topic [%s]", topic));
            String requestId = topic.substring("v1/devices/me/rpc/request/".length());
            JSONObject messageData = new JSONObject(new String(message.getPayload()));
            String method = messageData.getString("method");
            if (method != null) {
                if (method.equals("getIdStatus")) {
                    sendIdStatus(requestId);
                } else if (method.equals("setIdStatus")) {
                    JSONObject params = messageData.getJSONObject("params");
                    id = params.getString("id");
                    name = params.getString("name");
                    image = params.getString("image");
                    byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
                    face_image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (id != null) {
                        updateIdStatus(requestId);
                    }
                } else {
                    //Client acts as an echo service
                    mThingsboardMqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    };

    private MqttMessage getIdStatusMessage() throws Exception {
        JSONObject idStatus = new JSONObject();
            idStatus.put("Id",id);
            idStatus.put("Name",name);
        MqttMessage message = new MqttMessage(idStatus.toString().getBytes());
        return message;
    }

    private void sendIdStatus(String requestId) throws Exception {
        mThingsboardMqttClient.publish("v1/devices/me/rpc/response/" + requestId, getIdStatusMessage());
    }

    private void updateIdStatus(String requestId) throws Exception {
        JSONObject response = new JSONObject();
        runOnUiThread(new Runnable() {
            public void run() {
                id_view.setText(id);
                name_view.setText(name);
                face_view.setImageBitmap(face_image);
            }
        });

        response.put("id", id);
        response.put("name", name);

        MqttMessage message = new MqttMessage(response.toString().getBytes());

        mThingsboardMqttClient.publish("v1/devices/me/rpc/response/" + requestId, message);
        mThingsboardMqttClient.publish("v1/devices/me/attributes", message);
    }

}
