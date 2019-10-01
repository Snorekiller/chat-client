package no.ntnu.datakomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienHandler extends Thread {
    private final Socket clientSocket;

    public ClienHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        InputStreamReader reader = null;
        try {
          do {
              reader = new InputStreamReader(clientSocket.getInputStream());


            BufferedReader bufReader = new BufferedReader(reader);

            String clientInput = bufReader.readLine();
            if (clientInput == "game over") {
                clientSocket.close();
            }
            System.out.println("Client sent: " + clientInput);
            String[] parts = clientInput.split(" ");


            String response;

            if (parts.length == 2) {
                response = parts[0] + " " + parts[1].toUpperCase();
            } else {
                response = "error";
            }

            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println(response);
          } while (clientSocket.isConnected());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
