package restaurante;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import pedidos.DataAccessObject.PedidoDAO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RestauranteMain {

    private static final PedidoDAO pedidoDAO = new PedidoDAO();

    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "false");

            AgentContainer mainContainer = rt.createMainContainer(profile);
            if (mainContainer == null) {
                System.err.println("[RestauranteMain] No se pudo iniciar el Main-Container porque el puerto 1099 ya está en uso.");
                System.err.println("[RestauranteMain] Detén las ejecuciones anteriores de JADE (App / RestauranteMain) e inténtalo de nuevo.");
                return;
            }

            java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    mainContainer.kill();
                    rt.shutDown();
                } catch (Exception ignored) {
                }
            }));

            System.out.println("=================================================================");
            System.out.println("  INICIANDO PLATAFORMA MULTIAGENTE SMA-RESTAURANTE (4 AGENTES)");
            System.out.println("=================================================================");

            // AgenteInventario (módulo restaurante-core)
            iniciarAgenteSiExiste(mainContainer, "inventario", "sma.restaurante.agent.AgenteInventario");

            // AgenteCocina (módulo restaurante-core)
            iniciarAgenteSiExiste(mainContainer, "AgenteCocina", "sma.restaurante.agent.AgenteCocina");

            //  AgentePedidos (módulo Agente_Pedidos)
            mainContainer.createNewAgent("AgentePedidos", "pedidos.Agente.AgentePedidos", null).start();

            // AgenteCuentaPago (módulo Agente_Pedidos)
            mainContainer.createNewAgent("AgenteCuentaPago", "pedidos.Agente.AgenteCuentaPago", null).start();

            // Agente auxiliar de consola para procesar cobros
            mainContainer.createNewAgent("ConsolaCaja", AgenteConsolaCaja.class.getName(), null).start();

            Thread.sleep(1500);
            ejecutarMenuConsola();

        } catch (Exception e) {
            System.err.println("[RestauranteMain] Error iniciando el sistema: " + e.getMessage());
        }
    }

    private static void iniciarAgenteSiExiste(AgentContainer container, String nombreLocal, String claseFqcn) {
        try {
            Class.forName(claseFqcn);
            AgentController ac = container.createNewAgent(nombreLocal, claseFqcn, null);
            ac.start();
            System.out.println("[RestauranteMain] Agente '" + nombreLocal + "' iniciado (" + claseFqcn + ")");
        } catch (ClassNotFoundException e) {
            System.err.println("[RestauranteMain] ADVERTENCIA: No se encontró " + claseFqcn
                    + ". Agrega 'restaurante-core' en Project Structure -> Modules -> modulo-pedidos -> Dependencies.");
        } catch (Exception e) {
            System.err.println("[RestauranteMain] Error iniciando '" + nombreLocal + "': " + e.getMessage());
        }
    }

    private static void ejecutarMenuConsola() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=================================================================");
            System.out.println("  MENÚ DE SIMULACIÓN - RESTAURANTE MULTIAGENTE");
            System.out.println("=================================================================");
            System.out.println("  1. Crear pedido personalizado ");
            System.out.println("  2. Ejecutar escenarios de prueba predefinidos");
            System.out.println("  3. Pagar cuenta de un pedido completado");
            System.out.println("  4. Consultar inventario y gasto predictivo en Supabase");
            System.out.println("  0. Salir");
            System.out.print("Seleccione una opción: ");

            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1" -> flujoPedidoPersonalizado(scanner);
                case "2" -> flujoEscenariosPredefinidos(scanner);
                case "3" -> flujoPagarCuenta(scanner);
                case "4" -> mostrarInventarioActual();
                case "0" -> {
                    System.out.println("Cerrando simulador...");
                    System.exit(0);
                }
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    private static void flujoPedidoPersonalizado(Scanner scanner) {
        List<PedidoDAO.OpcionMenu> menu = pedidoDAO.listarMenuConDisponibilidad();
        if (menu.isEmpty()) {
            System.out.println("No hay platillos activos en la base de datos.");
            return;
        }

        int numeroMesa;
        while (true) {
            System.out.print("\nIngrese el número de mesa: ");
            String entradaMesa = scanner.nextLine().trim();
            try {
                numeroMesa = Integer.parseInt(entradaMesa);
                if (numeroMesa <= 0) {
                    System.out.println("El número de mesa debe ser mayor a 0.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Ingrese solo el número de la mesa (ej. 1, 2, 3).");
            }
        }
        String mesa = "mesa_" + numeroMesa;

        Map<Long, Integer> seleccion = new LinkedHashMap<>();

        while (true) {
            System.out.println("\n--- PLATILLOS DISPONIBLES (Mesa #" + numeroMesa + ") ---");
            for (PedidoDAO.OpcionMenu op : menu) {
                System.out.printf("  [%d] %-28s | $%8.0f | Disp. estimada: %d porciones%n",
                        op.idPlatillo, op.nombre, op.precio, op.porcionesDisponibles);
            }
            System.out.println("  [0] Confirmar y enviar pedido a los agentes");
            System.out.print("ID del platillo a agregar: ");

            long idPlatillo;
            try {
                idPlatillo = Long.parseLong(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número válido.");
                continue;
            }

            if (idPlatillo == 0) {
                break;
            }

            boolean existe = menu.stream().anyMatch(m -> m.idPlatillo == idPlatillo);
            if (!existe) {
                System.out.println("ID de platillo no encontrado.");
                continue;
            }

            System.out.print("Cantidad de porciones: ");
            int cantidad;
            try {
                cantidad = Integer.parseInt(scanner.nextLine().trim());
                if (cantidad <= 0) {
                    System.out.println("La cantidad debe ser mayor a 0.");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Cantidad inválida.");
                continue;
            }

            seleccion.merge(idPlatillo, cantidad, Integer::sum);
            System.out.println("Platillo agregado al carrito.");
        }

        if (seleccion.isEmpty()) {
            System.out.println("Pedido cancelado (sin platillos).");
            return;
        }

        long idPedido = pedidoDAO.crearPedidoDesdeConsola(mesa, seleccion);
        if (idPedido > 0) {
            esperarProcesamientoPedido(idPedido);
        }
    }

    private static void flujoEscenariosPredefinidos(Scanner scanner) {
        List<PedidoDAO.OpcionMenu> menu = pedidoDAO.listarMenuConDisponibilidad();
        Map<String, Long> idsPorNombre = new LinkedHashMap<>();
        for (PedidoDAO.OpcionMenu op : menu) {
            idsPorNombre.put(op.nombre.toUpperCase(), op.idPlatillo);
        }

        System.out.println("\n--- ESCENARIOS DE PRUEBA ---");
        System.out.println("  A. Exitoso simple: 2x POLLO A LA PLANCHA + 2x BEBIDA GASEOSA COLA");
        System.out.println("  B. Multi-lote FEFO: 18x POLLO A LA PLANCHA (consume 2 lotes de pechuga)");
        System.out.println("  C. Rechazo por agotado: 1x HAMBURGUESA DE RES (Queso Mozzarella = 0)");
        System.out.println("  D. Rechazo por stock parcial: 8x CAZUELA DE CAMARONES (2.0 KG > 1.5 KG)");
        System.out.println("  E. Rechazo por lote vencido: 2x COSTILLAS BBQ (Costilla vencida)");
        System.out.print("Elija escenario (A/B/C/D/E): ");

        String esc = scanner.nextLine().trim().toUpperCase();
        Map<Long, Integer> items = new LinkedHashMap<>();
        String etiquetaMesa;

        switch (esc) {
            case "A" -> {
                etiquetaMesa = "ESC_A_EXITOSO";
                agregarSiExiste(idsPorNombre, items, "POLLO A LA PLANCHA", 2);
                agregarSiExiste(idsPorNombre, items, "BEBIDA GASEOSA COLA", 2);
            }
            case "B" -> {
                etiquetaMesa = "ESC_B_FEFO_MULTILOTE";
                agregarSiExiste(idsPorNombre, items, "POLLO A LA PLANCHA", 18);
            }
            case "C" -> {
                etiquetaMesa = "ESC_C_AGOTADO";
                agregarSiExiste(idsPorNombre, items, "HAMBURGUESA DE RES", 1);
            }
            case "D" -> {
                etiquetaMesa = "ESC_D_PARCIAL";
                agregarSiExiste(idsPorNombre, items, "CAZUELA DE CAMARONES", 8);
            }
            case "E" -> {
                etiquetaMesa = "ESC_E_VENCIDO";
                agregarSiExiste(idsPorNombre, items, "COSTILLAS BBQ", 2);
            }
            default -> {
                System.out.println("Escenario no reconocido.");
                return;
            }
        }

        if (items.isEmpty()) {
            System.out.println("No se encontraron los platillos del escenario en BD (ejecuta primero el Seeder).");
            return;
        }

        long idPedido = pedidoDAO.crearPedidoDesdeConsola(etiquetaMesa, items);
        if (idPedido > 0) {
            esperarProcesamientoPedido(idPedido);
        }
    }

    private static void agregarSiExiste(Map<String, Long> catalogo, Map<Long, Integer> destino, String nombre, int cant) {
        Long id = catalogo.get(nombre);
        if (id != null) {
            destino.put(id, cant);
        }
    }

    private static void esperarProcesamientoPedido(long idPedido) {
        System.out.println("\n[RestauranteMain] Pedido #" + idPedido + " creado en BD. Esperando respuesta de los agentes...");
        for (int i = 0; i < 12; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            String estado = pedidoDAO.obtenerEstadoPedido(idPedido);
            if ("COMPLETADO".equalsIgnoreCase(estado) || "RECHAZADO".equalsIgnoreCase(estado)) {
                System.out.println("[RestauranteMain] >>> Flujo finalizado para Pedido #" + idPedido + " | Estado final: " + estado + " <<<");
                return;
            }
        }
        System.out.println("[RestauranteMain] El pedido #" + idPedido + " sigue en proceso (" + pedidoDAO.obtenerEstadoPedido(idPedido) + ").");
    }

    private static void flujoPagarCuenta(Scanner scanner) {
        System.out.print("\nIngrese el ID del pedido a pagar (ej. 1): ");
        long idPedido;
        try {
            idPedido = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return;
        }

        System.out.print("Método de pago (EFECTIVO / TARJETA / NEQUI): ");
        String metodo = scanner.nextLine().trim().toUpperCase();
        if (metodo.isBlank()) {
            metodo = "EFECTIVO";
        }

        AgenteConsolaCaja.enviarSolicitudPago(idPedido, metodo);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
    }

    private static void mostrarInventarioActual() {
        System.out.println("\n================================================================================================================================");
        System.out.println("  DEMANDA SEMANAL DE PLATILLOS (ÚLTIMOS 7 DÍAS)");
        System.out.println("================================================================================================================================");
        List<PedidoDAO.DemandaSemanalPlatillo> demandaPlatos = pedidoDAO.obtenerDemandaSemanalPlatillos();
        int totalPorcionesSemana = 0;
        for (PedidoDAO.DemandaSemanalPlatillo dp : demandaPlatos) {
            totalPorcionesSemana += dp.porcionesPedidasSemana;
            System.out.printf("  - %-28s : %d porciones pedidas en la semana%n",
                    dp.nombrePlatillo, dp.porcionesPedidasSemana);
        }
        System.out.println("  TOTAL PLATILLOS PEDIDOS EN LA SEMANA: " + totalPorcionesSemana + " porciones");

        System.out.println("\n================================================================================================================================");
        System.out.println("  INVENTARIO ACTUAL Y REABASTECIMIENTO PREDICTIVO DINÁMICO (SEGÚN PLATILLOS DE LA SEMANA)");
        System.out.println("================================================================================================================================");
        System.out.printf("  %-22s | %-11s | %-10s | %-12s | %-13s | %-10s | %-17s%n",
                "INGREDIENTE", "STOCK ACT.", "PLATOS SEM", "GASTO/DÍA", "DEM. 7 DÍAS", "AUTONOMÍA", "ESTADO PREDICTIVO");
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");

        List<PedidoDAO.ItemStockPredictivo> reporte = pedidoDAO.obtenerStockConPrediccion();
        if (reporte.isEmpty()) {
            System.out.println("  No se encontraron ingredientes activos en inventario.");
            return;
        }

        List<PedidoDAO.ItemStockPredictivo> alertasCompra = new ArrayList<>();

        for (PedidoDAO.ItemStockPredictivo it : reporte) {
            String autonomiaStr = (it.diasAutonomia < 0)
                    ? "Sin demanda"
                    : String.format("%.1f días", it.diasAutonomia);

            System.out.printf("  %-22s | %7.3f %-3s | %-10d | %8.3f %-3s | %9.3f %-3s | %-10s | %s%n",
                    it.nombre,
                    it.stockDisponible, it.unidadMedida,
                    it.platosSemanaAsociados,
                    it.tasaConsumoDiario, it.unidadMedida,
                    it.gastoPredictivo7Dias, it.unidadMedida,
                    autonomiaStr,
                    it.diagnostico);

            if (it.reposicionSugerida > 0 && !"ÓPTIMO".equals(it.diagnostico)) {
                alertasCompra.add(it);
            }
        }

        System.out.println("================================================================================================================================");
        if (!alertasCompra.isEmpty()) {
            System.out.println("  SUGERENCIAS DE REABASTECIMIENTO DINÁMICO (Demanda semanal de platos + Stock mínimo):");
            for (PedidoDAO.ItemStockPredictivo al : alertasCompra) {
                System.out.printf("   -> %-22s : Pedir %.3f %s | Motivo: %s | Impulsado por: [%s]%n",
                        al.nombre, al.reposicionSugerida, al.unidadMedida, al.diagnostico, al.detallePlatosSemana);
            }
            System.out.println("================================================================================================================================");
        }
    }

    public static class AgenteConsolaCaja extends Agent {
        private static AgenteConsolaCaja instancia;

        @Override
        protected void setup() {
            instancia = this;
        }

        public static void enviarSolicitudPago(long idPedido, String metodo) {
            if (instancia == null) {
                System.err.println("[ConsolaCaja] Agente de caja no inicializado.");
                return;
            }
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(new AID("AgenteCuentaPago", AID.ISLOCALNAME));
            req.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            req.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
            req.setOntology("Restaurante");
            req.setContent("(pagar pedido_" + idPedido + " " + metodo + ")");

            instancia.addBehaviour(new AchieveREInitiator(instancia, req) {
                @Override
                protected void handleInform(ACLMessage inform) {
                    System.out.println("[ConsolaCaja] <- Pago confirmado por AgenteCuentaPago: " + inform.getContent());
                }

                @Override
                protected void handleFailure(ACLMessage failure) {
                    System.err.println("[ConsolaCaja] <- No se pudo procesar el pago: " + failure.getContent());
                }
            });
        }
    }
}