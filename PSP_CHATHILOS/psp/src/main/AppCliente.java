import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppCliente {

    public static void nuevo(final String IP, final int PUERTO) {

        AtomicBoolean conexionAbierta = new AtomicBoolean(true);

        System.out.print("Nombre de usuario: ");
        String nombreUsuario;

        try (Socket socketTcp = new Socket(IP, PUERTO); Scanner s = new Scanner(System.in)) {

            do {
                nombreUsuario = s.nextLine();
            } while (nombreUsuario.isEmpty());

            new Thread() { // AtiendeServidor
                @Override
                public void run() {
                    DataInputStream outputServidor;
                    try {
                        outputServidor = new DataInputStream(socketTcp.getInputStream());
                        while (conexionAbierta.get()) {
                            String mensajeDelServidor = outputServidor.readUTF();
                            System.out.println(mensajeDelServidor);
                        }
                    } catch (IOException e) {
                        // conexion cerrada
                        conexionAbierta.set(false);
                    }

                }
            }.start();

            // Obtenemos canal de salida
            DataOutputStream inputServidor = new DataOutputStream(socketTcp.getOutputStream());

            // Enviamos un mensaje y esperamos la respuesta del servidor
            String linea;
            while (!(linea = s.nextLine()).equals("*")) {
                inputServidor.writeUTF("[" + nombreUsuario + "]: " + linea);
            }
            conexionAbierta.set(false);

            // Cerrar Conexion
            inputServidor.writeUTF(linea);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AppCliente.nuevo("localhost", 6969);
    }
}