package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtiendeCliente extends Thread {

    private final Socket socket;
    private final ComunHilos monit;
    private final int indice;

    private AtomicBoolean conexionAbierta;

    public AtiendeCliente(Socket socket, ComunHilos monit, int indiceEnElMonitor) {
        this.socket = socket;
        this.monit = monit;
        this.indice = indiceEnElMonitor;
        conexionAbierta = new AtomicBoolean(true);
    }

    @Override
    public void run() {
        try {
            new Thread() {
                private DataInputStream salidaDelSocket = new DataInputStream(socket.getInputStream());

                @Override
                public void run() {
                    while (conexionAbierta.get()) {
                        try {
                            String texto = salidaDelSocket.readUTF();

                            if (texto.equals("*")) {
                                conexionAbierta.set(false);
                            } else {
                                monit.put(texto);
                            }

                        } catch (InterruptedException | IOException e) {
                            conexionAbierta.set(false);
                        }
                    }
                    monit.retirarUsuario(indice);
                }
            }.start();

            DataOutputStream entradaDelSocket = new DataOutputStream(socket.getOutputStream());

            // primero mensajes antiguos
            for (String m : monit.getHistorial()) {
                if (conexionAbierta.get()) {
                    entradaDelSocket.writeUTF(m);
                } else {
                    break;
                }
            }

            while (conexionAbierta.get()) {
                // despues uno a uno los que vaya habiendo nuevos
                try {
                    String msj = monit.siguiente();
                    if (conexionAbierta.get()) {
                        entradaDelSocket.writeUTF(msj);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}