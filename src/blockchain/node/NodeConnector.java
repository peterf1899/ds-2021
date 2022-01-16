package blockchain.node;

import blockchain.block.Block;
import blockchain.block.Transaction;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


public interface NodeConnector extends Remote{

    String SERVICE_NAME = "Node";

    void stop() throws RemoteException;
    void connect(String address) throws RemoteException;
    void susedia(String address, List<String> susedia) throws RemoteException;
    void createTransaction(String address, Transaction transaction, boolean forward) throws RemoteException;
    void broadcastNewBlock(String address, Block block, boolean forward) throws RemoteException, NoSuchAlgorithmException;
    void validateBlock(boolean equals) throws RemoteException;
    List<Block> getBlockchain() throws RemoteException;
    List<Transaction> getTransactionList() throws RemoteException;
    List<String> getNeighbors() throws RemoteException;
    void requestBlockchain(String address, boolean retry) throws RemoteException;
    void responseBlockchain(List<Block> blockchain, boolean retry) throws RemoteException;
    void saveBlock(List<Block> blockchain) throws RemoteException;
}
