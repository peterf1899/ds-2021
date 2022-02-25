package blockchain.node;

import blockchain.block.Block;
import blockchain.block.Transaction;
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
    private List<Transaction> transactionList;
    private List<Block> blockchain;
    private int numberOfValidationCalls = 0;
    private int numberOfBlockchainsRecieved = 0;
    private int numberOfTransactions = 0;
    private boolean isValid = true;
    private List<Block> tmpBlockchain = new ArrayList<>();
    private List<Transaction> tmpTransactions = new ArrayList<>();
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
        this.blockchain = this.nodeDataStorage.getBlockchain();

        if(this.nodeDataStorage.getNeighbors().size() == 0){
            System.out.println("i have no neighbours, keeping my persisted transaction list and blockchain");
            this.transactionList = this.nodeDataStorage.getTransactions();
            this.blockchain = this.nodeDataStorage.getBlockchain();
        }else{
            System.out.println("i have neighbours, im honest, dropping my transaction list and blockchain, will sync");
            this.nodeDataStorage.setTransactions(new ArrayList<>());
            this.nodeDataStorage.setBlockchain(new ArrayList<>());
            this.synchronize();
        }


    }

    @Override
    public String getAddress() throws RemoteException {
        return address;
    }

    @Override
    public void responseTransaction(List<Transaction> transactions) throws RemoteException {
        int size1 = this.getNeighbors().size();
        this.numberOfTransactions++;

        if (transactions.size() >= this.tmpTransactions.size()) this.tmpTransactions = transactions;

        if (numberOfTransactions == size1) {
            System.out.print("recieved transacations from all blocks, evalutating... ");
            this.transactionList = this.tmpTransactions;
            try {
                this.nodeDataStorage.setTransactions(this.getTransactionList());
                System.out.print("Done");
                System.out.println();
            } catch (Exception e) {
                System.out.println("Something went wrong during transaction sync");
               e.printStackTrace();
            }
            this.numberOfBlockchainsRecieved = 0;
            this.tmpBlockchain = new ArrayList<>();
        }
    }

    private void synchronize(){
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (String neighbor : this.nodeDataStorage.getNeighbors()) {

            // Do not forward back to sender node
            if (!neighbor.equals(address)) {
                String[] neighborStrings = neighbor.split(":");
                try {
                    NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
//                    neighborNode.requestTransactions(this.address);
                    neighborNode.requestBlockchain(this.address, false);
                } catch (Exception e) {
                    System.out.println(e);

                }
            }
        }


            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        for (String neighbor : this.nodeDataStorage.getNeighbors()) {

            // Do not forward back to sender node
            if (!neighbor.equals(address)) {
                String[] neighborStrings = neighbor.split(":");
                try {
                    NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                    neighborNode.requestTransactions(this.address);
//                    neighborNode.requestBlockchain(this.address, false);
                } catch (Exception e) {
                    System.out.println(e);

                }
            }
        }
    }


    @Override
    public void requestTransactions(String address) throws RemoteException {
        String[] senderAddress = address.split(":");
        try {
            NodeConnector sender = (NodeConnector) LocateRegistry.getRegistry(senderAddress[0], Integer.parseInt(senderAddress[1])).lookup((SERVICE_NAME));
            sender.responseTransaction(this.nodeDataStorage.getTransactions());
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
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
        // Forward transaction to all neighbors
        if (forward) {
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

        this.transactionList.add(transaction);
        try {
            this.nodeDataStorage.setTransactions(this.getTransactionList());
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (transactionList.size() > 3 && transactionList.get(0).getOwnerId().equals(this.address)) {
//
//            int size = this.blockchain.size();
//            Block block = null;
//            try {
//                block = new Block((this.blockchain.get(size - 1).getId()),
//                        this.blockchain.get(size - 1).getBlockHash(),
//                        transactionList,
//                        new Date().getTime());
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            }
//
//            this.justCreatedBlock = block;
//            broadcastNewBlock(this.address, block, true);
//            this.transactionList = new ArrayList<>();
//        }
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
            System.out.print("recieved blockchain from all nodes, evaluting... ");
            this.blockchain = this.tmpBlockchain;
            try {
                this.nodeDataStorage.setBlockchain(this.blockchain);
                System.out.print("Done");
                System.out.println();
            } catch (Exception e) {
                System.out.println("Something went wrong during blockchain sync");
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
//                    try {
//                        this.TDStorage.rmTransactions();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    try {
                        this.nodeDataStorage.setTransactions(this.getTransactionList());
                    } catch (Exception e) {
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
                System.out.println("Validating block from " + Arrays.toString(creatorAddress) + " ... " + isValid);
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
//        System.out.println(this.numberOfValidationCalls);
        this.isValid = this.isValid && valid;


        if (pocetSusedov == this.numberOfValidationCalls) {
            if (isValid) {
                System.out.println("Block is valid, proceding...");
                this.numberOfValidationCalls = 0;
                this.blockchain.add(this.justCreatedBlock);
                this.justCreatedBlock = null;
                System.out.print("Saving block to blockchain ...");
                try {
                    this.nodeDataStorage.setBlockchain(this.getBlockchain());
                    System.out.print(" Done");
                    System.out.println();
                } catch (Exception e) {
                    System.out.println("Something went wrong while saving blockchain");
                    e.printStackTrace();
                }

                System.out.println("Sending message to "+ this.nodeDataStorage.getNeighbors().size()+" neighbours to save new block");
                for (String neighbor : this.nodeDataStorage.getNeighbors()) {
                    String[] neighborStrings = neighbor.split(":");
                    try {
                        NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(neighborStrings[0], Integer.parseInt(neighborStrings[1])).lookup(SERVICE_NAME);
                        neighborNode.saveBlock(this.getBlockchain());
                        System.out.println(neighbor+"... Done");
                    } catch (Exception e) {
                        System.out.println("Something went wrong sending save message to "+ neighbor);
                        e.printStackTrace();
                    }

                }

                System.out.print("Clearing transactions...");
                this.transactionList = new ArrayList<>();
                try {
                    this.nodeDataStorage.setTransactions(this.getTransactionList());
                    System.out.println(" Done");
                } catch (Exception e) {
                    System.out.println("Something went wrong while clearing transactions");
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
        System.out.print("block is valid, saving... " + blockchain.get(blockchain.size() - 1));
        this.blockchain.add(blockchain.get(blockchain.size() - 1));
        try {
            this.nodeDataStorage.setBlockchain(this.getBlockchain());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.transactionList = new ArrayList<>();
        try {
            this.nodeDataStorage.setTransactions(this.getTransactionList());
            System.out.print(" Done");
            System.out.println();
        } catch (Exception e) {
            System.out.println("Something went wrong while clearing transaction list after saving new block");
            e.printStackTrace();
        }
    }

    @Override
    public void createNewBlock() throws RemoteException {
//      loop through transacion list
        for (Transaction t: this.transactionList){
//            check if im creator
            if(t.getOwnerId().equals(this.address)){
                this.newBlock();
                break;
            }else{
//              who created transaction
                String creator = t.getOwnerId();

//              check if creator of transacion is available
                if(this.getNeighbors().contains(creator)){
                    //tell him to create block
                    try {
                        NodeConnector neighborNode = (NodeConnector) LocateRegistry.getRegistry(creator.split(":")[0], Integer.parseInt(creator.split(":")[1])).lookup(SERVICE_NAME);
                        neighborNode.newBlock();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void newBlock() throws RemoteException {
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
        this.broadcastNewBlock(this.address, block, true);
    }
}
