package com.nitramite.nitspec;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;

import com.nitramite.adapters.BluetoothDeviceItem;
import com.nitramite.adapters.HardwareItem;
import com.nitramite.libraries.horizontalwheelview.HorizontalWheelView;
import com.nitramite.math.MathUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("FieldCanBeLocal")
public class CameraSystem extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, SensorEventListener {

    // Logging
    private static final String TAG = CameraSystem.class.getSimpleName();

    // View components
    private TextView bluetoothConnectionStatusTitle, scopeHardwareReadingsTV, debugParametersTV, targetRangeTV, currentModeTV;
    private Button selectTargetBtn, pullTriggerEventBtn;
    private Button editParametersBtn;
    private Button zeroAngleSensorBtn;

    private CardView cameraZoomLevelSeekBarCard;
    private SeekBar cameraZoomLevelSeekBar;
    private Button cameraZoomLevelDismissBtn;

    private CardView gammaLevelAdjustCard;
    private Button gammaLevelDecrementBtn, gammaLevelIncrementBtn, gammaLevelDismissBtn;
    private TextView gammaLevelTV;

    private HorizontalWheelView manualTargetRangeAdjustWheel;
    private double manualTargetRangeAdjustPreviousValue = 0.0;


    // Bluetooth and it's service
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService;

    // Variables
    private ModeEnum CURRENT_MODE = ModeEnum.BUTTON_MANUAL_FIRE_ONLY;
    private Boolean TRIGGER_ACTIVATED = true;
    private Boolean SP_ENABLE_TRIGGER_SAFETY = true;
    private Boolean SP_ENABLE_TTS_ENGINE = true;
    private Boolean SP_TOGGLE_NIGHT_VISION = false;
    private Boolean SP_MANUAL_TARGET_RANGE_INPUT = true; // default
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private Integer selectedAmmunitionId;
    private HardwareItem ammunitionItem = null;
    private AudioPlayer audioPlayer = new AudioPlayer();
    private TextToSpeech textToSpeech;
    private static boolean activityActive = false;
    private SharedPreferences sharedPreferences;
    private Mat mRgbaMat;
    private ZoomCameraView zoomCameraView;
    private Integer cameraZoomLevel = 0;
    private Boolean autoPullTriggerActionSent = false;
    private Double GAMMA_VALUE = 1.0; // Night vision gamma value
    private DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private Long lastButtonPressMillis = System.currentTimeMillis() - (5 * 1000);
    private Boolean buttonTargetSelected = false; // For mode: BUTTON_SELECT_TARGET_BUTTON_MANUAL_FIRE


    // Scope hardware sensor readings
    private double pressureReadingHehtoPascals = 0.0;
    private double temperatureCelsiusReading = 0.0;
    private double targetRangeMetersReading = 80.0;
    private double voltageReading = 0.0;

    // Phone sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    static final float LPF_ALPHA = 0.025f; // Original low pass 0.25f
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private int counter = 0;
    private int counter2 = 0;
    private Float yAxisZeroing = 0.0f;
    private Float xAxisZeroing = 0.0f;

    // Target angle x and y points
    private Double targetYDegree = 0.0;
    private Double targetXDegree = 0.0;

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        activityActive = false;
        if (zoomCameraView != null) {
            zoomCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        registerReceiver(serviceMessageReceiver, serviceBroadcastUpdateIntentFilter());
        activityActive = true;
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
        // Destroy open cv component
        if (zoomCameraView != null) {
            zoomCameraView.disableView();
        }
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
        // Stop textToSpeech if active
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        // Save last used target range meters
        if (targetRangeMetersReading > 0) {
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.putInt(Constants.SP_MANUAL_TARGET_RANGE_LAST_VALUE, (int) targetRangeMetersReading);
            editor.apply();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera_system);
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

        // Get services
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        CURRENT_MODE = ModeEnum.valueOf(sharedPreferences.getString(Constants.SP_MODE_ENUM, ModeEnum.BUTTON_MANUAL_FIRE_ONLY.name())); // Get string to mode enum
        SP_ENABLE_TRIGGER_SAFETY = sharedPreferences.getBoolean(Constants.SP_ENABLE_TRIGGER_SAFETY, true);
        SP_ENABLE_TTS_ENGINE = sharedPreferences.getBoolean(Constants.SP_ENABLE_TTS_ENGINE, true);
        SP_TOGGLE_NIGHT_VISION = sharedPreferences.getBoolean(Constants.SP_TOGGLE_NIGHT_VISION, false);
        SP_MANUAL_TARGET_RANGE_INPUT = sharedPreferences.getBoolean(Constants.SP_MANUAL_TARGET_RANGE_INPUT, true);
        cameraZoomLevel = sharedPreferences.getInt(Constants.SP_CAMERA_ZOOM_LEVEL, cameraZoomLevel);
        GAMMA_VALUE = Utils.sharedPrefsLongToDouble(sharedPreferences, Constants.SP_NIGHT_VISION_GAMMA_LEVEL, GAMMA_VALUE);
        getUserGunHardwareParameters(); // Get selected ammunition params
        yAxisZeroing = sharedPreferences.getFloat(Constants.SP_CAMERA_ANGLE_Y_AXIS_ZEROING_OFFSET, 0.0f);
        xAxisZeroing = sharedPreferences.getFloat(Constants.SP_CAMERA_ANGLE_X_AXIS_ZEROING_OFFSET, 0.0f);

        if (SP_MANUAL_TARGET_RANGE_INPUT) {
            targetRangeMetersReading = (double) sharedPreferences.getInt(Constants.SP_MANUAL_TARGET_RANGE_LAST_VALUE, (int) targetRangeMetersReading);
        }

        // Check for bluetooth device
        if (sharedPreferences.getString(Constants.SP_SELECTED_BLUETOOTH_DEVICE_NAME, null) == null) {
            Toast.makeText(this, "No bluetooth device specified, quitting!", Toast.LENGTH_SHORT).show();
            CameraSystem.this.finish();
        }

        // Check for ammunition parameters
        if (ammunitionItem == null) {
            Toast.makeText(this, "Ammunition not defined or selected, quitting!", Toast.LENGTH_SHORT).show();
            CameraSystem.this.finish();
        }

        // Find view components
        bluetoothConnectionStatusTitle = findViewById(R.id.bluetoothConnectionStatusTitle);
        scopeHardwareReadingsTV = findViewById(R.id.scopeHardwareReadingsTV);
        scopeHardwareReadingsTV.setText(""); // Clear
        debugParametersTV = findViewById(R.id.debugParametersTV);
        targetRangeTV = findViewById(R.id.targetRangeTV);
        targetRangeTV.setText(" TR: " + String.valueOf(targetRangeMetersReading) + "m");
        selectTargetBtn = findViewById(R.id.selectTargetBtn);
        editParametersBtn = findViewById(R.id.editParametersBtn);
        pullTriggerEventBtn = findViewById(R.id.pullTriggerEventBtn);
        currentModeTV = findViewById(R.id.currentModeTV);
        setCurrentModeString();


        cameraZoomLevelSeekBarCard = findViewById(R.id.cameraZoomLevelSeekBarCard);
        cameraZoomLevelSeekBarCard.setVisibility(View.GONE);
        cameraZoomLevelSeekBar = findViewById(R.id.cameraZoomLevelSeekBar);
        cameraZoomLevelDismissBtn = findViewById(R.id.cameraZoomLevelDismissBtn);

        gammaLevelAdjustCard = findViewById(R.id.gammaLevelAdjustCard);
        gammaLevelAdjustCard.setVisibility(View.GONE);
        gammaLevelDecrementBtn = findViewById(R.id.gammaLevelDecrementBtn);
        gammaLevelIncrementBtn = findViewById(R.id.gammaLevelIncrementBtn);
        gammaLevelDismissBtn = findViewById(R.id.gammaLevelDismissBtn);
        gammaLevelTV = findViewById(R.id.gammaLevelTV);
        gammaLevelTV.setText(String.valueOf(decimalFormat.format(GAMMA_VALUE)));

        zeroAngleSensorBtn = findViewById(R.id.zeroAngleSensorBtn);
        zoomCameraView = findViewById(R.id.cameraOutputView);
        zoomCameraView.setMaxFrameSize(640, 480);
        zoomCameraView.setVisibility(SurfaceView.VISIBLE);
        zoomCameraView.setCvCameraViewListener(this);

        // Manual target range input
        manualTargetRangeAdjustWheel = findViewById(R.id.manualTargetRangeAdjustWheel);
        manualTargetRangeAdjustWheel.setCompleteTurnFraction(90);
        manualTargetRangeAdjustWheel.setShowActiveRange(false);
        toggleManualTargetRangeInput();
        manualTargetRangeAdjustWheel.setListener(new HorizontalWheelView.Listener() {
            @Override
            public void onRotationChanged(double radians) {
                if (counter2++ % 5 == 0) {
                    if (radians > manualTargetRangeAdjustPreviousValue) {
                        targetRangeMetersReading = targetRangeMetersReading + 1.0; // Increment by one meter
                    } else {
                        targetRangeMetersReading = targetRangeMetersReading - 1.0; // Decrement by one meter
                    }
                    manualTargetRangeAdjustPreviousValue = radians;
                    targetRangeTV.setText(" TR: " + String.valueOf(targetRangeMetersReading) + "m"); // Update view meters
                    counter2 = 1;
                }
            }
        });


        // Method to select current visible center angle as target
        selectTargetBtn.setOnClickListener(view -> selectTargetAction());

        // Method to send pull trigger command
        pullTriggerEventBtn.setOnClickListener(view -> sendPullTriggerAction());

        editParametersBtn.setOnClickListener(view -> {
            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
            parametersDialog();
        });

        cameraZoomLevelDismissBtn.setOnClickListener(view -> {
            cameraZoomLevelSeekBarCard.setVisibility(View.GONE);
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.putInt(Constants.SP_CAMERA_ZOOM_LEVEL, cameraZoomLevel);
            editor.apply();
        });


        gammaLevelDismissBtn.setOnClickListener(view -> {
            gammaLevelAdjustCard.setVisibility(View.GONE);
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.putLong(Constants.SP_NIGHT_VISION_GAMMA_LEVEL, Utils.sharedPrefsDoubleToLong(GAMMA_VALUE));
            editor.apply();
        });
        gammaLevelDecrementBtn.setOnClickListener(view -> {
            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
            GAMMA_VALUE = GAMMA_VALUE - 0.1;
            gammaLevelTV.setText(String.valueOf(decimalFormat.format(GAMMA_VALUE)));
        });
        gammaLevelIncrementBtn.setOnClickListener(view -> {
            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
            GAMMA_VALUE = GAMMA_VALUE + 0.1;
            gammaLevelTV.setText(String.valueOf(decimalFormat.format(GAMMA_VALUE)));
        });


        // Set zeroing for angle sensor Y and X axis and save values
        zeroAngleSensorBtn.setOnClickListener(view -> {
            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
            yAxisZeroing = mOrientation[2];
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.putFloat(Constants.SP_CAMERA_ANGLE_Y_AXIS_ZEROING_OFFSET, yAxisZeroing);
            editor.putFloat(Constants.SP_CAMERA_ANGLE_X_AXIS_ZEROING_OFFSET, xAxisZeroing);
            editor.apply();
            Toast.makeText(bluetoothService, "Y and X zeroing parameters saved", Toast.LENGTH_SHORT).show();
        });
        // Clear Y and X angle zeroing parameters
        zeroAngleSensorBtn.setOnLongClickListener(view -> {
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.remove(Constants.SP_CAMERA_ANGLE_Y_AXIS_ZEROING_OFFSET);
            editor.remove(Constants.SP_CAMERA_ANGLE_X_AXIS_ZEROING_OFFSET);
            editor.apply();
            yAxisZeroing = 0.0f;
            xAxisZeroing = 0.0f;
            Toast.makeText(bluetoothService, "Y and X zeroing parameters cleared", Toast.LENGTH_SHORT).show();
            return true;
        });


        // Initialize sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // TYPE_ACCELEROMETER
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        // TTS Engine support
        initializeTTSEngine();

        // Connect to scope hardware
        Intent btServiceIntent = new Intent(this, BluetoothService.class);
        bindService(btServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    } // End of onCreate();


    /**
     * Initialize OpenCV
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    zoomCameraView.initializeCamera(640, 480); // Call this before enableView(), causes otherwise small square image
                    zoomCameraView.enableView();
                    //zoomCameraView.setOnTouchListener(CameraSystem.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // Set saved zoom level
        zoomCameraView.setZoomLevel(sharedPreferences.getInt(Constants.SP_CAMERA_ZOOM_LEVEL, cameraZoomLevel));

        // Set night vision parameters
        if (SP_TOGGLE_NIGHT_VISION) {
            setNightVisionCameraParameters();
        }
    }


    @Override
    public void onCameraViewStopped() {
    }


    /**
     * Set night vision related camera parameters
     */
    private void setNightVisionCameraParameters() {
        try {
            // Set exposure
            if (zoomCameraView.setExposureCompensation(zoomCameraView.getMaxExposureCompensation())) {
                Toast.makeText(CameraSystem.this, "Exposure set", Toast.LENGTH_SHORT).show();
            }
            if (zoomCameraView.setPreviewFpsRange(12000, 15000)) { // DO NOT HARD CODE; GET VIA FLATTEN
                Toast.makeText(CameraSystem.this, "Fps min/max set", Toast.LENGTH_SHORT).show();
            }
        } catch (RuntimeException ignored) {
        }

        String[] flatten = zoomCameraView.getFlatten().split(";");
        for (String aFlatten : flatten) {
            Log.i(TAG, aFlatten);
        }

    }


    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Mat manipulation based on vision mode
        if (SP_TOGGLE_NIGHT_VISION) {
            mRgbaMat = matLowLightCorrection(inputFrame.gray()); // Mat frame
        } else {
            mRgbaMat = inputFrame.rgba(); // Mat frame
        }


        // Get screen center
        final int screenXCenter = mRgbaMat.width() / 2;
        final int screenYCenter = mRgbaMat.height() / 2;

        /* Do something with image */


        // Draw X line
        Point crossHairX1 = new Point(screenXCenter - 80, screenYCenter);
        Point crossHairX2 = new Point(screenXCenter + 80, screenYCenter);
        Imgproc.line(mRgbaMat, crossHairX1, crossHairX2, new Scalar(0, 255, 50), 1);

        // Draw Y line
        Point crossHairY1 = new Point(screenXCenter, screenYCenter - 80);
        Point crossHairY2 = new Point(screenXCenter, screenYCenter + 80);
        Imgproc.line(mRgbaMat, crossHairY1, crossHairY2, new Scalar(0, 255, 50), 1);


        if (targetYDegree != 0.0 && TRIGGER_ACTIVATED) {

            final Double cYDegrees = MathUtils.getProjectileYCorrectionDegrees(
                    1022.00,// pressureReadingHehtoPascals,
                    22.00,//temperatureCelsiusReading,
                    targetRangeMetersReading,// targetRangeMetersReading,
                    ammunitionItem.getAmmunitionSpeed(),
                    ammunitionItem.getAmmunitionWeight(),
                    ammunitionItem.getAmmunitionDragCoefficientXValue(),
                    ammunitionItem.getAmmunitionDragCoefficientYValue(),
                    ammunitionItem.getAmmunitionSizeMillisX(),
                    ammunitionItem.getAmmunitionSizeMillisY()
            );


            // Get variables
            final Double currentYDegrees = MathUtils.rollToDegrees(mOrientation[2], yAxisZeroing);
            final Double currentXDegrees = MathUtils.rollToDegrees(mOrientation[0], xAxisZeroing);
            final double correctionYDegrees = targetYDegree + cYDegrees; // Target + calculated correction trajectory degrees


            // Draw selected target circle
            final double selectedPointY = screenYCenter + ((MathUtils.flipValue(targetYDegree) + currentYDegrees) * 50); // Last * x can make circles flow more
            //final double selectedPointX = screenXCenter + ((MathUtils.flipValue(targetXDegree) + currentXDegrees) * 30); // Last * x can make circles flow more
            Imgproc.circle(mRgbaMat, new Point(screenXCenter, selectedPointY), 2, new Scalar(55, 242, 121), -1, 4, 0);


            // Draw guiding circle
            final double correctionPointY = screenYCenter + ((MathUtils.flipValue(correctionYDegrees) + currentYDegrees) * 50); // Last * x can make circles flow more
            Imgproc.circle(mRgbaMat, new Point(screenXCenter, correctionPointY), 2, new Scalar(250, 50, 50), -1, 8, 0);


            // Automatic trigger control
            if (CURRENT_MODE == ModeEnum.BUTTON_SELECT_TARGET_AUTO_FIRE) {
                if (!autoPullTriggerActionSent) {
                    if (currentYDegrees < (correctionYDegrees + 0.005) && currentYDegrees > (correctionYDegrees - 0.005)) {
                        sendPullTriggerAction();
                        autoPullTriggerActionSent = true; // Important! otherwise will shoot continuously
                    }
                }
            }


        }

        return mRgbaMat;
    }


    /**
     * Gamma correction method to correct image
     * https://docs.opencv.org/3.4.3/d3/dc1/tutorial_basic_linear_transform.html
     *
     * @param inputMat input mat frame
     * @return output mat frame
     */
    private Mat matLowLightCorrection(final Mat inputMat) {

        // Apply gamma correction
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, GAMMA_VALUE) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Mat outputMat = new Mat();
        Core.LUT(inputMat, lookUpTable, outputMat);
        lookUpTable.release();
        inputMat.release();

        return outputMat;
    }


    /**
     * Saturate methhod
     *
     * @param val saturation value
     * @return byte
     */
    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }


    /**
     * Select camera target
     */
    private void selectTargetAction() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                switch (CURRENT_MODE) {
                    case BUTTON_SELECT_TARGET_AUTO_FIRE:
                        if (System.currentTimeMillis() > (lastButtonPressMillis + (1000))) { // 1 seconds checking
                            toastFromThread("Target point selected!");
                            targetYDegree = MathUtils.rollToDegrees(mOrientation[2], yAxisZeroing);
                            targetXDegree = MathUtils.rollToDegrees(mOrientation[0], xAxisZeroing);
                            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
                            autoPullTriggerActionSent = false;
                            // Checking for valid readings
                            if (pressureReadingHehtoPascals == 0.0 || temperatureCelsiusReading == 0.0) {
                                speakTTS("Please wait for pressure and temperature readings");
                            }
                        }
                        break;
                    case BUTTON_SELECT_TARGET_BUTTON_MANUAL_FIRE:
                        if (!buttonTargetSelected) {
                            toastFromThread("Target point selected!");
                            targetYDegree = MathUtils.rollToDegrees(mOrientation[2], yAxisZeroing);
                            targetXDegree = MathUtils.rollToDegrees(mOrientation[0], xAxisZeroing);
                            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
                            autoPullTriggerActionSent = false;
                            // Checking for valid readings
                            if (pressureReadingHehtoPascals == 0.0 || temperatureCelsiusReading == 0.0) {
                                speakTTS("Please wait for pressure and temperature readings");
                            }
                            buttonTargetSelected = true;
                        } else {
                            sendPullTriggerAction();
                            autoPullTriggerActionSent = true; // Important! otherwise will shoot continuously
                            buttonTargetSelected = false;
                        }
                        break;
                    case BUTTON_MANUAL_FIRE_ONLY:
                        sendPullTriggerAction();
                        autoPullTriggerActionSent = true; // Important! otherwise will shoot continuously
                        break;
                }
                lastButtonPressMillis = System.currentTimeMillis();
                // Send acknowledgement with small delay
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            sendSelectTargetAcknowledgementAction();
                        } catch (final InterruptedException e) {
                            toastFromThread("Acknowledgement thread failed! " + e.toString());
                            e.printStackTrace();
                        }
                    }
                }).run();
            }
        };
        thread.start();
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
                    Toast.makeText(CameraSystem.this, "Scope bluetooth device was undefined, quitting!", Toast.LENGTH_SHORT).show();
                    CameraSystem.this.finish();
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
                        return new BluetoothDeviceItem(CameraSystem.this, device);
                    }
                }
            }
        } else {
            Toast.makeText(CameraSystem.this, "Bluetooth adapter undefined, quitting!", Toast.LENGTH_SHORT).show();
            CameraSystem.this.finish();
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
                            audioPlayer.playSound(CameraSystem.this, R.raw.bluetooth_connected);
                            bluetoothConnectionStatusTitle.setText(message);
                            speakTTS("Bluetooth connected");
                            break;
                        case ActionState.ACTION_CONNECTION_FAILED:
                            bluetoothConnectionStatusTitle.setText(R.string.came_bluetooth_state_connection_failed);
                            speakTTS("Bluetooth connection failed");
                            break;
                        case ActionState.ACTION_SCOPE_MESSAGE_IN:
                            parseScopeMessage(message);
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


    /**
     * Parse scope message
     *
     * @param scopeMessageData Incoming message from scope
     */
    private void parseScopeMessage(final String scopeMessageData) {
        //Log.i(TAG, scopeMessageData); // Example: PRS:991.73;TMP:23.60;VTG:12.02;

        // Parse commands
        if (scopeMessageData.equals("TRIGGER")) {
            selectTargetAction();
        }

        // Parse sensor readings
        try {
            String[] hwParams = scopeMessageData.split(";");
            for (String hwParam : hwParams) {
                if (hwParam.contains("PRS")) {
                    pressureReadingHehtoPascals = Double.parseDouble(hwParam.replace("PRS:", "").replace(";", ""));
                } else if (hwParam.contains("TMP")) {
                    temperatureCelsiusReading = Double.parseDouble(hwParam.replace("TMP:", "").replace(";", ""));
                } else if (hwParam.contains("VTG")) {
                    voltageReading = Double.parseDouble(hwParam.replace("VTG:", "").replace(";", ""));
                } else if (hwParam.contains("RNG")) {
                    targetRangeMetersReading = Double.parseDouble(hwParam.replace("RNG:", "").replace(";", ""));
                }
            }
            // Write debug if we had data
            if (hwParams.length > 0) {
                final String hardwareReadingsStr = " " +
                        Double.toString(voltageReading) + "V | " +
                        Double.toString(pressureReadingHehtoPascals) + "hPa | " +
                        Double.toString(temperatureCelsiusReading) + "°C";
                scopeHardwareReadingsTV.setText(hardwareReadingsStr);
            }
        } catch (Error e) {
            genericErrorDialog(getString(R.string.error), "Error when parsing data coming from scope hardware. " + e.toString());
        }
    }


    /**
     * This action pull's trigger / shoot
     */
    private void sendPullTriggerAction() {
        if (bluetoothService != null) {
            bluetoothService.write(Constants.PULL_TRIGGER_COMMAND, true);
            audioPlayer.playSound(CameraSystem.this, R.raw.pull_trigger);
        }
    }


    /**
     * Select target acknowledgement
     */
    private void sendSelectTargetAcknowledgementAction() {
        if (bluetoothService != null) {
            bluetoothService.write(Constants.SELECT_TARGET_ACKNOWLEDGEMENT, true);
        }
    }


    // ---------------------------------------------------------------------------------------------
    /* Helpers */


    /**
     * Run toast from thread, avoid crash on non ui thread
     *
     * @param text string to show
     */
    private void toastFromThread(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(bluetoothService, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Set current working mode
     */
    private void setCurrentModeString() {
        if (currentModeTV != null) {
            final String mode = " " + CURRENT_MODE.name();
            currentModeTV.setText(mode);
        }
    }


    /**
     * Manual target range input system (if no integration to range finder)
     */
    private void toggleManualTargetRangeInput() {
        if (SP_MANUAL_TARGET_RANGE_INPUT) {
            // Show
            manualTargetRangeAdjustWheel.setVisibility(View.VISIBLE);
        } else {
            // Do not show
            manualTargetRangeAdjustWheel.setVisibility(View.GONE);
        }
    }


    /**
     * Generic use error dialog
     *
     * @param title       Title
     * @param description Description
     */
    private void genericErrorDialog(final String title, final String description) {
        try {
            if (!this.isFinishing() && activityActive) {
                new AlertDialog.Builder(CameraSystem.this)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(R.drawable.circle_hi_res)
                        .show();
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Log.d(TAG, String.valueOf(sensorEvent.sensor.getType()));

        if (sensorEvent.sensor == accelerometer) {
            //System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometer = lowPassFilter(sensorEvent.values.clone(), mLastAccelerometer);
            mLastAccelerometerSet = true;

        } else if (sensorEvent.sensor == magnetometer) {
            // System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometer = lowPassFilter(sensorEvent.values.clone(), mLastMagnetometer);

            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);

            // orientation contains: azimut, pitch and roll
            // display every th values
            if (counter++ % 20 == 0) {

                final Double yDegrees = MathUtils.rollToDegrees(mOrientation[2], yAxisZeroing);

                // debugParametersTV.setText(String.format("Orientation: %f, %f, %f", mOrientation[0], mOrientation[1], mOrientation[2]));
                debugParametersTV.setText(" " + String.format("X: %f", MathUtils.azimuthToDegrees(mOrientation[0])) + " | "
                        /*+ String.format("Pitch: %f", mOrientation[1]) + " | "*/
                        + String.format("Y: %f", yDegrees));
                counter = 1;

                if (SP_ENABLE_TRIGGER_SAFETY) {
                    if (yDegrees > 70.0 || yDegrees < -70.0) {
                        if (TRIGGER_ACTIVATED) {
                            speakTTS("Deactivated");
                        }
                        TRIGGER_ACTIVATED = false;
                    } else {
                        if (!TRIGGER_ACTIVATED) {
                            speakTTS("Activated");
                        }
                        TRIGGER_ACTIVATED = true;
                    }
                } else {
                    TRIGGER_ACTIVATED = true;
                }

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }


    /**
     * Some notes for low pass filters: https://www.built.io/blog/applying-low-pass-filter-to-android-sensor-s-readings
     *
     * @param input  input
     * @param output output
     * @return return
     */
    protected float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + LPF_ALPHA * (input[i] - output[i]);
        }
        return output;
    }


    /**
     * Load user gun hardware parameters
     */
    private void getUserGunHardwareParameters() {
        selectedAmmunitionId = sharedPreferences.getInt(Constants.SP_SELECTED_AMMUNITION_ID, 0);
        ammunitionItem = databaseHelper.getSelectedAmmunition(String.valueOf(selectedAmmunitionId));
    }


    /**
     * Initialize tts engine
     */
    private void initializeTTSEngine() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                // Set locale as English
                textToSpeech.setLanguage(Locale.ENGLISH);
                // Look for us english male voice and set as current voice
                for (Voice tmpVoice : textToSpeech.getVoices()) {
                    // Log.i(TAG, tmpVoice.getName());
                    if (tmpVoice.getName().equals("en-us-x-sfg#male_1-local")) {
                        textToSpeech.setVoice(tmpVoice);
                        break;
                    }
                }
            }
        });
    }


    /**
     * Speak text
     *
     * @param text text to speak
     */
    private void speakTTS(final String text) {
        if (textToSpeech != null && SP_ENABLE_TTS_ENGINE) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "Utterance");
        }
    }


    /**
     * Parameters
     */
    private void parametersDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.camera_parameters_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        final Switch enableTTSEngineSwitch = dialog.findViewById(R.id.enableTTSEngineSwitch);
        enableTTSEngineSwitch.setChecked(sharedPreferences.getBoolean(Constants.SP_ENABLE_TTS_ENGINE, true));
        enableTTSEngineSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putBoolean(Constants.SP_ENABLE_TTS_ENGINE, b);
                SP_ENABLE_TTS_ENGINE = b;
                editor.apply();
            }
        });
        final Switch enableTriggerSafetySwitch = dialog.findViewById(R.id.enableTriggerSafetySwitch);
        enableTriggerSafetySwitch.setChecked(sharedPreferences.getBoolean(Constants.SP_ENABLE_TRIGGER_SAFETY, true));
        enableTriggerSafetySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putBoolean(Constants.SP_ENABLE_TRIGGER_SAFETY, b);
                SP_ENABLE_TRIGGER_SAFETY = b;
                editor.apply();
            }
        });
        final Button adjustZoomLevelBtn = dialog.findViewById(R.id.adjustZoomLevelBtn);
        adjustZoomLevelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraZoomLevelSeekBarCard != null && zoomCameraView != null) {
                    if (zoomCameraView.getZoomSupported()) {
                        cameraZoomLevelSeekBarCard.setVisibility(View.VISIBLE);
                        initSeekBarControls();
                        dialog.dismiss();
                    } else {
                        genericErrorDialog("Error", "Device does not support zooming");
                    }
                }
            }
        });
        final Button adjustNightVisionGammaLevelBtn = dialog.findViewById(R.id.adjustNightVisionGammaLevelBtn);
        adjustNightVisionGammaLevelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gammaLevelAdjustCard != null) {
                    gammaLevelAdjustCard.setVisibility(View.VISIBLE);
                    dialog.dismiss();
                }
            }
        });
        final Switch toggleNightVisionSwitch = dialog.findViewById(R.id.toggleNightVisionSwitch);
        toggleNightVisionSwitch.setEnabled(sharedPreferences.getBoolean(Constants.SP_IAP_NIGHT_VISION_BASE, false)); // Is feature bought check
        toggleNightVisionSwitch.setChecked(sharedPreferences.getBoolean(Constants.SP_TOGGLE_NIGHT_VISION, false));
        toggleNightVisionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putBoolean(Constants.SP_TOGGLE_NIGHT_VISION, b);
                SP_TOGGLE_NIGHT_VISION = b;
                editor.apply();
                if (b) {
                    setNightVisionCameraParameters();
                }
            }
        });
        final Switch manualTargetRangeInputSwitch = dialog.findViewById(R.id.manualTargetRangeInputSwitch);
        manualTargetRangeInputSwitch.setChecked(sharedPreferences.getBoolean(Constants.SP_MANUAL_TARGET_RANGE_INPUT, true));
        manualTargetRangeInputSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putBoolean(Constants.SP_MANUAL_TARGET_RANGE_INPUT, b);
                SP_MANUAL_TARGET_RANGE_INPUT = b;
                editor.apply();
                toggleManualTargetRangeInput();
            }
        });
        final Button dismissBtn = dialog.findViewById(R.id.dismissBtn);
        dismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        // Modes spinner
        final Spinner modeSelectorSpinner = dialog.findViewById(R.id.modeSelectorSpinner);
        final String[] modesList = {
                ModeEnum.BUTTON_MANUAL_FIRE_ONLY.name(),
                ModeEnum.BUTTON_SELECT_TARGET_AUTO_FIRE.name(),
                ModeEnum.BUTTON_SELECT_TARGET_BUTTON_MANUAL_FIRE.name(),
        };
        ArrayAdapter<String> modesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modesList);
        modeSelectorSpinner.setAdapter(modesAdapter);
        modeSelectorSpinner.setOnItemSelectedListener(null);
        modeSelectorSpinner.setSelection(getEnumListPosition(modesList, CURRENT_MODE.name()));
        modeSelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (ModeEnum.values()[i].equals(ModeEnum.BUTTON_SELECT_TARGET_AUTO_FIRE) && !sharedPreferences.getBoolean(Constants.SP_IAP_BUTTON_SELECT_TARGET_AUTO_FIRE, false)) {
                    genericErrorDialog("", "This feature needs bought before it can be used. See feature shop at main menu.");
                    return;
                }
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putString(Constants.SP_MODE_ENUM, modesList[i]);
                editor.apply();
                CURRENT_MODE = ModeEnum.values()[i]; // Set current selected mode
                setCurrentModeString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    /**
     * Get selection int
     *
     * @param enums          enums string[]
     * @param currentModeStr current mode
     * @return position
     */
    private int getEnumListPosition(final String[] enums, final String currentModeStr) {
        for (int i = 0; i < enums.length; i++) {
            if (enums[i].equals(currentModeStr)) {
                return i;
            }
        }
        return 0;
    }


    /**
     * Seek bar controls init
     */
    private void initSeekBarControls() {
        // Clear old instances
        cameraZoomLevelSeekBar.setOnSeekBarChangeListener(null);
        // Init listener
        cameraZoomLevelSeekBar.setMax(zoomCameraView.getMaxZoomLevel()); // Get maximum supported zoom level
        cameraZoomLevelSeekBar.setProgress(cameraZoomLevel);
        cameraZoomLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                zoomCameraView.setZoomLevel(i);
                cameraZoomLevel = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    // ---------------------------------------------------------------------------------------------

} // End of class