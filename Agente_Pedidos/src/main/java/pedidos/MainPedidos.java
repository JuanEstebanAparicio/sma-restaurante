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

            // Pasa la IP del Main-Container en Program arguments (ej. 192.168.56.1 o IP de red)
            String hostMainContainer = (args.length > 0 && !args[0].isBlank()) ? args[0].trim() : "localhost";

            profile.setParameter(Profile.MAIN_HOST, hostMainContainer);
            profile.setParameter(Profile.MAIN_PORT, "1099");
            profile.setParameter(Profile.CONTAINER_NAME, "Contenedor-Pedidos");

            // Opcional: si usas dos PCs en Wi-Fi, pasa tu IP local como 2do argumento para evitar 192.168.56.1
            if (args.length > 1 && !args[1].isBlank()) {
                profile.setParameter(Profile.LOCAL_HOST, args[1].trim());
            }

            AgentContainer container = rt.createAgentContainer(profile);
            System.out.println("[MainPedidos] Contenedor-Pedidos conectado al Main-Container en " + hostMainContainer);

            AgentController agentePedidos = container.createNewAgent(
                    "AgentePedidos",
                    "pedidos.Agente.AgentePedidos",
                    null
            );
            agentePedidos.start();

        } catch (Exception e) {
            System.err.println("[MainPedidos] No se pudo conectar al Main-Container de restaurante-core. "
                    + "Verifica que App.java esté iniciado y revisa la IP: " + e.getMessage());
            e.printStackTrace();
        }
    }
}