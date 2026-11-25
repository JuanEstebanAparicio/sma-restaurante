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

            // Si ambos módulos corren en el mismo PC, usa "localhost".
            // Si restaurante-core corre en el PC de un compañero, pasa su IP por argumentos (args[0]).
            String hostMainContainer = (args.length > 0 && !args[0].isBlank()) ? args[0] : "localhost";

            profile.setParameter(Profile.MAIN_HOST, hostMainContainer);
            profile.setParameter(Profile.MAIN_PORT, "1099");
            profile.setParameter(Profile.CONTAINER_NAME, "Contenedor-Pedidos");

            // Se une como contenedor secundario al Main-Container levantado por restaurante-core
            AgentContainer container = rt.createAgentContainer(profile);
            System.out.println("[MainPedidos] Contenedor-Pedidos conectado al Main-Container en " + hostMainContainer);

            AgentController agentePedidos = container.createNewAgent(
                    "AgentePedidos",
                    "pedidos.agentes.AgentePedidos",
                    null
            );
            agentePedidos.start();

        } catch (Exception e) {
            System.err.println("[MainPedidos] No se pudo conectar al Main-Container. "
                    + "Asegúrate de iniciar primero restaurante-core: " + e.getMessage());
            e.printStackTrace();
        }
    }
}