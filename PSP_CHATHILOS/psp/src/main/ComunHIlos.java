package chat;

import java.net.Socket;

public class ComunHilos {

    private MutexArrayMensajes historial;

    private final Thread[] usuarios;
    private int cantidad = 0;
    private final Object monitorUsuarios = new Object();

    public ComunHilos(final int MAX_USUARIOS, final int MAX_MENSAJES) {
        historial = new MutexArrayMensajes(MAX_MENSAJES);
        usuarios = new Thread[MAX_USUARIOS];
    }

    public String siguiente() {
        return historial.siguiente();
    }

    public void put(final String m) throws InterruptedException {
        historial.put(m);
    }

    public MutexArrayMensajes getHistorial() {
        return historial;
    }

    public void nuevoUsuario(Socket s) {
        synchronized (monitorUsuarios) {
            if (cantidad < usuarios.length) {
                usuarios[cantidad] = new AtiendeCliente(s, this, cantidad);
                usuarios[cantidad++].start();
                System.out.println("Se ha unido un usuario, ahora hay " + cantidad);
            }
        }
    }

    public void retirarUsuario(int i) {
        synchronized (monitorUsuarios) {
            if (i >= 0 && i < usuarios.length) {
                usuarios[i] = usuarios[--cantidad];
                System.out.println("Se ha ido un usuario, ahora quedan " + cantidad);
            }
        }
    }

    public int numUsuarios() {
        return cantidad;
    }