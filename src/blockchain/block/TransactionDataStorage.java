package blockchain.block;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TransactionDataStorage {

    private static final String fileName = "datastorage/transactions.datastorage";
    private FileWriter fw;
    private BufferedWriter bw;

    public TransactionDataStorage() {
    }

    public synchronized String getTransactions(){
        try {
            return Files.readString(Paths.get(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public synchronized void appendTransaction(String transaction) throws Exception {
        fw = new FileWriter(fileName, true);
        bw = new BufferedWriter(fw);
        bw.write(transaction);
        bw.newLine();
        bw.close();
    }

    public synchronized void rmTransactions() throws IOException {
        new FileWriter(fileName, false).close();
    }
}
