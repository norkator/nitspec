package com.nitramite.nitspec;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.nitramite.adapters.BluetoothDeviceItem;
import com.nitramite.adapters.SingleTapDetector;

import java.util.Set;

public class ButtonShoot extends AppCompatActivity {

    // Logging
    private static final String TAG = ButtonShoot.class.getSimpleName();

    // View components
    private TextView bluetoothConnectionStatusTitle;
    private Button triggerBtn;

    // Variables
    private SharedPreferences sharedPreferences;
    private static boolean activityActive = false;
    private AudioPlayer audioPlayer = new AudioPlayer();
    private Integer buttonAnimationMillis = 80;
    private GestureDetector gestureDetector;

    // Bluetooth and it's service
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService;


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(serviceMessageReceiver, serviceBroadcastUpdateIntentFilter());
        activityActive = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        activityActive = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        activityActive = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Destroy all bluetooth service components
        try {
            if (bluetoothService != null) {
                bluetoothService.disconnect();
                unbindService(serviceConnection);
            }
            bluetoothService = null;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_button_shoot);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
            }
        } else {
            //noinspection deprecation
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setNavigationBarColor(ContextCompat.getColor(ButtonShoot.this, R.color.colorBlack));

        // Get services
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Check for bluetooth device
        if (sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null) == null) {
            Toast.makeText(this, "No bluetooth device specified, quitting!", Toast.LENGTH_SHORT).show();
            ButtonShoot.this.finish();
        }

        // Find view components
        bluetoothConnectionStatusTitle = findViewById(R.id.bluetoothConnectionStatusTitle);
        triggerBtn = findViewById(R.id.triggerBtn);

        // View custom gesture listeners
        gestureDetector = new GestureDetector(ButtonShoot.this, new SingleTapDetector());

        triggerBtn.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(triggerBtn, "scaleX", 0.8f);
                    ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(triggerBtn, "scaleY", 0.8f);
                    scaleDownX.setDuration(buttonAnimationMillis);
                    scaleDownY.setDuration(buttonAnimationMillis);
                    AnimatorSet scaleDown = new AnimatorSet();
                    scaleDown.play(scaleDownX).with(scaleDownY);
                    scaleDown.start();
                    triggerBtn.setPressed(true);
                    break;
                case MotionEvent.ACTION_UP:
                    ObjectAnimator scaleDownX2 = ObjectAnimator.ofFloat(triggerBtn, "scaleX", 1f);
                    ObjectAnimator scaleDownY2 = ObjectAnimator.ofFloat(triggerBtn, "scaleY", 1f);
                    scaleDownX2.setDuration(buttonAnimationMillis);
                    scaleDownY2.setDuration(buttonAnimationMillis);
                    AnimatorSet scaleDown2 = new AnimatorSet();
                    scaleDown2.play(scaleDownX2).with(scaleDownY2);
                    scaleDown2.start();
                    triggerBtn.setPressed(false);
                    sendPullTriggerAction();
                    break;
            }
            return gestureDetector.onTouchEvent(motionEvent);
        });


        // Connect to scope hardware
        Intent btServiceIntent = new Intent(this, BluetoothService.class);
        bindService(btServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    } // End of onCreate()


    /**
     * This action pull's trigger / shoot
     */
    private void sendPullTriggerAction() {
        if (bluetoothService != null) {
            bluetoothService.write(Constants.PULL_TRIGGER_COMMAND, true);
            audioPlayer.playSound(ButtonShoot.this, R.raw.pull_trigger);
        }
    }


    // ---------------------------------------------------------------------------------------------
    /* Bluetooth service components and methods */


    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            try {
                bluetoothService = ((BluetoothService.LocalBinder) service).getService();
                connectToBluetoothDevice(); // We can now start connecting!
            } catch (ClassCastException ignored) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothService = null;
        }

    };


    /**
     * Connect to my earlier selected bluetooth device
     */
    private void connectToBluetoothDevice() {
        try {
            if (activityActive) {

                // Set bluetooth connecting status title
                final String connectingStr = getString(R.string.camera_connecting_title) + " " + sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null);
                bluetoothConnectionStatusTitle.setText(connectingStr);

                // Get scope bluetooth device for service
                BluetoothDeviceItem scopeBluetoothDeviceItem = getScopeBluetoothDeviceItem(sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null));
                if (scopeBluetoothDeviceItem == null) {
                    Toast.makeText(ButtonShoot.this, "Scope bluetooth device was undefined, quitting!", Toast.LENGTH_SHORT).show();
                    ButtonShoot.this.finish();
                }

                // Connect to this device
                if (bluetoothService != null && scopeBluetoothDeviceItem != null) {
                    if (bluetoothService.getState() == ConnectionState.STATE_NONE) {
                        // Try disconnect first, since this makes app work much better
                        bluetoothService.disconnect();
                        // Start connection to bluetooth device
                        bluetoothService.connect(scopeBluetoothDeviceItem.getBluetoothDevice());
                    } else {
                        genericErrorDialog("Error", "Bluetooth service not in STATE_NONE state");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            genericErrorDialog(getString(R.string.error), e.toString());
        }
    }


    /**
     * Get paired bluetooth devices
     */
    private BluetoothDeviceItem getScopeBluetoothDeviceItem(final String targetDeviceName) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices(); // // There are paired devices. Get the name and address of each paired device.
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(targetDeviceName)) {
                        return new BluetoothDeviceItem(ButtonShoot.this, device);
                    }
                }
            }
        } else {
            Toast.makeText(ButtonShoot.this, "Bluetooth adapter undefined, quitting!", Toast.LENGTH_SHORT).show();
            ButtonShoot.this.finish();
        }
        return null;
    }


    // Declare broadcast receiver actions
    private static IntentFilter serviceBroadcastUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionState.ACTION_STATE_NONE);
        intentFilter.addAction(ActionState.ACTION_STATE_CONNECTING);
        intentFilter.addAction(ActionState.ACTION_STATE_CONNECTED);
        intentFilter.addAction(ActionState.ACTION_CANCEL_DISCOVERY);
        intentFilter.addAction(ActionState.ACTION_CONNECTION_FAILED);
        intentFilter.addAction(ActionState.ACTION_CONNECTION_LOST);
        intentFilter.addAction(ActionState.ACTION_SCOPE_MESSAGE_IN);
        return intentFilter;
    }


    // Broadcast message receiver from service
    private final BroadcastReceiver serviceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                final String message = (intent.getStringExtra(action) != null ? intent.getStringExtra(action) : "");
                if (action != null) {
                    switch (action) {
                        case ActionState.ACTION_STATE_NONE:
                            bluetoothConnectionStatusTitle.setText(R.string.came_bluetooth_state_idle);
                            break;
                        case ActionState.ACTION_STATE_CONNECTED: // Works both bluetooth and wifi as is
                            audioPlayer.playSound(ButtonShoot.this, R.raw.bluetooth_connected);
                            bluetoothConnectionStatusTitle.setText(message);
                            break;
                        case ActionState.ACTION_CONNECTION_FAILED:
                            bluetoothConnectionStatusTitle.setText(R.string.came_bluetooth_state_connection_failed);
                            break;
                    }
                }
            } catch (NullPointerException e) {
                genericErrorDialog("Error", "Error on receiving communication events. Error message: " + e.toString());
            } catch (RuntimeException e) {
                genericErrorDialog("Error", e.toString());
            }
        }
    };


    // ---------------------------------------------------------------------------------------------
    /* Helpers */


    /**
     * Generic use error dialog
     *
     * @param title       Title
     * @param description Description
     */
    private void genericErrorDialog(final String title, final String description) {
        try {
            if (!this.isFinishing() && activityActive) {
                new AlertDialog.Builder(ButtonShoot.this)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton("Close", (dialog, which) -> {
                        })
                        .setIcon(R.drawable.circle_hi_res)
                        .show();
            }
        } catch (RuntimeException ignored) {
        }
    }


} // End of class
