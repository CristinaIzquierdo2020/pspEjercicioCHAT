
import java.util.concurrent.locks.Condition;

public class MutexArrayMensajes extends ConcurrentCircularBuffer<String> {

    private final Condition HAY_MENSAJE_NUEVO;

    public MutexArrayMensajes(final int maxSize) {
        super(maxSize);
        HAY_MENSAJE_NUEVO = LOCK.newCondition();
    }

    /**
     * Bloquea hasta que llegue un mensaje nuevo. Entonces, adquiere el mutex y
     * devuelve el mensaje.
     */
    public String siguiente() {
        try {
            LOCK.lock();
            HAY_MENSAJE_NUEVO.await();
            return (String) Q[(lastFree == 0 ? MAX_SIZE : lastFree) - 1];
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    protected void nq(String e) {
        super.nq(e);
        HAY_MENSAJE_NUEVO.signalAll();
    }
    
}
