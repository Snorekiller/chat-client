package no.ntnu.datakomm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A Simple TCP client, used as a warm-up exercise for assignment A4.
 */
public class SimpleTcpServer {
    private static final int PORT = 1301;

    public static void main(String[] args) {
        SimpleTcpServer server = new SimpleTcpServer();
        log("Simple TCP server starting");
        server.run();
        log("ERROR: the server should never go out of the run() method! After handling one client");
    }

    public void run() {
        // TODO - implement the logic of the server, according to the protocol.
        // Take a look at the tutorial to understand the basic blocks: creating a listening socket,
        // accepting the next client connection, sending and receiving messages and closing the connection
        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            boolean mustRun = true;

            while (mustRun) {
                Socket clientSocket = welcomeSocket.accept();
                ClienHandler clienthandler = new ClienHandler(clientSocket);
                clienthandler.start();
            }
            welcomeSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message) {
        System.out.println(message);
    }
}
