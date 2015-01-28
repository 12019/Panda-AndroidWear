package com.smirnovlabs.android.panda;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import static com.smirnovlabs.android.panda.Constants.HEALTH;
import static com.smirnovlabs.android.panda.Constants.PANDA_BASE_URL;

/** Shows a list of commands. */
public class CommandsFragment extends Fragment {
    private String TAG = "PANDA command fragment";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_commands, container, false);

        TextView connection = (TextView) v.findViewById(R.id.connection);
        checkConnectionIndicator(v);



        return v;
    }

    void checkConnectionIndicator(final View v){
        String url = PANDA_BASE_URL + HEALTH;
        Ion.with(getActivity().getApplicationContext())
                .load(url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Log.d(TAG, "connection checker result: " + result);
                        updateConnectionIndicator(v, result.equals("\"OK\""));
                    }
                });
    }

    /** Updates the status on whether panda is reachable or not.*/
    private void updateConnectionIndicator(View v, boolean connected) {
        // FragmentManager fm = getFragmentManager();

        TextView connection = (TextView) v.findViewById(R.id.connection);

        if (connected) {
            Log.d(TAG, "connected to panda!");
            connection.setText(getResources().getText(R.string.connected));
        } else {
            Log.d(TAG, "disconnected to panda!");
            connection.setText(getResources().getText(R.string.disconnected));
        }
    }
}