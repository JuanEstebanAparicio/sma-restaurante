package sma.restaurante.memoria;

import sma.restaurante.repository.Transaccion;

import java.util.function.Supplier;

/** Sin base de datos no hay nada que revertir: solo ejecuta y cuenta las veces que se uso. */
public class TransaccionMemoria implements Transaccion {

    private int ejecuciones;

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        ejecuciones++;
        return trabajo.get();
    }

    public int ejecuciones() {
        return ejecuciones;
    }
}