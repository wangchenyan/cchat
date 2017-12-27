package me.wcy.cchat.model;

public interface Callback<T> {
    void onEvent(int code, String msg, T t);
}
