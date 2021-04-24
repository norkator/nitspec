package com.nitramite.nitspec;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class FeatureShop extends AppCompatActivity implements PurchasesUpdatedListener {

    // Logging
    private static final String TAG = FeatureShop.class.getSimpleName();

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
        initInAppBilling();


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
            restorePurchases();
        });

    } // End of onCreate()


    // ---------------------------------------------------------------------------------------------
    /* In app billing features */


    // Initialize in app billing feature
    private void initInAppBilling() {
        mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    boolean ITEM_SKU_REMOVE_ADS_BOUGHT = false;
                    // The billing client is ready. You can query purchases here.
                    final Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
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
    public void onPurchasesUpdated(BillingResult billingResult, @androidx.annotation.Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_BUTTON_SELECT_TARGET_AUTO_FIRE)) {
                    setTargetAutoFireEnabled(false);

                } else if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_NIGHT_VISION_BASE)) {
                    setNightVisionEnabled(false);

                } else if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_DONATE_MEDIUM)) {
                    Toast.makeText(FeatureShop.this, "Thank you for your donation!", Toast.LENGTH_LONG).show();
                }
                acknowledgePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED && purchases != null) {
            for (Purchase purchase : purchases) {
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }
            }
        } else {
            // Handle any other error codes.
        }
    }


    /**
     * Acknowledge purchase required by billing lib >2.x++
     *
     * @param purchase billing purchase
     */
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> Toast.makeText(FeatureShop.this, "Purchase acknowledged!", Toast.LENGTH_SHORT).show();


    public void inAppPurchase(final String IAP_ITEM_SKU) {
        if (mBillingClient.isReady()) {

            List<String> skuList = new ArrayList<>();
            skuList.add(IAP_ITEM_SKU);

            SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(skuList).setType(BillingClient.SkuType.INAPP).build();

            mBillingClient.querySkuDetailsAsync(skuDetailsParams, (billingResult, skuDetailsList) -> {
                try {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList.get(0))
                            .build();
                    mBillingClient.launchBillingFlow(FeatureShop.this, flowParams);
                } catch (IndexOutOfBoundsException e) {
                    genericErrorDialog(getString(R.string.error), e.toString());
                }
            });
        } else {
            genericErrorDialog("Billing service", "Billing service is not initialized yet. Please try again.");
            initInAppBilling();
        }
    }


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


    /* In app Restore purchases */
    public void restorePurchases() {
        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, (billingResult, purchaseHistoryRecordList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchaseHistoryRecordList != null) {
                if (purchaseHistoryRecordList.size() > 0) {
                    for (PurchaseHistoryRecord purchase : purchaseHistoryRecordList) {
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
        });
    }


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
                        .setPositiveButton("Close", (dialog, which) -> {
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