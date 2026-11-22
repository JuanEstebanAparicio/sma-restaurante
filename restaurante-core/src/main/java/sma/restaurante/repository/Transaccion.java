package sma.restaurante.repository;

import java.util.function.Supplier;

/** Ejecuta varias operaciones de repositorio como una sola unidad: todo o nada. */
public interface Transaccion {

    <T> T ejecutar(Supplier<T> trabajo);
}