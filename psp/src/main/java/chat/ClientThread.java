package chat;

import java.io.*;
import java.net.*;
//import java.util.*;


/**
 * Este hilo administra cada cliente conectado con un nuevo hilo, por lo que
 * el servidor puede controlar más de un cliente a la vez.
 */
public class ClientThread extends Thread {
    private Socket socket;
    private AppServidor server;
    private PrintWriter writer;
    private BufferedReader reader;
    private int position;
    static PrintStream stream = new PrintStream(System.out, true);
 
	public ClientThread(Socket socket, AppServidor server) {
        this.socket = socket;
        this.server = server;
        init();
    }
	
	public void init()
	{
		try {
			InputStream input = socket.getInputStream();
	        reader = new BufferedReader(new InputStreamReader(input));

	        OutputStream output = socket.getOutputStream();
	        writer = new PrintWriter(output, true);
		} catch (IOException e) {
			stream.println("==> ClientThread init [Error]: " + e.getMessage());
		}
	}
	
	public int getPosition() {
		return position;
	}

    public void setPosition(int position) {
	this.position = position;
    }
	
    private synchronized int addUser(String name) throws MaxConnections
    {
    	return server.addNewUser(name, this);
    }
 
    public void run() {
        try {
            printUsers();
            printHistory();
 
            String name = reader.readLine();
            position = addUser(name);
 
            this.sendMessage("SERVER_CONNECTION_ACCEPTED");
            
            String serverMessage = "Nuevo usuario: " + name;
            server.sendToAll(serverMessage, this);
            stream.println(serverMessage);
 
            String clientMessage;
 
            do {
                clientMessage = reader.readLine();
                
                /* Avoid sending quit messages to the rest of users */
                if (!clientMessage.equals("*")) {
                	serverMessage = "[" + name + "]: " + clientMessage;
                    server.sendToAll(serverMessage, this);	
                    server.saveHistory(serverMessage);
                }
            } while (!clientMessage.equals("*"));
 
            server.deleteUser(name, this);
            socket.close();
 
            serverMessage = name + " se ha desconectado.";
            server.sendToAll(serverMessage, this);
 
        } catch (IOException e) {
        	stream.println("==> ClientThread[Error]: " + e.getMessage());
        } catch (MaxConnections e) {
        	this.sendMessage("CONNECTION_REJECTED_MAX_CONNECTIONS_REACHED");
        	stream.println("==> Server [Error]: " + e.getMessage());
        }
    }
 
    private void printHistory() {
    	if (server.history.length > 0) {
        	String text = "";
        	
        	for (int i = 0; i < server.history.length; i++) {
        		if (server.history[i] != null) {
        			text += server.history[i] + '\n';
        		}
        	}
        	
        	if (!text.equals("")) {
        		writer.println("Historial de mensajes \n"+ text);
        	}
        	
    	}
	}

	/**
     * Envía una lista de usuarios online al usuario recién conectado.
     */
    void printUsers() {
    	int n = 0;
        if (server.usernames.length > 0) {
        	String usernames = "";
        	if (server.usernames[0] != null) {
        		usernames = server.usernames[0];
        		n++;
        	}

        	for (int i = 1; i < server.usernames.length; i++) { 
        		if (server.usernames[i] != null) {
        			usernames += ", " + server.usernames[i];
        			n++;
        		}
        	}
        	
        	if (n>0) {
        		writer.println("Usuario conectados: ["+ usernames +"]");
        	} else {
        		writer.println("No hay más usuarios conectados");
        	}
        } else {
            writer.println("No hay otros usuarios conectados ahora mismo");
        }
    }
 
    /**
     * Sends a message to the client.
     */
    void sendMessage(String message) {
        writer.println(message);
    }
}
