package chat;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * AppServer se inicializa primero y luego AppClient
 * El servidor recibira un mensaje y devolvera un string con texto adjunto
 */

public class AppServidor {

	// Constantes
	static final int PUERTO = 4444;
	static final int MAX_CONNECTIONS = 3;
	static final int MAX_HISTORY_MESSAGES = 100;
	
	private int port;
	
	public String[] usernames = new String[MAX_CONNECTIONS];
	public String[] history = new String[MAX_HISTORY_MESSAGES];
	ClientThread[] threads = new ClientThread[MAX_CONNECTIONS];
	static PrintStream stream = new PrintStream(System.out, true);
	
	public AppServidor (int port)
	{
		this.port = port;
	}
	
	@SuppressWarnings("resource")
	private void launchServer()
	{
		try {
			// Creating server socket
			ServerSocket server = new ServerSocket(port);

	        while (true) {
	        	// Wait for new connection entry and accept automatically
	        	Socket socketTcp = server.accept();
	            stream.println("Se ha conectado un usuario");

	            // Creating new thread to manage incoming messages from it 
	            ClientThread newUser = new ClientThread(socketTcp, this);
            	newUser.start();
	        }
		} catch (IOException e) {
			stream.println("Error en el servidor [Error]: " + e.getMessage());
		}
	}

	public static void main(String[] args) throws IOException {
		// Creating new server instance
		AppServidor server = new AppServidor(PUERTO);
		stream.println("Escuchando en el puerto [" + server.port + "]...");
		
		server.launchServer();
	}

	public int addNewUser(String name, ClientThread c) throws MaxConnections
	{
		for (int i=0; i < usernames.length; i++) {
			if (usernames[i] == null) {
				usernames[i] = name;
				threads[i] = c;
				return i;
			}
		}

		throw new MaxConnections();
	}
	
    /**
     * Send one message from one user to others in the list
     */
    public void sendToAll(String message, ClientThread clientThread) {
    	for (ClientThread thread : threads) {
            if (thread != null && thread.getPosition() != clientThread.getPosition()) {
            	thread.sendMessage(message);
            }	
    	}
    }

	/* Save new messages*/
    public void saveHistory(String message) {
    	boolean saved = false;
		for (int i=0; i < history.length; i++) {
        	if (history[i] == null) {
        		history[i] = message;
        		saved = true;
        		break;
        	}
        }
		
		/* 
		 * If we could not save the last message, we reset history and store 
		 * the new incoming messages
		 */
		if (!saved) {
			/* Initialize history */
			history = new String[history.length];
			/* Save message again */
			saveHistory(message);
		}
	}

	public void deleteUser(String name, ClientThread clientThread) {
        int p = clientThread.getPosition();
		if (usernames[p] != null) {
        	usernames[p] = null;
        	threads[p] = null;
        	stream.println(name + " ha abandonado el chat");
        }
	}
}
