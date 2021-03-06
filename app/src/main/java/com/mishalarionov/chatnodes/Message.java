package com.mishalarionov.chatnodes;

public class Message {
    String text;
    String address;
    boolean ownMessage;
    String name;
    String message;

    public Message(String text, String address, boolean ownMessage){
        this.text= text;
        this.name = text.substring(0,3);
        if (this.name.equals("Pro")) {
            this.name = "Misha";
        } else if (this.name.equals("sca")) {
            this.name = "Dora";
        }
        this.message = text.substring(3);
        this.address = address;
        this.ownMessage = ownMessage;
    }

    public String getColor(){
        return "#"+address.substring(0,2)+address.substring(3,5)+address.substring(6,8);
    }

    public boolean equals (Message m1){
        if(text.equals(m1.text)&&address.equals(address)&&ownMessage==ownMessage){
            return true;
        }
        return false;
    }
}
