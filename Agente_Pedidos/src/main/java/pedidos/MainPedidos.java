package pedidos;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class MainPedidos {

    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();
            Profile profile = new ProfileImpl();

            String hostMainContainer = (args.length > 0 && !args[0].isBlank()) ? args[0].trim() : "localhost";

            profile.setParameter(Profile.MAIN_HOST, hostMainContainer);
            profile.setParameter(Profile.MAIN_PORT, "1099");
            profile.setParameter(Profile.CONTAINER_NAME, "Contenedor-Pedidos");

            if (args.length > 1 && !args[1].isBlank()) {
                profile.setParameter(Profile.LOCAL_HOST, args[1].trim());
            }

            AgentContainer container = rt.createAgentContainer(profile);
            System.out.println("[MainPedidos] Contenedor-Pedidos conectado al Main-Container en " + hostMainContainer);

            // 1. Iniciar AgentePedidos
            AgentController agentePedidos = container.createNewAgent(
                    "AgentePedidos",
                    "pedidos.Agente.AgentePedidos",
                    null
            );
            agentePedidos.start();

            // 2. Iniciar AgenteCuentaPago
            AgentController agenteCuentaPago = container.createNewAgent(
                    "AgenteCuentaPago",
                    "pedidos.Agente.AgenteCuentaPago",
                    null
            );
            agenteCuentaPago.start();

            System.out.println("[MainPedidos] 'AgentePedidos' y 'AgenteCuentaPago' activos.");

        } catch (Exception e) {
            System.err.println("[MainPedidos] Error conectando con el Main-Container: " + e.getMessage());
            e.printStackTrace();
        }
    }
}