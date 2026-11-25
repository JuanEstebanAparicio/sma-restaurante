package sma.restaurante.service;

import sma.restaurante.datasource.Jdbc;
import sma.restaurante.repository.Transaccion;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
                )::text AS ingredientes_faltantes,
                COALESCE(
                    (
                        SELECT string_agg(elem ->> 'nombre', '|')
                        FROM jsonb_array_elements(
                            COALESCE(r.resultado -> 'ingredientes_faltantes', '[]'::jsonb)
                        ) AS elem
                    ),
                    ''
                ) AS nombres_faltantes
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
                            Long idPedidoResultado = Jdbc.leerLong(rs, "id_pedido");
                            String estado = rs.getString("estado");
                            String ingredientesFaltantesJson = rs.getString("ingredientes_faltantes");
                            List<String> ingredientesFaltantes = extraerNombres(rs.getString("nombres_faltantes"));

                            return new ResultadoReserva(
                                    exito,
                                    mensaje,
                                    idPedidoResultado != null ? idPedidoResultado : idPedido,
                                    estado,
                                    ingredientesFaltantesJson,
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

    private static List<String> extraerNombres(String nombresSeparados) {
        if (nombresSeparados == null || nombresSeparados.isBlank()) {
            return List.of();
        }
        return Arrays.stream(nombresSeparados.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public record ResultadoReserva(
            boolean exito,
            String mensaje,
            Long idPedido,
            String estado,
            String ingredientesFaltantesJson,
            List<String> ingredientesFaltantes
    ) {
    }
}