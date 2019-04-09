package org.kay.tasksc.channel;

public class Receipt {
    private int channelID;
    private int nonce;
    private int aBalance;
    private int bBalance;

    public Receipt(int channelID, int nonce, int aBalance, int bBalance) {
        this.channelID = channelID;
        this.nonce = nonce;
        this.aBalance = aBalance;
        this.bBalance = bBalance;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public int getaBalance() {
        return aBalance;
    }

    public void setaBalance(int aBalance) {
        this.aBalance = aBalance;
    }

    public int getbBalance() {
        return bBalance;
    }

    public void setbBalance(int bBalance) {
        this.bBalance = bBalance;
    }
}
