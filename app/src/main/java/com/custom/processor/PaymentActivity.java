package com.custom.processor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import co.poynt.api.model.Transaction;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntSecondScreenCashbackEntryListener;
import co.poynt.os.services.v1.IPoyntSecondScreenService;

public class PaymentActivity extends Activity implements
        CustomPaymentFragment.OnFragmentInteractionListener {

    private static final String TAG = "PaymentActivity";
    private IPoyntSecondScreenService mSecondScreenService;
    private AtomicBoolean active = new AtomicBoolean();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Get the intent that started this activity
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.e(TAG, "PaymentActivity launched with no intent!");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "binding to services...");
        active.set(true);
        bindService(new Intent(IPoyntSecondScreenService.class.getName()),
                mSecondScreenConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        active.set(false);
        Log.d(TAG, "unbinding from services...");
        unbindService(mSecondScreenConnection);
    }

    @Override
    public void onFragmentInteraction(Transaction transaction, PoyntError error) {
        // Create intent to deliver some kind of result data
        Intent result = new Intent(Intents.ACTION_COLLECT_PAYMENT_RESULT);
        result.putExtra("transaction", transaction);
        result.putExtra("error", error);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public void displaySecondScreen(final Bitmap image, final String message) {
        Log.d(TAG, "displaySecondScreen");
        if (mSecondScreenService != null) {
            try {
                //if you need to display some message to the user
                // mSecondScreenService.displayMessage(message, image);
                // if you need to collect cash back
                mSecondScreenService.captureCashback(1000, // txn amount
                        0, // tip amount
                        100000, // cashback limit
                        "USD", // currency
                        false, // do not timeout
                        0,
                        new IPoyntSecondScreenCashbackEntryListener.Stub() {
                            @Override
                            public void onCashbackResponse(long l) throws RemoteException {
                                Log.d(TAG, "Cashback collected:" + l);
                            }
                        }
                );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "not bound to second screen - trying again in 200 millisecs");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // only if we are still being displayed
                    if (active.get()) {
                        displaySecondScreen(image, message);
                    } else {
                        Log.d(TAG, "Activity not active anymore");
                    }
                }
            }, 200);
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if ("COLLECT_CUSTOM_PAYMENT".equals(action)) {
            final Transaction transaction = intent.getParcelableExtra("transaction");
            if (transaction == null) {
                Log.e(TAG, "PaymentActivity launched with no payment object");
                Intent result = new Intent(Intents.ACTION_COLLECT_PAYMENT_RESULT);
                setResult(Activity.RESULT_CANCELED, result);
                finish();
            } else {
                CustomPaymentFragment customPaymentFragment =
                        CustomPaymentFragment.newInstance(transaction);
                // prevent the merchant from dismissing the payment fragment by taping
                // anywhere on the screen
                customPaymentFragment.setCancelable(false);
                Log.d(TAG, "loading custom payment fragment");
                getFragmentManager().beginTransaction()
                        .add(R.id.container, customPaymentFragment)
                        .commit();
            }
        }
    }


    /**
     * Class for interacting with the Second Screen Service
     */
    private ServiceConnection mSecondScreenConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IPoyntSecondScreenService is now connected");
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mSecondScreenService = IPoyntSecondScreenService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IPoyntSecondScreenService has unexpectedly disconnected");
            mSecondScreenService = null;
        }
    };
}
