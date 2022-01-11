package blockchain.block;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block implements Serializable {
    private int id;
    private String previousHash;
    private List<Transaction> data;
    private long timestamp;
    private String blockHash;

    public int getId() {
        return id;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public List<Transaction> getData() {
        return data;
    }

    public Block(int id, String previousHash, List<Transaction> data, long date) throws NoSuchAlgorithmException {
        this.id = id + 1;
        this.previousHash = previousHash;
        this.data = data == null ? new ArrayList<>() : data;
        this.timestamp = date;

        String contentHash = Arrays.toString(getHash(this.data.toString()));

        try {
            this.blockHash = toHexString(getHash(contentHash+id+timestamp+previousHash));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

//    USED FOR REPLICA OF BLOCKCHAIN
    public Block(int id, String previousHash, List<Transaction> data, long timestamp, String blockHash) {
        this.id = id;
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
        this.blockHash = blockHash;
    }

    private byte[] getHash(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));

    }

    private String toHexString(byte[] hash)
    {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    @Override
    public String toString() {
        return id +
                ";" + previousHash +
                ";" + data +
                ";" + timestamp +
                ";" + blockHash;
    }
}
