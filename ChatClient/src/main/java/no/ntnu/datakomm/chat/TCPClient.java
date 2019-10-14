package no.ntnu.datakomm.chat;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.net.*;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;
    private final static String PUBLICMESSAGECMD = "msg ";
    private final static String LOGINCMD = "login ";
    private final static String PRIVATEMSGCMD = "privmsg ";
    private final static String USERSCMD = "users";
    private final static String HELPCMD = "help";
    private String[] usersList = null;
    private String response = null;

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
        try {
            //connects to a hostsocket with ip address == host and portnumber == port
            connection = new Socket(host, port);

            // setup toserver so it can send information to a server
            toServer = new PrintWriter(connection.getOutputStream(), true);

            // setup fromserver so it can recieve information from server
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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
        if (isConnectionActive()) {
            try {
            //terminates the connection between the host socket and the local socket
            connection.close();
            // sets connection == null so when we test isConnectionActive, it will return null
            connection = null;
            onDisconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //tests if connection == null to check if connection.close() was successful, if unsuccessful it will try again.
        }
    }

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
        if (isConnectionActive()) {
            //sends a command to the server so the server knows what type of message is incoming
            toServer.print(cmd);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        if (sendCommand(PUBLICMESSAGECMD)) {
            //sends message to server
            toServer.println(message);
            //local testing for easy troubleshooting
            System.out.println("Message sent successfully");
            return true;
        } else {
            //local testing for easy troubleshooting
            System.out.println("Message not sent");
            return false;
        }

    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        if (sendCommand(LOGINCMD)) {
            //sends desired username to server
            toServer.println(username);
            //local testing for easy troubleshooting
            System.out.println("Login successful");
        } else {
            System.out.println("login unsuccessful");
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        if (sendCommand(USERSCMD)) {
            //sends a end line to the server so the server wont expect more information
            toServer.println(" ");
            System.out.println("asked for users");
        } else {
            System.out.println("failed to ask for users");
        }

    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        if (sendCommand(PRIVATEMSGCMD)) {
            //sends message to server
            toServer.println(recipient + " " + message);
            //local testing for easy troubleshooting
            System.out.println("Message sent successfully");
            return true;
        } else {
            //local testing for easy troubleshooting
            System.out.println("Message not sent");
            return false;
        }
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        if (sendCommand(HELPCMD)) {
            toServer.println(" ");
        }
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        String anwser = "error";
        try {
            //sets the anwser from the server as anwser
            anwser = fromServer.readLine();
            return anwser;
        } catch (IOException e) {
            e.printStackTrace();
            //calls the disconnect function to terminate the connection on exception
            disconnect();
            return anwser;

        }

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
        while (isConnectionActive()) {
            response = waitServerResponse();
            if (response != null) {
                /*
                splits the string recieved from the server into different parts so that the first part can be used as
                the case, and the rest of the string can be used as a list or a message
                 */
                String[] parts = response.split(" ");
                String firstPart = parts[0];

                switch (firstPart) {

                    case "loginok":
                        onLoginResult(true, "");
                        System.out.println("login successful");
                        break;

                    case "loginerr":
                        if (parts[1].equals("username")){
                            onLoginResult(false, "username is already in use");
                        } else if (parts[1].equals("incorrect")){
                            onLoginResult(false, "No special symbols allowed(including space)");
                        }

                        break;

                    case "users":
                        /*
                        removes the first element in the string so it doesnt show upp as a user in the client,
                        then it splits the string where there are a space, before it sets the userlist as onuserlist.
                         */
                        String cutString = response.replaceFirst("users ", " ");
                        usersList = cutString.split(" ");
                        onUsersList(usersList);
                        System.out.println("users recieved");
                        break;

                    case "msg":
                        String sender = parts[1];
                        System.out.println("message recieved");
                        String publicMessage = response.replaceFirst("msg ", " ");
                        publicMessage = publicMessage.replaceFirst(sender, " ");
                        onMsgReceived(false, sender, publicMessage);
                        break;

                    case "privmsg":
                        String privateSender = parts[1];
                        System.out.println("private message recieved");
                        String privateMessage = response.replaceFirst("privmsg ", " ");
                        privateMessage = privateMessage.replaceFirst(privateSender, " ");
                        onMsgReceived(true, privateSender, privateMessage);
                        break;

                    case "msgerr":
                        String msgError = response.replaceFirst("msgerr", "");
                        onMsgError(msgError);
                        break;

                    case "cmderr":
                        String cmdError = response.replaceFirst("cmderr", "");
                        onCmdError(cmdError);
                        break;

                    case "supported":
                        String helpCommands = response.replaceFirst("supported", " ");
                        String[] helpCmd = helpCommands.split(" ");
                        onSupported(helpCmd);
                        break;

                    default:
                        lastError=response;
                        break;
                }
            }
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
        for (ChatListener l : listeners) {
            l.onDisconnect();
        }
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        for (ChatListener l : listeners) {
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
        for (ChatListener l : listeners) {
            TextMessage textMessage = new TextMessage(sender, priv, text);
            l.onMessageReceived(textMessage);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        for (ChatListener l : listeners) {
            l.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        for (ChatListener l : listeners) {
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
        for (ChatListener l : listeners) {
            l.onSupportedCommands(commands);
        }
    }
}
