package org.kay.tasksc.channel;

public class CooperativeCloseMessage {
    private int channelID;
    private int aBalance;
    private int bBalance;

    public CooperativeCloseMessage(int channelID, int aBalance, int bBalance) {
        this.channelID = channelID;
        this.aBalance = aBalance;
        this.bBalance = bBalance;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
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
