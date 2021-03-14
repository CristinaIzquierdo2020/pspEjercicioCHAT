package chat;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * AppServer debe iniciarse primero y luego AppClient
 * Se envian mensajes al servidor y se espera una respuesta
 */
public class AppCliente {
	public static final int DEFAULT_PORT = 4444;
	public static final String DEFAULT_IP = "localhost";
	
	String ip = "localhost";
	int port;
	private String userName;
	public boolean closed = true;

	public AppCliente (String ip, int port)
	{
		this.ip = ip;
		this.port = port;
	}
	
    public void setUserName(String userName) {
        this.userName = userName;
    }
 
    public String getUserName() {
        return this.userName;
    }
  
	private void launchClient()
	{
		try {
			Socket socketTcp = new Socket(ip, port);
			System.out.println("Conectando al servidor: " + socketTcp);
			closed = false;
 
			/* One thread to read */
			new ReadThread(socketTcp, this).start();
			/* One thread to send, avoiding blocks waiting for server messages */
			new WriteThread(socketTcp, this).start();
		} catch (UnknownHostException e) {
			System.out.println("Error en el servidor: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("I/O [Error]: " + e.getMessage());
		}
	}
  
	public static void main(String[] args) throws IOException 
	{
		/* Se inicializa AppCliente con el puerto por defecto */
		AppCliente app = new AppCliente(DEFAULT_IP, DEFAULT_PORT);
		app.launchClient();
  }
}
