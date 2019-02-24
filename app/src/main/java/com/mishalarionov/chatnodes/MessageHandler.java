package com.mishalarionov.chatnodes;

import android.widget.TextView;

import java.util.ArrayList;

public class MessageHandler {

    private String latestMessage;
    private TextView theBoi;

    MessageHandler(TextView newBoi) {
        theBoi = newBoi;
    }

    public void setLatestMessage(String message) {
        latestMessage = message;
        theBoi.setText(message);
    }
}
