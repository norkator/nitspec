package com.nitramite.nitspec;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nitramite.adapters.BluetoothDeviceItem;
import com.nitramite.adapters.ClickListener;
import com.nitramite.adapters.DeviceItemsAdapter;
import com.nitramite.adapters.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.Set;

@SuppressWarnings("FieldCanBeLocal")
public class MainMenu extends AppCompatActivity {

    // Logging
    private static final String TAG = MainMenu.class.getSimpleName();

    // View components
    private TextView versionTV;
    private CardView manageBluetoothBtn, hardwareParametersBtn, startCameraBtn, btnShotBtn, shoppingCartBtn;
    private ImageView manageBluetoothIcon;
    private TextView manageBluetoothTitle;
    private TextView noPairedDevicesTV;
    private ImageView nitramiteBtn, rateBtn;

    // Variables
    private AudioPlayer audioPlayer = new AudioPlayer();
    private Vibrator vibrator;
    private int vibTime = 50;
    private SharedPreferences sharedPreferences;
    private static boolean activityActive = false;
    private Dialog devicesDialog;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDeviceItem> pairedDevicesItemsArrayList = new ArrayList<>();
    private RecyclerView pairedDevicesRecyclerView;
    private RecyclerTouchListener recyclerTouchListener = null;

    // App bluetooth permissions
    private static final int BLUETOOTH_PERMISSIONS_REQUEST_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_BT_FOR_CAMERA = 2;
    private static final int REQUEST_ENABLE_BT_FOR_BTN_SHOOT = 3;
    private final String permissionBluetooth = Manifest.permission.BLUETOOTH;
    private final String permissionBluetoothAdmin = Manifest.permission.BLUETOOTH_ADMIN;
    private final String permissionGPS = Manifest.permission.ACCESS_COARSE_LOCATION;

    // App camera permissions
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 2;
    private final String permissionCamera = Manifest.permission.CAMERA;


    @Override
    protected void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { // Bluetooth switch result
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                Log.i(TAG, String.valueOf(resultCode));
                if (resultCode == -1) {
                    initializeBluetooth();
                } else {
                    genericErrorDialogForPermissionResult("Bluetooth", "You must enable bluetooth to continue. Otherwise application cannot lookup your scope hardware device.");
                }
                break;
            case REQUEST_ENABLE_BT_FOR_CAMERA:
                if (resultCode == -1) {
                    startActivity(new Intent(MainMenu.this, CameraSystem.class));
                } else {
                    genericErrorDialogForPermissionResult("Bluetooth", "You must have bluetooth enabled to continue to camera.");
                }
                break;
            case REQUEST_ENABLE_BT_FOR_BTN_SHOOT:
                if (resultCode == -1) {
                    startActivity(new Intent(MainMenu.this, ButtonShoot.class));
                } else {
                    genericErrorDialogForPermissionResult("Bluetooth", "You must have bluetooth enabled to continue to button shoot system.");
                }
                break;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_menu);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Init devices dialog before finding components
        devicesDialog = new Dialog(this);
        devicesDialog.setContentView(R.layout.devices_dialog);
        devicesDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        devicesDialog.setTitle("");
        devicesDialog.setCanceledOnTouchOutside(false);

        // Find view components
        versionTV = findViewById(R.id.versionTV);
        versionTV.setText("Beta, build: " + Utils.getAppVersionCode(this));
        manageBluetoothBtn = findViewById(R.id.manageBluetoothBtn);
        manageBluetoothIcon = findViewById(R.id.manageBluetoothIcon);
        manageBluetoothTitle = findViewById(R.id.manageBluetoothTitle);
        hardwareParametersBtn = findViewById(R.id.hardwareParametersBtn);
        startCameraBtn = findViewById(R.id.startCameraBtn);
        btnShotBtn = findViewById(R.id.btnShotBtn);
        shoppingCartBtn = findViewById(R.id.shoppingCartBtn);
        noPairedDevicesTV = devicesDialog.findViewById(R.id.noPairedDevicesTV);
        pairedDevicesRecyclerView = devicesDialog.findViewById(R.id.pairedDevicesRecyclerView);
        nitramiteBtn = findViewById(R.id.nitramiteBtn);
        rateBtn = findViewById(R.id.rateBtn);

        // Get services
        vibrator = (Vibrator) MainMenu.this.getSystemService(Context.VIBRATOR_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        setManageBluetoothButtonState(); // Reset se proper state for the button


        // Bluetooth manager button control
        manageBluetoothBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            if (sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null) != null) {
                forgetBluetoothDeviceName();
            } else {
                initializeBluetooth();
            }
        });

        // Hardware manager to setup hardware specs
        hardwareParametersBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            startActivity(new Intent(MainMenu.this, HardwareParameters.class));
        });

        // Start camera system button
        startCameraBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            startCamera();
        });

        // Start button shoot system
        btnShotBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            startBtnShoot();
        });

        // Nitramite contact form page
        nitramiteBtn.setOnClickListener(view -> {
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            openWebsite("http://www.nitramite.com/contact.html");
        });

        // Rate application
        rateBtn.setOnClickListener(view -> {
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            openWebsite("https://play.google.com/store/apps/details?id=com.nitramite.nitspec");
        });


        // Feature shop
        shoppingCartBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(MainMenu.this, R.raw.pull_trigger);
            startActivity(new Intent(MainMenu.this, FeatureShop.class));
        });


        // Debug
        // startActivity(new Intent(MainMenu.this, CameraSystem.class));
    } // End of onCreate();


    /**
     * Start camera, check for valid settings
     */
    private void startCamera() {
        // Check for permissions
        if (hasPermissions(MainMenu.this, new String[]{permissionCamera})) {
            if (validSettingRequirementsForCamera(true)) {
                if (bluetoothAdapter == null) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
                if (bluetoothAdapter.isEnabled()) {
                    startActivity(new Intent(MainMenu.this, CameraSystem.class));
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_FOR_CAMERA);
                }
            }
        } else {
            ActivityCompat.requestPermissions(MainMenu.this, new String[]{permissionCamera}, CAMERA_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     * Start btn shoot, check for valid settings
     */
    private void startBtnShoot() {
        // Check for permissions
        if (validSettingRequirementsForCamera(false)) {
            if (bluetoothAdapter == null) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            if (bluetoothAdapter.isEnabled()) {
                startActivity(new Intent(MainMenu.this, ButtonShoot.class));
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_FOR_BTN_SHOOT);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.i(TAG, "Permission request code: " + String.valueOf(requestCode));
        switch (requestCode) {
            case BLUETOOTH_PERMISSIONS_REQUEST_CODE: {
                Log.i(TAG, "Permission request for bluetooth");
                if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    initializeBluetooth();
                } else {
                    genericErrorDialogForPermissionResult("Permission problem", "This app requires Bluetooth to access bluetooth hardware device. Whole app translates pointless without this permission.");
                }
                break;
            }
            case CAMERA_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // startActivity(new Intent(MainMenu.this, CameraSystem.class));
                    startCamera();
                } else {
                    genericErrorDialogForPermissionResult("Permission problem", "Camera targeting system requires access to camera. Without camera whole feature is pointless.");
                }
                break;
            }
        }
    }


    // Initializes bluetooth
    private void initializeBluetooth() {
        // Check for permissions
        if (hasPermissions(this, new String[]{permissionBluetooth, permissionBluetoothAdmin, permissionGPS})) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                noBluetoothSupportDialog();
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    getPairedBluetoothDevices();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permissionBluetooth, permissionBluetoothAdmin, permissionGPS}, BLUETOOTH_PERMISSIONS_REQUEST_CODE);
        }
    }


    // Get's already paired bluetooth devices
    private void getPairedBluetoothDevices() {
        if (bluetoothAdapter != null) {
            pairedDevicesItemsArrayList.clear();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices(); // // There are paired devices. Get the name and address of each paired device.
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesItemsArrayList.add(new BluetoothDeviceItem(MainMenu.this, device));
                }
                noPairedDevicesTV.setVisibility(View.GONE);
                setPairedDevicesDialogData(pairedDevicesItemsArrayList);
            }
            showBluetoothDevicesDialog();
        } else {
            genericErrorDialog("Error", "Bluetooth adapter undefined");
        }
    }


    // Set paired devices dialog data
    private void setPairedDevicesDialogData(final ArrayList<BluetoothDeviceItem> deviceItemsList) {
        if (recyclerTouchListener != null) {
            pairedDevicesRecyclerView.removeOnItemTouchListener(recyclerTouchListener);
            recyclerTouchListener = null;
        }
        DeviceItemsAdapter deviceItemsAdapter = new DeviceItemsAdapter(deviceItemsList);
        pairedDevicesRecyclerView.setAdapter(deviceItemsAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        pairedDevicesRecyclerView.setLayoutManager(layoutManager);
        recyclerTouchListener = new RecyclerTouchListener(this, pairedDevicesRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                saveBluetoothDeviceName(position);
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        });
        pairedDevicesRecyclerView.addOnItemTouchListener(recyclerTouchListener);
    }


    // Show devices dialog
    private void showBluetoothDevicesDialog() {
        devicesDialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(devicesDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        devicesDialog.getWindow().setAttributes(lp);
    }


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
                new AlertDialog.Builder(MainMenu.this)
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


    /**
     * Error dialog for permissionResult. This leads to activityActive being false so it needed own method
     *
     * @param title       Title
     * @param description Description
     */
    private void genericErrorDialogForPermissionResult(final String title, final String description) {
        try {
            if (!this.isFinishing()) {
                new AlertDialog.Builder(MainMenu.this)
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


    // Check for required permissions
    private static boolean hasPermissions(Context context, String[] permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    private void noBluetoothSupportDialog() {
        new AlertDialog.Builder(MainMenu.this)
                .setTitle("Error")
                .setMessage("No bluetooth supported")
                .setPositiveButton("Close", (dialog, which) -> {
                })
                .setIcon(R.mipmap.nitspec_circle_logo)
                .show();
    }


    /**
     * Save connected device name
     *
     * @param devicePosition position from paired devices array
     */
    private void saveBluetoothDeviceName(final int devicePosition) {
        if (pairedDevicesItemsArrayList != null) {
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.putString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, pairedDevicesItemsArrayList.get(devicePosition).getDeviceName());
            editor.apply();
            Toast.makeText(this, "Default device saved", Toast.LENGTH_SHORT).show();
            devicesDialog.dismiss();
            setManageBluetoothButtonState();
        }
    }


    /**
     * Forget connected device name
     */
    private void forgetBluetoothDeviceName() {
        SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = setSharedPreferences.edit();
        editor.remove(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME);
        editor.apply();
        Toast.makeText(this, "Default device removed", Toast.LENGTH_SHORT).show();
        setManageBluetoothButtonState();
    }


    /**
     * Set proper states for the bluetooth button
     */
    private void setManageBluetoothButtonState() {
        if (sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null) != null) {
            manageBluetoothIcon.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_bluetooth_device_forget));
            manageBluetoothTitle.setText(R.string.main_menu_manage_bluetooth_button_forget_device_title);
        } else {
            manageBluetoothIcon.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_bluetooth_icon));
            manageBluetoothTitle.setText(R.string.main_menu_manage_bluetooth_button_connect_scope_title);
        }
    }


    // Vibrate
    private void vibrate() {
        //if (VIBRATION_ENABLED) {
        vibrator.vibrate(vibTime);
        //}
    }


    /**
     * Check for valid setting requirements for camera view
     *
     * @return Boolean
     */
    private Boolean validSettingRequirementsForCamera(boolean ammunitionRequired) {
        if (sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null) == null) {
            genericErrorDialog("Bluetooth device problem", "You don't have default scope bluetooth device yet selected.");
            return false;
        }
        if (!ammunitionRequired) {
            return true;
        }
        if (sharedPreferences.getInt(Constants.SP_SELECTED_AMMUNITION_ID, 0) == 0) {
            genericErrorDialog("Hardware problem", "Go to Hardware menu item and create gun and ammunition configurations.");
            return false;
        }
        return true;
    }


    /**
     * Helper to open website
     *
     * @param websiteUrl url to open
     */
    private void openWebsite(final String websiteUrl) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(websiteUrl));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            genericErrorDialog("Error", "Seems like you have no browser to open this website");
        }
    }

} // End of class