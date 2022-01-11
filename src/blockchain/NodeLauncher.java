package blockchain;

import blockchain.node.Node;

public class NodeLauncher {

    public static Node node = null;  // We need a static reference, because GC deletes a Node object in automatic reference when main() is finished.

    public static void main(String[] args) {

        // Start node
        try {
            if (args.length == 1)
                node = new Node(args[0]);
            else
                System.out.println("Wrong arguments.");
        }
        catch (Exception e) {
            System.out.println(e);
            System.out.println("Cannot start node " + args[0]);
        }
    }
}
