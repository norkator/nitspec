package com.nitramite.nitspec;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.nitramite.adapters.DialogMode;
import com.nitramite.adapters.HardwareItem;
import com.nitramite.adapters.HardwareSpinnerAdapter;
import com.nitramite.adapters.HardwareType;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class HardwareParameters extends AppCompatActivity {

    // Logging
    private static final String TAG = HardwareParameters.class.getSimpleName();

    // View components
    private Spinner gunSelectSpinner, ammunitionSelectSpinner;
    private Button addNewGunBtn, editGunBtn, addNewAmmunitionBtn, editAmmunitionBtn;

    // Variables
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private AudioPlayer audioPlayer = new AudioPlayer();
    private Vibrator vibrator;
    private int vibTime = 50;
    private SharedPreferences sharedPreferences;

    // Arrays
    private ArrayList<HardwareItem> gunHardwareItems;
    private ArrayList<HardwareItem> ammunitionHardwareItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_hardware_parameters);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Find view components
        gunSelectSpinner = findViewById(R.id.gunSelectSpinner);
        ammunitionSelectSpinner = findViewById(R.id.ammunitionSelectSpinner);
        addNewGunBtn = findViewById(R.id.addNewGunBtn);
        addNewAmmunitionBtn = findViewById(R.id.addNewAmmunitionBtn);
        editGunBtn = findViewById(R.id.editGunBtn);
        editAmmunitionBtn = findViewById(R.id.editAmmunitionBtn);

        // Get services
        vibrator = (Vibrator) HardwareParameters.this.getSystemService(Context.VIBRATOR_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        addNewGunBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(HardwareParameters.this, R.raw.pull_trigger);
            addGunDialog();
        });


        addNewAmmunitionBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(HardwareParameters.this, R.raw.pull_trigger);
            addOrUpdateAmmunitionDialog(DialogMode.INSERT, null);
        });


        editAmmunitionBtn.setOnClickListener(view -> {
            vibrate();
            audioPlayer.playSound(HardwareParameters.this, R.raw.pull_trigger);
            final Integer currentSelection = sharedPreferences.getInt(Constants.SP_SELECTED_AMMUNITION_ID, 0);
            for (int i = 0; i < ammunitionHardwareItems.size(); i++) {
                if (ammunitionHardwareItems.get(i).getHardwareId().equals(currentSelection)) {
                    addOrUpdateAmmunitionDialog(DialogMode.UPDATE, ammunitionHardwareItems.get(i));
                    break;
                }
            }
        });


        getGuns();
        getAmmunitions();
    } // End of onCreate();


    /**
     * Get guns
     */
    private void getGuns() {
        final Integer selectedGunId = sharedPreferences.getInt(Constants.SP_SELECTED_GUN_ID, 0);
        gunHardwareItems = databaseHelper.getGuns();
        HardwareSpinnerAdapter gunHardwareSpinnerAdapter = new HardwareSpinnerAdapter(this, gunHardwareItems);
        gunSelectSpinner.setAdapter(null);
        gunSelectSpinner.setAdapter(gunHardwareSpinnerAdapter);
        gunSelectSpinner.setOnItemSelectedListener(null);
        gunSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putInt(Constants.SP_SELECTED_GUN_ID, gunHardwareItems.get(i).getHardwareId());
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        gunSelectSpinner.setOnLongClickListener(view -> {
            final Integer currentSelection = sharedPreferences.getInt(Constants.SP_SELECTED_GUN_ID, 0);
            for (int i = 0; i < gunHardwareItems.size(); i++) {
                if (gunHardwareItems.get(i).getHardwareId().equals(currentSelection)) {
                    deleteConfirmationDialog(gunHardwareItems.get(i));
                    break;
                }
            }
            return true;
        });
        for (int i = 0; i < gunHardwareItems.size(); i++) {
            if (gunHardwareItems.get(i).getHardwareId().equals(selectedGunId)) {
                gunSelectSpinner.setSelection(i);
            }
        }
    }


    /**
     * Get ammunition's
     */
    private void getAmmunitions() {
        final Integer selectedAmmunitionId = sharedPreferences.getInt(Constants.SP_SELECTED_AMMUNITION_ID, 0);
        ammunitionHardwareItems = databaseHelper.getAmmunitions();
        editAmmunitionBtn.setEnabled(ammunitionHardwareItems.size() > 0);
        HardwareSpinnerAdapter ammunitionHardwareSpinnerAdapter = new HardwareSpinnerAdapter(this, ammunitionHardwareItems);
        ammunitionSelectSpinner.setAdapter(null);
        ammunitionSelectSpinner.setAdapter(ammunitionHardwareSpinnerAdapter);
        ammunitionSelectSpinner.setOnItemSelectedListener(null);
        ammunitionSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = setSharedPreferences.edit();
                editor.putInt(Constants.SP_SELECTED_AMMUNITION_ID, ammunitionHardwareItems.get(i).getHardwareId());
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        ammunitionSelectSpinner.setOnLongClickListener(view -> {
            final Integer currentSelection = sharedPreferences.getInt(Constants.SP_SELECTED_AMMUNITION_ID, 0);
            for (int i = 0; i < ammunitionHardwareItems.size(); i++) {
                if (ammunitionHardwareItems.get(i).getHardwareId().equals(currentSelection)) {
                    deleteConfirmationDialog(ammunitionHardwareItems.get(i));
                    break;
                }
            }
            return true;
        });
        for (int i = 0; i < ammunitionHardwareItems.size(); i++) {
            if (ammunitionHardwareItems.get(i).getHardwareId().equals(selectedAmmunitionId)) {
                ammunitionSelectSpinner.setSelection(i);
            }
        }
    }


    /**
     * Add new gun dialog
     */
    private void addGunDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_gun_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        final Button dismissBtn = dialog.findViewById(R.id.dismissBtn);
        final Button proceedBtn = dialog.findViewById(R.id.proceedBtn);
        final EditText letterInput = dialog.findViewById(R.id.letterInput);
        final EditText nameInput = dialog.findViewById(R.id.nameInput);
        final EditText descriptionInput = dialog.findViewById(R.id.descriptionInput);
        proceedBtn.setOnClickListener(view -> {
            // Get input variables
            final String letter = letterInput.getText().toString();
            final String name = nameInput.getText().toString();
            final String description = descriptionInput.getText().toString();
            // Insert
            if (letter.length() > 0 && name.length() > 0 && description.length() > 0) {
                // new HardwareItem(HardwareType.GUN, "G", "Gamo Socom Storm", "IGT 4.5mm")
                databaseHelper.insertGun(
                        new HardwareItem(HardwareType.GUN, null, letter, name, description)
                );
                getGuns();
                dialog.dismiss();
            } else {
                Toast.makeText(HardwareParameters.this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
            }
        });
        dismissBtn.setOnClickListener(view -> dialog.dismiss());
    }


    /**
     * Add new ammunition dialog
     */
    @SuppressLint("SetTextI18n")
    private void addOrUpdateAmmunitionDialog(final DialogMode dialogMode, final HardwareItem hardwareItem) {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_ammunition_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        final Button dismissBtn = dialog.findViewById(R.id.dismissBtn);
        final Button proceedBtn = dialog.findViewById(R.id.proceedBtn);
        final EditText letterInput = dialog.findViewById(R.id.letterInput);
        final EditText nameInput = dialog.findViewById(R.id.nameInput);
        final EditText descriptionInput = dialog.findViewById(R.id.descriptionInput);
        final EditText speedInput = dialog.findViewById(R.id.speedInput);
        final EditText weightInput = dialog.findViewById(R.id.weightInput);
        final EditText sizeXInput = dialog.findViewById(R.id.sizeXInput);
        final EditText sizeYInput = dialog.findViewById(R.id.sizeYInput);
        final EditText dragCoefficientXInput = dialog.findViewById(R.id.dragCoefficientXInput);
        final EditText dragCoefficientYInput = dialog.findViewById(R.id.dragCoefficientYInput);

        // Update case
        if (dialogMode == DialogMode.UPDATE) {
            letterInput.setText(hardwareItem.getItemLetter());
            nameInput.setText(hardwareItem.getItemName());
            descriptionInput.setText(hardwareItem.getItemDescription());
            speedInput.setText(Double.toString(hardwareItem.getAmmunitionSpeed()));
            weightInput.setText(Double.toString(hardwareItem.getAmmunitionWeight()));
            sizeXInput.setText(Double.toString(hardwareItem.getAmmunitionSizeMillisX()));
            sizeYInput.setText(Double.toString(hardwareItem.getAmmunitionSizeMillisY()));
            dragCoefficientXInput.setText(Double.toString(hardwareItem.getAmmunitionDragCoefficientXValue()));
            dragCoefficientYInput.setText(Double.toString(hardwareItem.getAmmunitionDragCoefficientYValue()));
        }

        proceedBtn.setOnClickListener(view -> {

            // Get input variables
            final String letter = letterInput.getText().toString();
            final String name = nameInput.getText().toString();
            final String description = descriptionInput.getText().toString();
            final Double speed = Double.parseDouble(speedInput.getText().toString());
            final Double weight = Double.parseDouble(weightInput.getText().toString());
            final Double sizeX = Double.parseDouble(sizeXInput.getText().toString());
            final Double sizeY = Double.parseDouble(sizeYInput.getText().toString());
            final Double coefficientX = Double.parseDouble(dragCoefficientXInput.getText().toString());
            final Double coefficientY = Double.parseDouble(dragCoefficientYInput.getText().toString());

            // Validate & Insert
            if (letter.length() > 0 && name.length() > 0 && description.length() > 0 && sizeX > 0.0 && sizeY > 0.0 && coefficientX > 0.0 && coefficientY > 0.0 && speed > 0.0 && weight > 0.0) {
                switch (dialogMode) {
                    case INSERT:
                        HardwareItem ammunitionItem = new HardwareItem(HardwareType.AMMUNITION, null, letter, name, description);
                        ammunitionItem.setAmmunitionSpeed(speed);
                        ammunitionItem.setAmmunitionWeight(weight);
                        ammunitionItem.setAmmunitionSizeMillisX(sizeX);
                        ammunitionItem.setAmmunitionSizeMillisY(sizeY);
                        ammunitionItem.setAmmunitionSizeMillisZ(0.0);
                        ammunitionItem.setAmmunitionDragCoefficientXValue(coefficientX);
                        ammunitionItem.setAmmunitionDragCoefficientYValue(coefficientY);
                        databaseHelper.insertAmmunition(ammunitionItem);
                        Toast.makeText(HardwareParameters.this, "New ammunition inserted", Toast.LENGTH_SHORT).show();
                        break;
                    case UPDATE:
                        hardwareItem.setItemLetter(letter);
                        hardwareItem.setItemName(name);
                        hardwareItem.setItemDescription(description);
                        hardwareItem.setAmmunitionSpeed(speed);
                        hardwareItem.setAmmunitionWeight(weight);
                        hardwareItem.setAmmunitionSizeMillisX(sizeX);
                        hardwareItem.setAmmunitionSizeMillisY(sizeY);
                        hardwareItem.setAmmunitionSizeMillisZ(0.0);
                        hardwareItem.setAmmunitionDragCoefficientXValue(coefficientX);
                        hardwareItem.setAmmunitionDragCoefficientYValue(coefficientY);
                        databaseHelper.updateAmmunition(hardwareItem);
                        Toast.makeText(HardwareParameters.this, hardwareItem.getItemName() + " updated!", Toast.LENGTH_SHORT).show();
                        break;
                }
                getAmmunitions();
                dialog.dismiss();
            } else {
                Toast.makeText(HardwareParameters.this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        dismissBtn.setOnClickListener(view -> dialog.dismiss());
    }


    // ---------------------------------------------------------------------------------------------
    /* Helpers */


    // Delete hardware confirmation dialog
    private void deleteConfirmationDialog(final HardwareItem hardwareItem) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete " + hardwareItem.getItemName())
                .setIcon(R.mipmap.nitspec_circle_logo)
                .setNegativeButton("Delete", (dialog, whichButton) -> {
                    switch (hardwareItem.getHardwareType()) {
                        case GUN:
                            databaseHelper.deleteGun(hardwareItem.getHardwareIdToString());
                            getGuns();
                            break;
                        case AMMUNITION:
                            databaseHelper.deleteAmmunition(hardwareItem.getHardwareIdToString());
                            getAmmunitions();
                            break;
                    }
                })
                .setNeutralButton("Return", null).show();
    }

    // Vibrate
    private void vibrate() {
        //if (VIBRATION_ENABLED) {
        vibrator.vibrate(vibTime);
        //}
    }


} // End of class