package com.smirnovlabs.android.panda;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.common.base.Joiner;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import static com.smirnovlabs.android.common.Constants.CARBON_API_URL;
import static com.smirnovlabs.android.common.Constants.DAY_SUMMARY;
import static com.smirnovlabs.android.common.Constants.HEALTH;
import static com.smirnovlabs.android.common.Constants.MOST_PLAYED;
import static com.smirnovlabs.android.common.Constants.MUSIC_API_URL;
import static com.smirnovlabs.android.common.Constants.NEXT_SONG;
import static com.smirnovlabs.android.common.Constants.PANDA_BASE_URL;
import static com.smirnovlabs.android.common.Constants.PAUSE;
import static com.smirnovlabs.android.common.Constants.PLAY_SONG;
import static com.smirnovlabs.android.common.Constants.PREV_SONG;
import static com.smirnovlabs.android.common.Constants.RESUME;
import static com.smirnovlabs.android.common.Constants.TOP_RATED;
import static com.smirnovlabs.android.common.Constants.VOL_DOWN;
import static com.smirnovlabs.android.common.Constants.VOL_SET;
import static com.smirnovlabs.android.common.Constants.VOL_UP;

public class VoiceInput extends Activity implements GoogleApiClient.ConnectionCallbacks{

    /** Code used to get voice input as text. s*/
    private static final int SPEECH_REQUEST_CODE = 539890;

    /** Tag used for message passing. */
    private static final String PANDA_SERVICE_TAG = "/panda_service";

    /** Google API client used for message passing. */
    private GoogleApiClient mApiClient;

    /** Debug tag.*/
    private String TAG = "PANDA WEAR";

    /** Delim for splitting url and json. BE SURE to change the one in wear service as well if modifying. */
    private final String DELIM = "#";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_input);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                // mTextView = (TextView) stub.findViewById(R.id.text);
                // add button click listener
                Button voiceButton  = (Button) findViewById(R.id.start_voice_input);
                voiceButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displaySpeechRecognizer();
                    }
                });
            }
        });

        // prepare for message passing
        initGoogleApiClient();

        // DO NOT PUT findViewByID() here - will return null, since context is in fragment

        // Show this on app open, to save us a click.
        displaySpeechRecognizer();
    }



    /**
     * Create an intent that can start the Speech Recognizer activity
     * */
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    /**
     * This callback is invoked when the Speech Recognizer returns.
     * This is where you process the intent and extract the speech text from the intent.
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            processCommand(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Given a command, perform relevant API call to Panda on Puma.
     * */
    private void processCommand(String text) {
        String[] tokens = text.split(" ");
        JsonObject data = new JsonObject();
        String payload = "";

        Log.d(TAG, "parsing command");

        switch (tokens[0]) {
            case "play":
                if (tokens.length == 1) {
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + RESUME, data);
                    break;
                } else if (text.contains("top rated")) {
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + TOP_RATED, data);
                } else if (text.contains("most played")) {
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + MOST_PLAYED, data);
                } else { // name of song
                    payload = Joiner.on(" ").join(Arrays.copyOfRange(tokens,1, tokens.length));
                    System.out.println("payload: " + payload);
                    data.addProperty("query", payload);
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + PLAY_SONG, data);
                }
                break;

            case "next":
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + NEXT_SONG, data);
                break;

            case "skip":
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + NEXT_SONG, data);
                break;

            case "previous":
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + PREV_SONG, data);
                break;

            case "back":
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + PREV_SONG, data);
                break;

            case "pause":
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + PAUSE, data);
                break;

            case "paws": //TODO decide whether to keep or not
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + PAUSE, data);
                break;

            case "resume":
                sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + RESUME, data);
                break;

            case "health":
                sendAPICall(PANDA_BASE_URL + HEALTH, data);
                // TODO process return value here.
                break;

            case "tell":
                payload = Joiner.on(" ").join(Arrays.copyOfRange(tokens, 1, tokens.length));
                Log.d(TAG, "tell command with extra: " + payload);
                if (payload.equals("me about my day")) {
                    Log.d(TAG, "tell me about my day!");
                    sendAPICall(PANDA_BASE_URL + CARBON_API_URL + DAY_SUMMARY, data);
                }
                break;

            case "set":
                if (tokens[1].equals("volume")) {
                    if (tokens[2].equals("to")) {
                        payload = Joiner.on(" ").join(Arrays.copyOfRange(tokens, 3, tokens.length));
                    } else {
                        payload = Joiner.on(" ").join(Arrays.copyOfRange(tokens, 2, tokens.length));
                    }
                    System.out.println("payload: " + payload);
                    data.addProperty("value", payload);
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + VOL_SET, data);
                }
                break;

            case "volume":
                if (tokens[1].equals("up")) {
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + VOL_UP, data);
                } else if (tokens[1].equals("down")) {
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + VOL_DOWN, data);
                } else if (tokens[1].equals("set")) {
                    payload = Joiner.on(" ").join(Arrays.copyOfRange(tokens, 2, tokens.length));
                    System.out.println("payload: " + payload);
                    data.addProperty("value", payload);
                    sendAPICall(PANDA_BASE_URL + MUSIC_API_URL + VOL_SET, data);
                }
                break;

            default:
                Log.d(TAG, "unknown command");
                vibrate(2);
                // TODO - add pop up - didn't catch that
                // also, relaunch the voice recognizer activity
                displaySpeechRecognizer();

        }

    }


    /**
     *  Vibrates the watch for the specified number of pulses.
     * */
    private void vibrate(int numPulses) {
        Vibrator mVibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] onePulse = {0, 200};
        long[] twoPulses = {0, 200, 200, 200};
        switch (numPulses) {
            case 1:
                mVibrator.vibrate(onePulse, -1);
                break;
            case 2:
                mVibrator.vibrate(twoPulses, -1);
                break;
            default:
                // do nothing
        }
    }


    /* Sends a url and data object to the phone (WearMessageListener) for a REST API call. */
   private void sendAPICall(String url, JsonObject data) {
       String payload = url + DELIM + data.toString();
       Log.d(TAG, "sending message: " + payload);
       vibrate(1); // vibrate to inform user that API call is being sent.
       sendMessage(PANDA_SERVICE_TAG, payload);
   }

    /** Connect to the phone. */
    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();
        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //sendMessage( START_ACTIVITY, "" );
        // we don't use this, but can be used to trigger an action on connect. perhaps show a toast?
    }

    @Override
    public void onConnectionSuspended(int i) {
        // do nothing.
    }

    /**
     * Send an encoded message to the phone. Decoded in WearMessageListenerService in mobile app.
     * */
    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
                // display result
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        // TODO - Show toast on phone with command ran

                        // mEditText.setText( "" );
                    }
                });
            }
        }).start();
    }

    /**
     * Disconnect from node when app quits.
     * */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApiClient.disconnect();
    }

    //TODO - implement reverse message listener, for displaying panda status on wear. (maybe)
}
