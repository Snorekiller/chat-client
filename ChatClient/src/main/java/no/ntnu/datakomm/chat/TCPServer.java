//package no.ntnu.datakomm.chat;
//
//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//
//public class TCPServer {
//    private static final int PORT = 1301;
//
//    public static void main(String[] args) {
//        TCPServer server = new TCPServer();
//        log("TCP servver starting");
//        server.run();
//        log("error the server should never go out of the run method after handling one client");
//    }
//
//    public void run() {
//        try {
//            ServerSocket welcomeSocket = new ServerSocket(PORT);
//            System.out.println("Server started on port " + PORT);
//
//            boolean mustRun = true;
//
//            while (mustRun) {
//                Socket clientSocket = welcomeSocket.accept();
//                ClientHandler clienthandler = new ClientHandler(clientSocket);
//                clienthandler.start();
//            }
//            welcomeSocket.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void log(String message){
//        System.out.println(message);
//    }
//}