package com.custom.processor;

import android.app.Application;

import com.custom.processor.core.TransactionManager;


/**
 * Created by palavilli on 1/25/16.
 */
public class CustomPaymentProcessorApplication extends Application {
    public static CustomPaymentProcessorApplication instance;

    public static CustomPaymentProcessorApplication getInstance() {
        return instance;
    }

    TransactionManager transactionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        transactionManager = TransactionManager.getInstance(this);
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
}
