
package sma.restaurante.service;

import sma.restaurante.datasource.Jdbc;
import sma.restaurante.repository.Transaccion;

import java.util.Objects;

/**
 * Servicio encargado de gestionar las reservas de ingredientes
 * solicitadas por el AgentePedidos.
 *
 * La operación de reserva se ejecuta mediante una función
 * transaccional de PostgreSQL.
 */
public class ReservaService {

    private static final String SQL_RESERVAR_PEDIDO = """
            SELECT
                (r.resultado ->> 'exito')::boolean AS exito,
                r.resultado ->> 'mensaje' AS mensaje,
                NULLIF(r.resultado ->> 'id_pedido', '')::bigint AS id_pedido,
                r.resultado ->> 'estado' AS estado,
                COALESCE(
                    r.resultado -> 'ingredientes_faltantes',
                    '[]'::jsonb
                )::text AS ingredientes_faltantes
            FROM (
                SELECT inventario.fn_reservar_ingredientes_pedido(?)
                    AS resultado
            ) r
            """;

    private final Transaccion transaccion;

    public ReservaService(Transaccion transaccion) {
        this.transaccion = Objects.requireNonNull(
                transaccion,
                "La transacción no puede ser null"
        );
    }

    /**
     * Solicita a PostgreSQL reservar los ingredientes de un pedido.
     *
     * @param idPedido identificador del pedido
     * @return resultado de la operación de reserva
     */
    public ResultadoReserva reservarPedido(Long idPedido) {

        if (idPedido == null || idPedido <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del pedido debe ser positivo"
            );
        }

        return transaccion.ejecutar(() ->
                Jdbc.consultarUno(
                        SQL_RESERVAR_PEDIDO,
                        ps -> ps.setLong(1, idPedido),
                        rs -> {

                            boolean exito = rs.getBoolean("exito");

                            String mensaje = rs.getString("mensaje");

                            Long idPedidoResultado =
                                    Jdbc.leerLong(rs, "id_pedido");

                            String estado = rs.getString("estado");

                            String ingredientesFaltantes =
                                    rs.getString("ingredientes_faltantes");

                            return new ResultadoReserva(
                                    exito,
                                    mensaje,
                                    idPedidoResultado != null
                                            ? idPedidoResultado
                                            : idPedido,
                                    estado,
                                    ingredientesFaltantes
                            );
                        }
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "La función de reserva no devolvió resultados"
                        )
                )
        );
    }

    /**
     * Resultado devuelto por PostgreSQL después de intentar
     * reservar los ingredientes.
     *
     * ingredientesFaltantesJson contiene el arreglo JSONB
     * con los ingredientes que no tienen disponibilidad suficiente.
     */
    public record ResultadoReserva(
            boolean exito,
            String mensaje,
            Long idPedido,
            String estado,
            String ingredientesFaltantesJson
    ) {
    }
}
