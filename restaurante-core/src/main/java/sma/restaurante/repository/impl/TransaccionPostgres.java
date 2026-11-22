package sma.restaurante.repository.impl;

import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.repository.Transaccion;

import java.util.function.Supplier;

public class TransaccionPostgres implements Transaccion {

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return DatabaseConnection.enTransaccion(trabajo);
    }
}