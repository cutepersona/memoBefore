package com.peng.power.memo.manager;

public interface AsyncCallback<T> {
    void onCallback(T result);
}