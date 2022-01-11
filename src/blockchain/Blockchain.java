package blockchain;

import blockchain.block.Block;
import blockchain.block.Transaction;
import blockchain.node.NodeConnector;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Blockchain {


    public static void main(String[] args) {
        System.out.println("Blockchain");

        Scanner scanner = new Scanner(System.in);
        String address = null;
        NodeConnector node = null;

        boolean working = true;



        // User interface commands interpreter loop
        while (working) {
            // Read command
            System.out.print("[" + (address == null ? "not connected" : address) + "]> ");
            String command = scanner.nextLine().trim();

            // Command connect
            if (command.startsWith("connect")) {
                address = command.substring("connect".length()).trim();
                String[] addressArray = address.split(":");
                try {
                    node = (NodeConnector) LocateRegistry.getRegistry(addressArray[0], Integer.parseInt(addressArray[1])).lookup(NodeConnector.SERVICE_NAME);
                    System.out.println("Connected to node " + address + ".");
                } catch (Exception e) {
                    System.out.println("Cannot connect to node " + address + ".");
                    address = null;
                    node = null;
                }
            }

            // Command disconnect
            else if (command.equals("disconnect")) {
                if (node == null) {
                    System.out.println("Not connected to a node.");
                }
                else {
                    System.out.println("Disconnected from node " + address + ".");
                    address = null;
                    node = null;
                }
            }

            // Command stop
            else if (command.equals("stop")) {
                if (node == null) {
                    System.out.println("Not connected to a node.");
                }
                else {
                    try {
                        node.stop();
                        System.out.println("Node " + address + " is stopped.");
                        address = null;
                        node = null;
                    }
                    catch (Exception e) {
                        System.out.println("Cannot stop node " + address + ".");
                    }
                }

            }else if (command.startsWith("t")){
                String text = command.substring("t".length()).trim();
                if (node == null) {
                    System.out.println("Not connected to a node.");
                }
                else {
                    try {
                        Transaction transaction = new Transaction(address, new Date().getTime(), text);
                        System.out.println(transaction);
                        node.createTransaction(null, transaction, true);
                        System.out.println("transaction \"" + text + "\" from node " + address + ".");
                    }
                    catch (Exception e) {
                        System.out.println(e);
                        System.out.println("Cannot broadcast text from node " + address + ".");
                    }
                }
            }

            else if(command.equals("ls-block")){
                if (node == null) {
                    System.out.println("Not connected to a node.");
                }else{
                    try {
                        List<Block> blockchain= node.getBlockchain();

                        for (Block block : blockchain){
                            System.out.println("--------------------");
                            System.out.println("Id: "+ block.getId());
                            System.out.println("Date: " + new java.util.Date((block.getTimestamp())));
                            System.out.println("Previous hash: "+block.getPreviousHash());
                            System.out.println("Block hash: " +block.getBlockHash());
                            System.out.println("Transactions: ");
                            for (Transaction transaction : block.getData()){
                                transaction.printTransaction();
                            }
                            System.out.println();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }


            }

            else if(command.equals("ls-trans")){
                if (node == null) {
                    System.out.println("Not connected to a node.");
                }else{
                    try {
                        List<Transaction> transactions = node.getTransactionList();
                        if(transactions.size() == 0){
                            System.out.println("No transactions yet");
                        }else{
                            for(Transaction transaction : transactions){
                                transaction.printTransaction();
                            }
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            else if(command.equals("ls-peers")){
                if (node == null) {
                    System.out.println("Not connected to a node.");
                }else{
                    try {
                        List<String> peers = node.getNeighbors();
                        if(peers.size() == 0){
                            System.out.println("I dont have any peers");
                        }else{
                            System.out.print("My peers: ");
                            for (String peer : peers){
                                System.out.print(peer + ", ");
                            }
                            System.out.println();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            // Command exit
            else if (command.equals("exit")) {
                System.out.println("Good bye!");
                working = false;
            }

            // Command help
            else if (command.equals("help")) {
                System.out.println("connect <address> - Connect to a node at specified <address>.");
                System.out.println("disconnect - Disconnect from connected node.");
                System.out.println("stop - Stop connected node.");
                System.out.println("transaction <transaction> - Create transaction");
                System.out.println("ls-block - list blockchain of the node");
                System.out.println("ls-trans - list transactions of the node");
                System.out.println("ls-peers - list all peers in the peer to peer network");
                System.out.println("exit - Exit application.");
                System.out.println("help - This help text.");
            }

            // Unknown command
            else {
                System.out.println("Command is unknown.");
            }
        }
    }

}
