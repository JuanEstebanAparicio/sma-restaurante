package sma.restaurante.hito0;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

public class LanzadorPruebaACL {

    public static void main(String[] args) throws Exception {

        Runtime runtime = Runtime.instance();

        Profile profile = new ProfileImpl();

        AgentContainer container =
                runtime.createMainContainer(profile);

        // Iniciar AgenteInventario
        container.createNewAgent(
                "inventario",
                "sma.restaurante.agent.AgenteInventario",
                null
        ).start();

        // Iniciar receptor: AgenteCocina
        container.createNewAgent(
                "cocina",
                "sma.restaurante.agent.AgenteCocina",
                null
        ).start();

        // Iniciar emisor de prueba: AgenteEmisor con nombre "pedidos"
        container.createNewAgent(
                "pedidos",
                "sma.restaurante.hito0.AgenteEmisor",
                null
        ).start();

        System.out.println(
                "Plataforma JADE de prueba ACL iniciada."
        );
    }
}