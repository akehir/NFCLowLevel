package com.siemens.cordova.nfcalowlevel;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.telecom.Call;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;


public class NFCLowLevel extends CordovaPlugin {

    private static final String TAG = "NfcPlugin";
    private PendingIntent pendingIntent = null;
    private Intent savedIntent = null;
    private CallbackContext eventFireCallbackContext = null;
    private NfcA nfcA = null;


    // handles the commands received from javascript and forwards them to separate
    // methods
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("init")) {
            this.init(callbackContext);
            return true;
        }

        if (action.equals("addTagDiscoveredListener")) {
            this.addTagDiscoveredListener(callbackContext);
            return true;
        }
        if (action.equals("connect")) {
            this.connect(callbackContext);
            return true;
        }
        if (action.equals("transceive")) {
            // JSONArray data = args.
            this.transceive(args, callbackContext);
            return true;
        }
        if (action.equals("close")) {
            this.close(callbackContext);
            return true;
        }

        return false;
    }

    // gets called when plugin is loaded for the first time
    private void init(CallbackContext callbackContext) {
        Log.d(TAG, "NFCPlugin is being initialized");
        this.startNfc();
    }

    /**
     * saves the callback state to emulate an observable. Normally callbacks
     * can't be called twice
     * @return calls back string "init" for the first time, then calls back with JSON
     * object of discovered TAG
     */ 
    private void addTagDiscoveredListener(CallbackContext callbackContext) {
        eventFireCallbackContext = callbackContext;
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "init");
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    /**
     * sets internal NfcA state to connected. Now it is able to transceive
     */
    private void connect(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

                    if (tag == null) {
                        tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    }
                    if (tag == null) {
                        Log.e(TAG, "No Tag");
                        callbackContext.error("No Tag");
                        return;
                    }

                    nfcA = NfcA.get(tag);
                    if (nfcA == null) {
                        Log.e(TAG, "No Tech");
                        callbackContext.error("No NfcA Tech");
                        return;
                    }

                    Log.e(TAG, nfcA.toString());

                    Log.e(TAG, "## connect... ");
                    nfcA.connect();
                    nfcA.setTimeout(3000);
                    Log.e(TAG, "## connected ");

                    //nfcPlugin.fireConnected(nfcPlugin.tag);
                    Log.d(TAG, "returning now successfully from connect");
                    callbackContext.success();
                } catch (IOException ex) {
                    Log.e(TAG, "Can't connect to NfcA", ex);
                }
            }
        });
    }

    /**
     * Transmits data to Nfc tag
     * @param data data to transmit in from [{"0": <byte>, "1": <byte>, ... }]
     * it is inside an array because this function gets the whole argument JsonArray
     * @return a JsonArray of bytes received in form [<byte>, <byte>, ... ]
     */
    private void transceive(JSONArray data, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (nfcA == null) {
                        Log.e(TAG, "No Tech");
                        callbackContext.error("No Tech");
                        return;
                    }
                    if (!nfcA.isConnected()) {
                        Log.e(TAG, "Not connected");
                        callbackContext.error("Not connected");
                        return;
                    }

                    Log.d(TAG, data.toString());
                    byte[] commandAPDU = JSONArrayToByte(data);
                    Log.d(TAG, commandAPDU.toString());
                    byte[] responseAPDU = nfcA.transceive(commandAPDU);

                    Log.d(TAG, "sending: " + byteToHex(commandAPDU));
                    Log.e(TAG, "## transceive > " + byteToHex(responseAPDU));

                    callbackContext.success(byteArrayToJSON(responseAPDU));


                } catch (IOException ex) {
                    Log.e(TAG, "Can't connect to NfcA", ex);
                    callbackContext.error("transceive failed" + ex.getMessage());
                }
            }
        });
    }

    private String byteToHex(byte[] byteArray) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i=0; i<byteArray.length; i++){
            stringBuilder.append(String.format("%02X ", byteArray[i]));
        }

        return stringBuilder.toString();
    }

    private void close(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (nfcA == null) {
                        Log.e(TAG, "No Tech");
                        callbackContext.error("No Tech");
                        return;
                    }
                    if (!nfcA.isConnected()) {
                        Log.e(TAG, "Not connected");
                        callbackContext.error("Not connected");
                        return;
                    }

                    Log.e(TAG, "## close... ");
                    nfcA.close();
                    nfcA = null;
                    Log.e(TAG, "## closed ");
                    callbackContext.success();
                } catch (IOException ex) {
                    Log.e(TAG, "Can't connect to NfcA", ex);
                    callbackContext.error("Could not connect to NfcA");
                }
            }
        });
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "onPause " + getIntent());
        super.onPause(multitasking);
        if (multitasking) {
            // nfc can't run in background
            stopNfc();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume " + getIntent());
        super.onResume(multitasking);
        startNfc();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        parseMessage();
    }

    void parseMessage() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "parseMessage " + getIntent());
                Intent intent = getIntent();
                String action = intent.getAction();
                Log.d(TAG, "action " + action);
                if (action == null) {
                    return;
                }

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                    fireTagEvent(tag);
                }

                setIntent(new Intent());
            }
        });
    }

    private void fireTagEvent(Tag tag) {
        Log.d(TAG, "tag detected");

        if(eventFireCallbackContext == null){
            Log.d(TAG, "no listener active");
            return;
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, tagToJSON(tag));
        pluginResult.setKeepCallback(true);
        eventFireCallbackContext.sendPluginResult(pluginResult);

    }

    private JSONObject tagToJSON(Tag tag) {
        JSONObject json = new JSONObject();

        if (tag != null) {
            try {
                json.put("id", byteArrayToJSON(tag.getId()));
                json.put("techTypes", new JSONArray(Arrays.asList(tag.getTechList())));
            } catch (JSONException e) {
                Log.e(TAG, "Failed to convert tag into json: " + tag.toString(), e);
            }
        }
        return json;
    }

    private JSONArray byteArrayToJSON(byte[] byteArray) {
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<byteArray.length; i++){
            jsonArray.put(byteArray[i]);
        }
        return jsonArray;
    }

    private void startNfc() {
        Log.d(TAG, "startNfc");
        createPendingIntent(); // onResume can call startNfc before execute

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null && !getActivity().isFinishing()) {
                    try {
                        nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, null,
                                null);
                    } catch (IllegalStateException e) {
                        // issue 110 - user exits app with home button while nfc is initializing
                        Log.w(TAG, "Illegal State Exception starting NFC. Assuming application is terminating.");
                    }

                }
            }
        });
    }

    private void stopNfc() {
        Log.d(TAG, "stopNfc");
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    try {
                        nfcAdapter.disableForegroundDispatch(getActivity());
                    } catch (IllegalStateException e) {
                        // issue 125 - user exits app with back button while nfc
                        Log.w(TAG, "Illegal State Exception stopping NFC. Assuming application is terminating.");
                    }
                }
            }
        });
    }

    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private void createPendingIntent() {
        if (pendingIntent == null) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }

    private Intent getIntent() {
        return getActivity().getIntent();
    }

    private void setIntent(Intent intent) {
        getActivity().setIntent(intent);
    }

    private byte[] JSONArrayToByte(JSONArray jsonArray){
        JSONObject hexObject;
        try {
            hexObject = jsonArray.getJSONObject(0);
            byte[] bytes = new byte[hexObject.length()];
            for (int i = 0; i<bytes.length; i++){
                bytes[i] = (byte) hexObject.getInt(Integer.toString(i));
            }

            return bytes;
        } catch (JSONException e){
            Log.e(TAG, "JSON exception");
        }

        return null;
    }

}