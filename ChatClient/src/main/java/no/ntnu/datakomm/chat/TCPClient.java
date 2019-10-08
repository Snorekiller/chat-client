package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        // TODO Step 1: implement this method


        try {
            //Opens new socket

            connection = new Socket(host, port);

            System.out.println("Successfully connected!");

            //Opens an output stream to the server
            toServer = new PrintWriter(connection.getOutputStream(), true);

            //Opens an "input stream" from the server
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return true;

        } catch (IOException e) {
            System.out.println("ERROR: "+ e.getMessage());
            return false;
        }


        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        // TODO Step 4: implement this method
        // Hint: remember to check if connection is active

        //checks if connection is active and closes + sets socket to null to avoid that methods tries to call on the closed socket
        if(isConnectionActive()) {
            try {
                connection.close();
                connection = null;

            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("IOExeption in isConnectionActive method ");
            }
            onDisconnect();
        }
    }


    //Beneftits from socket being set to null on disconnect cause it will be true even if socket is closed, but not if it is null
    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // TODO Step 2: Implement this method

        // Checks if socket is open and prints the command to the server if it is


        if(isConnectionActive()) {
            toServer.print(cmd);
            return true;
        }
        else {
            System.out.println("Socket is closed");
            return false;
        }


        // Hint: Remember to check if connection is active

    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // TODO Step 2: implement this method

        // Calls on "sendCommand to send "msg " command, and sends the message after
        if (sendCommand("msg ")) {
            toServer.println(message);
            System.out.println("Message sent successfully!");
            return true;
        }
        else {
            System.out.println("Message not sent");
            return false;
        }

        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.

    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // TODO Step 3: implement this method

        //Sends a username to the server for check
        if(sendCommand("login ")){
            toServer.println(username);
            System.out.println("Username sent to server");
        }
        else {
            System.out.println("Username was not sent to server");
        }

        // Hint: Reuse sendCommand() method
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // TODO Step 5: implement this method

        // Sends the command "users" to the sendCommand method
        if(sendCommand("users")){
            //this one is needed cause there is no println in sendCommand
            toServer.println("");
            System.out.println("Asked server for user list");
        }

//            toServer.println("users ");

        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // TODO Step 6: Implement this method

        //makes a string of command and the recipient, and then adds " " and message after
        if(sendCommand("privmsg " + recipient)){
            toServer.println(" " + message);
            return true;
        }

        System.out.println("Could not send a PM");
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        return false;
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // TODO Step 3: Implement this method

            String answer = "error";

            // Reads answer from server and returns it as a string
            // Should it not get an answer it returns "error", which will be handled by parseIncomingCommands()
            try {
                answer = fromServer.readLine();
                return answer;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                //e.printStackTrace();
                //disconnect();


                return "";
            }

        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        // didn't do this, let's wait for the problem to introduce itself

    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        //while (!connection.isClosed()) {
        while (isConnectionActive()) {
            // TODO Step 3: Implement this method

            //Switch case calls on method to display an error message if username is already taken
            //Or if no answer is recieved from the server after trying to log in
/*
            //Create a string from the response from server
            String switchString = waitServerResponse();
            //String userListString = switchString;

            //Create a string to split up in order to make an array
            String stringToSplit = switchString;

            //replace the first word with an empty string, this is so that "users" will not appear as a user
            stringToSplit = stringToSplit.replaceFirst("users ", "");

            //Create the array that contains all the users
            String[] tempArrayUserList;

            //Declare the symbol that means the string should be split
            String delimiter = " ";

            // given string will be split by the argument delimiter provided.
            tempArrayUserList = stringToSplit.split(delimiter);



            if (switchString.contains("users")){
                switchString = "users ";
            }
            if (tempArrayUserList[0] == "msg"){
                switchString = "msg";
            }
*/
            String sender;

            String response = waitServerResponse();

            String[] recievedWords;

            recievedWords = response.split(" ");

            switch (recievedWords[0]){
                case "loginok":
                    onLoginResult(true, "");
                    System.out.println("Server responded with loginok");
                    break;
                case "loginerr":

                    if (recievedWords[1].equals("username")){
                        onLoginResult(false, "Username already in use");
                        System.out.println("Username already in use");
                    }

                    else if (recievedWords[1].equals("incorrect")){
                        onLoginResult(false, "No special symbols (includes space)");
                        System.out.println("Username must not contain special symbols");
                    }
                    break;
                case "users":
                    //System.out.println(userListString);

                    String forRemoveCommand = response.replaceFirst("users ", "");
                    String[] userList = forRemoveCommand.split(" ");

                    onUsersList(userList);
                    for (int i = 0; i < recievedWords.length; i++){
                        System.out.println(recievedWords[i]);
                    }
                    break;
                case "msg":
                    System.out.println("recieved message command from server");

                    sender = recievedWords[1];
                    String message = response.replaceFirst("msg ", "");
                    message = message.replaceFirst(sender, "");
                    onMsgReceived(false,sender ,message);
                    break;
                case "privmsg":
                    System.out.println("recieved private message command from server");

                    sender = recievedWords[1];
                    String privateMessage = response.replaceFirst("privmsg", "");
                    privateMessage = privateMessage.replaceFirst(sender, "");
                    onMsgReceived(true, sender, privateMessage);

                    break;
                case "msgerr":
                    System.out.println("there was an error in sending the last message from client");

                    onMsgError(response);
                    break;
                case "cmderr":
                    System.out.println("The server did not understaand the command");

                    onCmdError(response);
                    break;

                default:
                    System.out.println(response);


            }

            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method

            // TODO Step 5: update this method, handle user-list response from the server
            // Hint: In Step 5 reuse onUserList() method

            // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
            // TODO Step 7: add support for incoming message errors (type: msgerr)
            // TODO Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners

            // TODO Step 8: add support for incoming supported command list (type: supported)

        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // TODO Step 4: Implement this method

        for (ChatListener l : listeners) {
            l.onDisconnect();
        }

        // Hint: all the onXXX() methods will be similar to onLoginResult()
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        // TODO Step 5: Implement this method

        for (ChatListener l : listeners){
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        // TODO Step 7: Implement this method

        TextMessage message = new TextMessage(sender, priv, text);
        for (ChatListener l : listeners){

            l.onMessageReceived(message);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // TODO Step 7: Implement this method


        for (ChatListener l : listeners){
            l.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method

        for (ChatListener l : listeners){
            l.onCommandError(errMsg);
        }
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
    }
}
