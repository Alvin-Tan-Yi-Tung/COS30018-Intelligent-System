package IntelligentProject;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import javax.swing.*;

/**
 * Core system initializer and agent management hub
 * Handles JADE platform setup and agent lifecycle operations
 */
public class Main {
    private static AgentContainer mainContainer;    // JADE main container reference
    public static jade.wrapper.AgentContainer containerController;  // GUI access point
    
    /**
     * Provides singleton access to JADE container
     * @return Configured AgentContainer instance
     */
    public static AgentContainer getContainer() {
        // Singleton pattern ensures single container instance
    	if (mainContainer == null) {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost"); // Local JADE platform
            p.setParameter(Profile.GUI, "true");    // Enable JADE GUI
            
            // Enable Owner-to-Agent (O2A) communication for GUI integration
            p.setParameter("jade_core_ProfileImpl_o2a", "true");
            
            mainContainer = rt.createMainContainer(p);
            containerController = mainContainer; 
        }
        return mainContainer;
    }

    /**
     * Application entry point
     * @param args Command-line arguments (unused)
     */
    public static void main(String[] args) {
        try {
            // Initialize GUI
        	SwingUtilities.invokeLater(() -> {
                GUI.initialize();  // Create and show main interface
            });

            // Start JADE platform
            jade.core.Runtime rt = jade.core.Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.GUI, "true");
            
            // Enable O2A communication using the actual parameter name
            p.setParameter("jade_core_ProfileImpl_o2a", "true");
            
            mainContainer = rt.createMainContainer(p);
            
            // Initialize core agents
            createBroker();
            createInitialDealers();
            createInitialBuyers();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates BrokerAgent and monitoring tools
     */
    private static void createBroker() throws StaleProxyException {
        mainContainer.createNewAgent(
            "BrokerAgent", 
            "IntelligentProject.BrokerAgent", 
            null
        ).start();
        createSniffer();    // Message monitoring agent
    }
    
    /**
     * Creates SnifferAgent for message debugging
     */
    private static void createSniffer() throws StaleProxyException {
        mainContainer.createNewAgent(
            "SnifferAgent",
            "IntelligentProject.SnifferAgent",
            null
        ).start();
    }

    /**
     * Creates initial dealer agents for automated negotiation
     */
    private static void createInitialDealers() throws StaleProxyException {
        // Name, CarType, Price
        createDealer("A.Dealer1", "Toyota", 30000);
        createDealer("A.Dealer2", "Honda", 25000);
        createDealer("A.Dealer3", "Toyota", 28000);
    }

    /**
     * Creates initial buyer agents for automated negotiation
     */
    private static void createInitialBuyers() throws StaleProxyException {
        // Name, CarType, Offer, Budget
        createBuyer("A.Buyer1", "Toyota", 20000, 25000);
        createBuyer("A.Buyer2", "Toyota", 22000, 27000);
        createBuyer("A.Buyer3", "Honda", 18000, 23000);
        createBuyer("A.Buyer4", "Toyota", 30000, 45000);
        createBuyer("A.Buyer5", "Honda", 16000, 26000);
    }

    /**
     * Factory method for creating dealer agents
     * @param name Agent unique identifier
     * @param car Vehicle type being sold
     * @param price Initial asking price
     */
    public static void createDealer(String name, String car, int price) 
            throws StaleProxyException {
        Object[] args = new Object[]{car, price};
        mainContainer.createNewAgent(
            name, 
            "IntelligentProject.DealerAgent", 
            args
        ).start();
    }

    /**
     * Factory method for creating buyer agents
     * @param name Agent unique identifier
     * @param car Desired vehicle type
     * @param offer Initial negotiation offer
     * @param budget Maximum spending limit
     */
    public static void createBuyer(String name, String car, int offer, int budget) 
            throws StaleProxyException {
        Object[] args = new Object[]{car, offer, budget};
        mainContainer.createNewAgent(
            name, 
            "IntelligentProject.BuyerAgent", 
            args
        ).start();
    }
}