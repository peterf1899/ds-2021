package blockchain.node;

import blockchain.block.Block;
import blockchain.block.Transaction;
import blockchain.block.TransactionDataStorage;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Node implements NodeConnector {

    private NodeDataStorage nodeDataStorage;
    private String address;
    private Registry registry;
    private Heartbeat heartbeat;
    private TransactionDataStorage TDStorage;
    private List<Transaction> transactionList;
    private List<Block> blockchain;
    private int numberOfValidationCalls = 0;
    private int numberOfBlockchainsRecieved = 0;
    private boolean isValid = true;
    private List<Block> tmpBlockchain = new ArrayList<>();
    private Block justCreatedBlock = null;

    public Node(String name) throws Exception {
        // Create data storage of a node
        this.nodeDataStorage = new NodeDataStorage(name);

        // Get node's address from data storage
        this.address = this.nodeDataStorage.getAddress();

        // Start node service
        int port = Integer.valueOf(this.address.substring(this.address.indexOf(":") + 1));
        this.registry = LocateRegistry.createRegistry(port);
        this.registry.bind(SERVICE_NAME, UnicastRemoteObject.exportObject(this, port));
        System.out.println("Node " + name + " at " + this.address + " is started.");

        // Start heartbeat thread
        this.heartbeat = new Heartbeat(this);
        this.heartbeat.start();
        this.TDStorage = new TransactionDataStorage();
        this.transactionList = this.parseTransactions(this.TDStorage.getTransactions());
        this.blockchain = this.nodeDataStorage.getBlockchain();
    }


    private List<Transaction> parseTransactions(String transactions) {
        if (transactions == null || transactions.equals("") || transactions.equals("null")) return new ArrayList<>();
        List<Transaction> transactions1 = new ArrayList<>();
        for (String transaction : List.of(transactions.split("\n"))) {
            List<String> item = List.of(transaction.split("\\|"));
            transactions1.add(new Transaction(item.get(0), Long.parseLong(item.get(1)), item.get(2)));
        }
        return transactions1;
    }

    @Override
    public List<String> getNeighbors() throws RemoteException {
        return this.nodeDataStorage.getNeighbors();
    }

    @Override
    public List<Block> getBlockchain() throws RemoteException {
        return blockchain;
    }

    @Override
    public List<Transaction> getTransactionList() throws RemoteException {
        return transactionList;
    }

    @Override
    public void stop() throws RemoteException {
        this.heartbeat.stop();

        try {
            this.registry.unbind(SERVICE_NAME);
            UnicastRemoteObject.unexportObject(this, true);
            System.out.println("Node " + this.address + " is stopped.");
        } catch (Exception e) {
            System.out.println("Cannot stop node " + this.address + ".");
        }


    }

    @Override
    public void connect(String address) throws RemoteException {
        List<String> neighbours = this.nodeDataStorage.getNeighbors();

        // Add new address to the list of neighbors
        if (!neighbours.contains(address)) {
            neighbours.add(address);
            System.out.println("New node " + address + " is connected. Add it to list of neighbors.");
            try {
                this.nodeDataStorage.setNeighbors(neighbours);
            } catch (Exception e) {
                System.out.println("Cannot store list of neighbors.");
            }
        }
    }


    public void susedia(String address, List<String> susedia) throws RemoteException {
        //moji susedia
        List<String> neighbors = this.nodeDataStorage.getNeighbors();
        susedia.remove(this.nodeDataStorage.getAddress());

        for (String sused : susedia) {
            if (!neighbors.contains(sused)) {
                neighbors.add(sused);
            }
        }

        try {
            this.nodeDataStorage.setNeighbors(neighbors);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void createTransaction(String address, Transaction transaction, boolean forward) throws RemoteException {
        System.out.println("Received transaction: " + transaction);
        this.transactionList.add(transaction);
        // Forward transaction to all neighbors
        if (forward) {
            try {
                this.TDStorage.appendTransaction(transaction.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (String neighbor : this.nodeDataStorage.getNeighbors()) {

                // Do not forward back to sender node
                if (!neighbor.equals(address)) {
                    String[] neighborStrings = neighbor.split(":");
                    try {
                        NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                        neighborNode.createTransaction(this.address, transaction, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (transactionList.size() > 3) {
//            TODO SORT BY DATE?

//            TODO SELECT FIRST AND CHECK IF ITS MY IP
//            TODO IF YES, CREATE BLOCK and BROADCAST IT, ELSE SKIP
            if (transactionList.get(0).getOwnerId().equals(this.address)) {
                int size = this.blockchain.size();
                Block block = null;
                try {
                    block = new Block((this.blockchain.get(size - 1).getId()),
                            this.blockchain.get(size - 1).getBlockHash(),
                            transactionList,
                            new Date().getTime());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                this.justCreatedBlock = block;
                broadcastNewBlock(this.address, block, true);
                this.transactionList = new ArrayList<>();
            }

        }

    }


    public void connectNeighbors() {
        List<String> neighbors = this.nodeDataStorage.getNeighbors();
        List<String> inactiveNeighbors = new LinkedList<>();

        // Connect to all neighbors and collect inactive ones
        for (String neighbor : neighbors) {
            String[] neighborAddress = neighbor.split(":");
            try {
                NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborAddress[0], Integer.parseInt(neighborAddress[1])).lookup(SERVICE_NAME);
                neighborNode.connect(this.address);
            } catch (Exception e) {
                inactiveNeighbors.add(neighbor);
//                System.out.println("Cannot connect to node " + neighbor + ".");
            }
        }

        // Remove inactive neighbors
        for (String inactiveNeighbor : inactiveNeighbors) {
            neighbors.remove(inactiveNeighbor);
        }

        // Store updated list of neighbors
        if (!inactiveNeighbors.isEmpty()) {
            try {
                this.nodeDataStorage.setNeighbors(neighbors);
            } catch (Exception e) {
                System.out.println("Cannot store list of neighbors.");
            }
        }

        for (String neighbor : this.nodeDataStorage.getNeighbors()) {
            String[] neighborAddress = neighbor.split(":");
            try {
                NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborAddress[0], Integer.parseInt(neighborAddress[1])).lookup(SERVICE_NAME);
                neighborNode.susedia(this.address, neighbors);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

    }

    @Override
    public void requestBlockchain(String address, boolean retry) throws RemoteException {
        String[] senderAddress = address.split(":");
        try {
            NodeConnector sender = (NodeConnector) LocateRegistry.getRegistry(senderAddress[0], Integer.parseInt(senderAddress[1])).lookup((SERVICE_NAME));
            sender.responseBlockchain(this.getBlockchain(), retry);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void responseBlockchain(List<Block> blockchain, boolean retry) throws RemoteException {
        int size1 = this.getNeighbors().size();
        this.numberOfBlockchainsRecieved++;

//        LONGEST BLOCKCHAIN
        if (blockchain.size() >= this.tmpBlockchain.size()) {
            this.tmpBlockchain = blockchain;
        }


        if (numberOfBlockchainsRecieved == size1) {
//            RESET AND SET NEW BLOCKCHAIN
            System.out.println("recieved block from all nodes");
            this.blockchain = this.tmpBlockchain;
            try {
                this.nodeDataStorage.setBlockchain(this.blockchain);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (retry) {
                System.out.println("im creator");
                if (transactionList.size() > 3) {
                    System.out.println("creating new block with updated blockchain");
                    int size = this.blockchain.size();
                    Block block = null;
                    try {
                        block = new Block((this.blockchain.get(size - 1).getId()),
                                this.blockchain.get(size - 1).getBlockHash(),
                                transactionList,
                                new Date().getTime());
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }

                    System.out.println("forwarding new block " + block.getId());
                    this.justCreatedBlock = block;
                    broadcastNewBlock(this.address, block, true);
                    this.transactionList = new ArrayList<>();
                    try {
                        this.TDStorage.rmTransactions();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.numberOfBlockchainsRecieved = 0;
            this.tmpBlockchain = new ArrayList<>();
        }
    }

    @Override
    public void broadcastNewBlock(String address, Block block, boolean forward) throws RemoteException {
        int size = this.blockchain.size();
        Block myBlock = null;
        try {
            myBlock = new Block(this.blockchain.get(size - 1).getId(),
                    this.blockchain.get(size - 1).getBlockHash(),
                    transactionList,
                    block.getTimestamp());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        if (!this.address.equals(address)) {
            String[] creatorAddress = address.split(":");
            try {
                NodeConnector creatorNode = (NodeConnector) LocateRegistry.getRegistry(creatorAddress[0], Integer.parseInt(creatorAddress[1])).lookup(SERVICE_NAME);
                boolean isValid = myBlock.getBlockHash().equals(block.getBlockHash()) && (myBlock.getId() == block.getId());
                System.out.println("validating " + Arrays.toString(creatorAddress) + "..." + isValid);
                creatorNode.validateBlock(isValid);
                if (!isValid) {
//                    request blockchain from all
                    for (String neighbor : this.nodeDataStorage.getNeighbors()) {
                        String[] neighborStrings = neighbor.split(":");
                        try {
                            NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                            neighborNode.requestBlockchain(this.address, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        if (forward) {
            for (String neighbor : this.nodeDataStorage.getNeighbors()) {

                // Do not forward back to sender node
                if (!neighbor.equals(address)) {
                    String[] neighborStrings = neighbor.split(":");
                    try {
                        NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                        neighborNode.broadcastNewBlock(this.address, block, false);
                        System.out.println("Forwarding block to node " + neighbor + ".");
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Cannot forward block to node " + neighbor + ".");
                    }
                }
            }

        }
    }

    @Override
    public void validateBlock(boolean valid) {
        int pocetSusedov = this.nodeDataStorage.getNeighbors().size();
        this.numberOfValidationCalls++;
        System.out.println(this.numberOfValidationCalls);
        this.isValid = this.isValid && valid;

        System.out.println(isValid);
        if (pocetSusedov == this.numberOfValidationCalls) {
            if (isValid) {
                this.numberOfValidationCalls = 0;
                this.blockchain.add(this.justCreatedBlock);
                this.justCreatedBlock = null;
                System.out.println("creating new block, deleting transaction list");
                try {
                    this.nodeDataStorage.setBlockchain(this.getBlockchain());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (String neighbor : this.nodeDataStorage.getNeighbors()) {
                    String[] neighborStrings = neighbor.split(":");
                    try {
                        NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                        neighborNode.saveBlock(this.getBlockchain());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                try {
                    this.TDStorage.rmTransactions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                this.justCreatedBlock = null;
                this.isValid = true;
                this.numberOfValidationCalls = 0;
                System.out.println("updating blockchain and will create new block");
                for (String neighbor : this.nodeDataStorage.getNeighbors()) {
                    String[] neighborStrings = neighbor.split(":");
                    try {
                        NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                        neighborNode.requestBlockchain(this.address, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            this.isValid = true;
            this.numberOfValidationCalls = 0;
        }
    }


    public void saveBlock(List<Block> blockchain) throws RemoteException {
        System.out.println("block is valid, saving ... " + blockchain.get(blockchain.size() - 1));
        this.blockchain.add(blockchain.get(blockchain.size() - 1));
        try {
            this.nodeDataStorage.setBlockchain(this.getBlockchain());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.transactionList = new ArrayList<>();
    }

}
