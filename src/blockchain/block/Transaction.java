package blockchain.block;

import java.io.Serializable;
import java.util.Date;

public class Transaction implements Serializable {

    private String ownerId;
    private long timeStamp;
    private String data;

    public Transaction(String ownerId, long date, String data) {
        this.ownerId = ownerId;
        this.timeStamp = date;
        this.data = data;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return ownerId + "|"+timeStamp +"|"+data;
    }


    public void printTransaction(){
        System.out.print("created: "+ new java.util.Date((this.getTimeStamp())));
        System.out.print(" creator: "+this.getOwnerId());
        System.out.print(" data: "+this.getData());
        System.out.println();
    }

}
