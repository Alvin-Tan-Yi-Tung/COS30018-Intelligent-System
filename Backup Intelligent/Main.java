package IntelligentProject;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import javax.swing.*;

public class Main {
    private static AgentContainer mainContainer;
    public static jade.wrapper.AgentContainer containerController;
    
    public static AgentContainer getContainer() {
    	if (mainContainer == null) {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.GUI, "true");
            
            // Enable O2A communication
            p.setParameter("jade_core_ProfileImpl_o2a", "true");
            
            mainContainer = rt.createMainContainer(p);
            containerController = mainContainer; 
        }
        return mainContainer;
    }

    public static void main(String[] args) {
        try {
            // Initialize GUI
        	SwingUtilities.invokeLater(() -> {
                GUI.initialize();  // Assuming your GUI uses Swing
            });

            // Start JADE platform
            jade.core.Runtime rt = jade.core.Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, "localhost");
            p.setParameter(Profile.GUI, "true");
            
            // Enable O2A communication using the actual parameter name
            p.setParameter("jade_core_ProfileImpl_o2a", "true");
            
            mainContainer = rt.createMainContainer(p);
            
            // Create initial agents
            createBroker();
            createInitialDealers();
            createInitialBuyers();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createBroker() throws StaleProxyException {
        mainContainer.createNewAgent(
            "BrokerAgent", 
            "IntelligentProject.BrokerAgent", 
            null
        ).start();
        createSniffer();
    }
    
 // Add this new method to Main.java
    private static void createSniffer() throws StaleProxyException {
        mainContainer.createNewAgent(
            "SnifferAgent",
            "IntelligentProject.SnifferAgent",
            null
        ).start();
    }

    private static void createInitialDealers() throws StaleProxyException {
        createDealer("A.Dealer1", "Toyota", 30000);
        createDealer("A.Dealer2", "Honda", 25000);
        createDealer("A.Dealer3", "Toyota", 28000);
    }

    private static void createInitialBuyers() throws StaleProxyException {
        createBuyer("A.Buyer1", "Toyota", 20000, 25000);
        createBuyer("A.Buyer2", "Toyota", 22000, 27000);
        createBuyer("A.Buyer3", "Honda", 18000, 23000);
    }

    public static void createDealer(String name, String car, int price) 
            throws StaleProxyException {
        Object[] args = new Object[]{car, price};
        mainContainer.createNewAgent(
            name, 
            "IntelligentProject.DealerAgent", 
            args
        ).start();
    }

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