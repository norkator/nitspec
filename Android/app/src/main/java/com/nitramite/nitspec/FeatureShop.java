package com.nitramite.nitspec;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.BillingClient;

@SuppressWarnings("FieldCanBeLocal")
public class FeatureShop extends AppCompatActivity {


    // In app billing
    private BillingClient mBillingClient;

    // Variables
    private SharedPreferences sharedPreferences;
    private AudioPlayer audioPlayer = new AudioPlayer();
    private AppCompatButton donateMediumBtn, autoFireBuyBtn, nightVisionBuyBtn, restoreBoughtBtn;
    private TextView autoFireBuyOwned, nightVisionOwned;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_shop);

        // Shared prefs
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Find views
        donateMediumBtn = findViewById(R.id.donateMediumBtn);
        autoFireBuyBtn = findViewById(R.id.autoFireBuyBtn);
        nightVisionBuyBtn = findViewById(R.id.nightVisionBuyBtn);
        restoreBoughtBtn = findViewById(R.id.restoreBoughtBtn);
        autoFireBuyOwned = findViewById(R.id.autoFireBuyOwned);
        nightVisionOwned = findViewById(R.id.nightVisionOwned);

        // Set button states
        checkBoughtStates();

        // Init in app billing
        // initInAppBilling();


        donateMediumBtn.setOnClickListener(view -> {
            audioPlayer.playSound(FeatureShop.this, R.raw.pull_trigger);
            inAppPurchase(Constants.IAP_ITEM_SKU_DONATE_MEDIUM);
        });

        autoFireBuyBtn.setOnClickListener(view -> {
            audioPlayer.playSound(FeatureShop.this, R.raw.pull_trigger);
            inAppPurchase(Constants.IAP_ITEM_SKU_BUTTON_SELECT_TARGET_AUTO_FIRE);
        });

        nightVisionBuyBtn.setOnClickListener(view -> {
            audioPlayer.playSound(FeatureShop.this, R.raw.pull_trigger);
            inAppPurchase(Constants.IAP_ITEM_SKU_NIGHT_VISION_BASE);
        });

        restoreBoughtBtn.setOnClickListener(view -> {
            audioPlayer.playSound(FeatureShop.this, R.raw.pull_trigger);
            // restorePurchases();
        });

    } // End of onCreate()


    // ---------------------------------------------------------------------------------------------
    /* In app billing features */

    /* TODO, MIGRATE !  MIGRATE !  MIGRATE !  MIGRATE !  MIGRATE !  MIGRATE !  MIGRATE !

    // Initialize in app billing feature
    private void initInAppBilling() {
        // In app billing
        mBillingClient = BillingClient.newBuilder(this).setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_BUTTON_SELECT_TARGET_AUTO_FIRE)) {
                    setTargetAutoFireEnabled(false);

                } else if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_NIGHT_VISION_BASE)) {
                    setNightVisionEnabled(false);

                } else if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_DONATE_MEDIUM)) {
                    Toast.makeText(FeatureShop.this, "Thank you for your donation!", Toast.LENGTH_LONG).show();
                }
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }
    */

    // Donate action "purchase"
    public void inAppPurchase(final String IAP_ITEM_SKU) {
        //  if (mBillingClient.isReady()) {
        //      BillingFlowParams flowParams = BillingFlowParams.newBuilder()
        //              .setSku(IAP_ITEM_SKU)
        //              .setType(BillingClient.SkuType.INAPP)
        //              .build();
        //      mBillingClient.launchBillingFlow(this, flowParams);
        //  } else {
        //      genericErrorDialog("Billing service", "Billing service is not initialized yet. Please try again.");
        //      initInAppBilling();
        //  }
    }


    /*
    private void setTargetAutoFireEnabled(final Boolean isRestore) {
        SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = setSharedPreferences.edit();
        editor.putBoolean(Constants.SP_IAP_BUTTON_SELECT_TARGET_AUTO_FIRE, true);
        editor.apply();
        genericErrorDialog((isRestore ? "Restore" : "Purchase") + " success", "Auto fire feature is now enabled.");
        checkBoughtStates();
    }


    private void setNightVisionEnabled(final Boolean isRestore) {
        SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = setSharedPreferences.edit();
        editor.putBoolean(Constants.SP_IAP_NIGHT_VISION_BASE, true);
        editor.apply();
        genericErrorDialog((isRestore ? "Restore" : "Purchase") + " success", "Night vision feature is now enabled.");
        checkBoughtStates();
    }

    */
    /* In app Restore purchases */
    /*
    public void restorePurchases() {
        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,
                new PurchaseHistoryResponseListener() {
                    @Override
                    public void onPurchaseHistoryResponse(@BillingClient.BillingResponse int responseCode, List<Purchase> purchases) {
                        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                            if (purchases.size() > 0) {
                                for (Purchase purchase : purchases) {
                                    if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_BUTTON_SELECT_TARGET_AUTO_FIRE)) {
                                        setTargetAutoFireEnabled(true);
                                    } else if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_NIGHT_VISION_BASE)) {
                                        setNightVisionEnabled(true);
                                    }
                                }
                            } else {
                                genericErrorDialog(getString(R.string.error), "No purchases made");
                            }
                        } else {
                            genericErrorDialog(getString(R.string.error), "Error querying purchased items");
                        }
                    }
                });
    }

    */

    // ---------------------------------------------------------------------------------------------


    /**
     * Generic use error dialog
     *
     * @param title       Title
     * @param description Description
     */
    private void genericErrorDialog(final String title, final String description) {
        try {
            if (!this.isFinishing()) {
                new AlertDialog.Builder(FeatureShop.this)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(R.mipmap.nitspec_circle_logo)
                        .show();
            }
        } catch (RuntimeException ignored) {
        }
    }


    private void checkBoughtStates() {
        autoFireBuyBtn.setVisibility(sharedPreferences.getBoolean(Constants.SP_IAP_BUTTON_SELECT_TARGET_AUTO_FIRE, false) ? View.GONE : View.VISIBLE);
        autoFireBuyOwned.setVisibility(!sharedPreferences.getBoolean(Constants.SP_IAP_BUTTON_SELECT_TARGET_AUTO_FIRE, false) ? View.GONE : View.VISIBLE);

        nightVisionBuyBtn.setVisibility(sharedPreferences.getBoolean(Constants.SP_IAP_NIGHT_VISION_BASE, false) ? View.GONE : View.VISIBLE);
        nightVisionOwned.setVisibility(!sharedPreferences.getBoolean(Constants.SP_IAP_NIGHT_VISION_BASE, false) ? View.GONE : View.VISIBLE);
    }


} // End of class