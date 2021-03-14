package chat;

import java.io.*;
import java.net.*;
 
/**
 * Este hilo lee el mensaje del usuario para enviarlo al servidor
 * Este hilo NO termina hasta que el usuario introduzca '*'.
 */
public class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private AppCliente appCliente;
 
    public WriteThread(Socket socket, AppCliente appCliente) {
        this.socket = socket;
        this.appCliente = appCliente;

        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.out.println("Error en el OutputStream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
 
    public void run() {
         Console console = System.console();
 
        String name = console.readLine("\nIntroduce tu nick: ");
        appCliente.setUserName(name);
        writer.println(name);
 
        String message;
 
        do {
        	message = console.readLine("[" + name + "]: ");
            writer.println(message);
        } while (!message.equals("*") && !appCliente.closed);
        
        appCliente.closed = true;
 
        try {
            socket.close();
        } catch (IOException ex) {
//            System.out.println("Error writing to server: " + ex.getMessage());
        }
    }
}