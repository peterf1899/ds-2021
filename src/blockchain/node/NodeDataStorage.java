package blockchain.node;

import blockchain.block.Block;
import blockchain.block.Transaction;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class NodeDataStorage extends DataStorage {

    private static final String ADDRESS = "address";
    private static final String NEIGHBORS = "neighbors";
    private static final String BLOCKCHAIN = "blockchain";
    private static final String DETERMINER = " ;; ";

    public NodeDataStorage(String fileName) throws Exception {
        super(fileName);
    }

    // Read address
    public synchronized String getAddress() {
        return get(ADDRESS);
    }

    // Read list of neighbors
    public synchronized List<String> getNeighbors() {
        String neighborsString = get(NEIGHBORS);
        if (neighborsString == null || neighborsString.equals(""))
            return new LinkedList<>();
        String[] neighborsArray = neighborsString.split(";");
        return new LinkedList<>(Arrays.asList(neighborsArray));
    }

    // Store list of neighbors
    public synchronized void setNeighbors(List<String> neighbors) throws Exception {
        String neighborsString = String.join(";", neighbors);
        set(NEIGHBORS, neighborsString);
    }


    public synchronized List<Block> getBlockchain() {
        List<String> tmp = List.of(get(BLOCKCHAIN).split(DETERMINER));
        if (tmp.size() == 1) {
//            CREATE GENESIS
            try {
                Block genesisBlock = new Block(-1, "0", null, 0);
                ArrayList<Block> geneisList = new ArrayList<>();
                geneisList.add(genesisBlock);
                this.setBlockchain(geneisList);
                return geneisList;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        } else {
            List<Block> blockchain1 = new ArrayList<>();

            for (String block : tmp) {
                List<String> tmpBlock = List.of(block.split(";"));
                blockchain1.add(new Block(
                        Integer.parseInt(tmpBlock.get(0)),
                        tmpBlock.get(1),
                        parseTransactions(
                                tmpBlock.get(2).equals("null") ? "null" :
                                        tmpBlock.get(2).substring(1, tmpBlock.get(2).length() - 1)),
                        Long.parseLong(tmpBlock.get(3)),
                        tmpBlock.get(4)));
            }
            return blockchain1;

        }


    }

    public synchronized void setBlockchain(List<Block> blockchain) throws Exception {
        List<String> a = new ArrayList<>();
        for (Block block : blockchain) {
            a.add(block.toString());
        }
        String blockchainString = String.join(DETERMINER, a);
        set(BLOCKCHAIN, blockchainString);
    }


    private List<Transaction> parseTransactions(String transactions) {
        if (transactions == null || transactions.equals("") || transactions.equals("null")) return new ArrayList<>();
        List<String> tmp = List.of(transactions.split(","));
        List<Transaction> transactions1 = new ArrayList<>();

        for (String s : tmp) {
            List<String> a = List.of(s.split("\\|"));
            transactions1.add(new Transaction(a.get(0), Long.parseLong(a.get(1)), a.get(2)));
        }
        return transactions1;
    }

}
