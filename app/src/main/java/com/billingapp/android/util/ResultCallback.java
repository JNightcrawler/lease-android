package com.billingapp.android.util;

/** Simple success/error callback for repository write operations, delivered on the main thread. */
public interface ResultCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}
