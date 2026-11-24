package sma.restaurante.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Hora del restaurante. El dominio trabaja en hora local de Bogota y la base de datos
 * guarda instantes absolutos (TIMESTAMPTZ), asi nada depende de la zona de la maquina o de la sesion.
 */
public final class Tiempo {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Tiempo() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static OffsetDateTime aInstante(LocalDateTime local) {
        return local == null ? null : local.atZone(ZONA).toOffsetDateTime();
    }

    public static LocalDateTime aLocal(OffsetDateTime instante) {
        return instante == null ? null : instante.atZoneSameInstant(ZONA).toLocalDateTime();
    }
}