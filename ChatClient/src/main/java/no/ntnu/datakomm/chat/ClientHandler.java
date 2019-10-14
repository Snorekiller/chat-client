//package no.ntnu.datakomm.chat;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.Socket;
//import java.util.List;
//
//public class ClientHandler extends Thread {
//    private final Socket clientSocket;
//    private String response;
//
//    public ClientHandler(Socket clientSocket) {
//        this.clientSocket = clientSocket;
//    }
//
//    @Override
//    public void run() {
//        InputStreamReader reader;
//        try {
//            do {
//                reader = new InputStreamReader(clientSocket.getInputStream());
//
//                BufferedReader bufferedReader = new BufferedReader(reader);
//
//                String clientInput = bufferedReader.readLine();
//                System.out.println("client sent " + clientInput);
//
//
//                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
//                writer.println(response);
//            } while (response != "game over" || response == null);
//            clientSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private List usersOnline(){
//
//    }
//
//    private void userNotLoggedIn(String requestedUsername){
//
//    }
//
//    private void checkIncommingCMD(String request) {
//        if (request != null) {
//                /*
//                splits the string recieved from the server into different parts so that the first part can be used as
//                the case, and the rest of the string can be used as a list or a message
//                 */
//            String[] parts = request.split(" ");
//            String firstPart = parts[0];
//
//            switch (firstPart) {
//
//                case "login":
//                    if (userNotLoggedIn(parts[0])) {
//                        System.out.println("login successful");
//                    } else {
//                        System.out.println("login unsuccessful");
//                    }
//                    break;
//
//                case "users":
//                    return usersOnline();
//                System.out.println("users recieved");
//                break;
//
//                case "msg":
//                    String sender = parts[1];
//                    System.out.println("message recieved");
//                    String publicMessage = request.replaceFirst("msg ", " ");
//                    publicMessage = publicMessage.replaceFirst(sender, " ");
//                    break;
//
//                case "privmsg":
//                    String privateSender = parts[1];
//                    System.out.println("private message recieved");
//                    String privateMessage = request.replaceFirst("privmsg ", " ");
//                    privateMessage = privateMessage.replaceFirst(privateSender, " ");
//                    break;
//
//                case "help":
//                    String helpCommands = request.replaceFirst("supported", " ");
//                    String[] helpCmd = helpCommands.split(" ");
//                    break;
//
//                default:
//                    //send cmderr til client
//                    break;
//            }
//
//        }
//    }
//}
