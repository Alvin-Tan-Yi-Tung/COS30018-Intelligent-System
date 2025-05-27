package IntelligentProject;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;

public class SequenceDiagramPanel extends JPanel {
    private List<String> interactions = new ArrayList<>();
    private String currentAgent;
    private final Map<String, Color> performativeColors;
    private static final int LANE_WIDTH = 300;
    private static final int START_X = 50;
    private static final int START_Y = 50;
    private static final int VERTICAL_SPACING = 40;

    public SequenceDiagramPanel() {
        setBackground(Color.WHITE);
        performativeColors = new HashMap<>();
        initializePerformativeColors();
        
        // Add this timer for auto-refresh
        Timer refreshTimer = new Timer(1000, e -> repaint());
        refreshTimer.start();
    }

    private void initializePerformativeColors() {
        performativeColors.put("PROPOSE", new Color(0, 0, 139));    // Dark Blue
        performativeColors.put("ACCEPT", new Color(0, 128, 0));     // Dark Green
        performativeColors.put("REJECT", new Color(255, 0, 0));   // Firebrick
        performativeColors.put("INFORM", new Color(128, 0, 128));   // Purple
        performativeColors.put("REQUEST", new Color(34, 139, 34));  // Forest Green
        performativeColors.put("UNKNOWN", Color.DARK_GRAY);
        performativeColors.put("QUERY_IF", new Color(0, 0, 200));
    }

    public void setInteractions(List<String> interactions, String currentAgent) {
        this.interactions = new ArrayList<>(interactions);
        this.currentAgent = currentAgent;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Setup participant lanes with manual agent support
        Map<String, Integer> participantLanes = new LinkedHashMap<>();
        participantLanes.put("Buyer", START_X);
        participantLanes.put("Broker", START_X + LANE_WIDTH);
        participantLanes.put("Dealer", START_X + 2 * LANE_WIDTH);

        // Draw participant lanes
        drawParticipantLanes(g2d, participantLanes);

        // Draw interactions including manual agents
        int yPos = START_Y + 30;
        for (String interaction : interactions) {
            try {
                // Parse interaction components
                String[] parts = interaction.split(" -> ");
                if (parts.length < 2) continue;
                
                String sender = parts[0].trim();
                String[] receiverParts = parts[1].split(":");
                String receiver = receiverParts[0].trim();

                // Resolve agent types with enhanced manual agent handling
                String senderType = resolveAgentType(sender);
                String receiverType = resolveAgentType(receiver);

                // Skip invalid participants
                if (!participantLanes.containsKey(senderType) || 
                    !participantLanes.containsKey(receiverType)) {
                    continue;
                }

                // Get lane positions
                int senderX = participantLanes.get(senderType);
                int receiverX = participantLanes.get(receiverType);

                // Extract performative and content
                String performative = extractPerformative(interaction);
                String content = interaction.contains("]") ? 
                    interaction.substring(interaction.indexOf("]") + 1).trim() : "";

                // Set drawing color
                g2d.setColor(performativeColors.getOrDefault(
                    performative.toUpperCase(), 
                    performativeColors.get("UNKNOWN"))
                );

                // Draw interaction line
                g2d.drawLine(senderX, yPos, receiverX, yPos);
                
                // Draw arrowhead
                drawArrowHead(g2d, senderX, receiverX, yPos);

                // Draw interaction text
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                String displayText = !content.isEmpty() ? 
                    performative + ": " + content : performative;
                int textX = (senderX + receiverX)/2 - (g2d.getFontMetrics().stringWidth(displayText)/2);
                g2d.drawString(displayText, textX, yPos - 10);

                yPos += VERTICAL_SPACING;
            } catch (Exception e) {
                System.err.println("Error drawing interaction: " + interaction);
            }
        }
    }

    private void drawParticipantLanes(Graphics2D g2d, Map<String, Integer> lanes) {
        g2d.setColor(Color.LIGHT_GRAY);
        for (Map.Entry<String, Integer> entry : lanes.entrySet()) {
            int x = entry.getValue();
            g2d.drawLine(x, START_Y, x, getHeight() - 50);
            g2d.setColor(Color.BLACK);
            g2d.drawString(entry.getKey(), x - 25, START_Y - 10);
        }
    }

    private boolean shouldDisplayInteraction(String interaction) {
        return interaction.contains(currentAgent) 
            && !interaction.contains("Sniffer")
            && (interaction.contains("REQUEST") 
                || interaction.contains("QUERY_IF") 
                || interaction.contains("INFORM"));
    }

    private void drawInteraction(Graphics2D g2d, String interaction, 
                               Map<String, Integer> lanes, int yPos) {
        try {
            String[] parts = interaction.split(" -> ");
            String sender = parts[0].trim();
            String[] receiverPart = parts[1].split(":");
            String receiver = receiverPart[0].trim();
            
            String performative = extractPerformative(interaction);
            String content = interaction.contains("]") ? 
                interaction.substring(interaction.indexOf("]") + 1).trim() : 
                "";

            String senderType = resolveAgentType(sender);
            String receiverType = resolveAgentType(receiver);

            Integer senderX = lanes.get(senderType);
            Integer receiverX = lanes.get(receiverType);

            if (senderX != null && receiverX != null) {
                // Set interaction color
                g2d.setColor(performativeColors.getOrDefault(
                    performative.toUpperCase(), 
                    performativeColors.get("UNKNOWN"))
                );

                // Draw interaction line
                g2d.drawLine(senderX, yPos, receiverX, yPos);

                // Draw arrowhead
                drawArrowHead(g2d, senderX, receiverX, yPos);

                // Draw content
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                String displayText = performative + ": " + content;
                int textX = (senderX + receiverX)/2 - (displayText.length() * 3);
                g2d.drawString(displayText, textX, yPos - 10);
            }
        } catch (Exception e) {
            System.err.println("Error drawing interaction: " + interaction);
            e.printStackTrace();
        }
    }

    private String resolveAgentType(String agentName) {
        // Handle manual and automated buyers
        if (agentName.startsWith("M.Buyer") || agentName.startsWith("A.Buyer")) {
            return "Buyer";
        }
        // Handle manual and automated dealers
        if (agentName.startsWith("M.Dealer") || agentName.startsWith("A.Dealer")) {
            return "Dealer";
        }
        // Handle broker agent
        if (agentName.equals("BrokerAgent")) {
            return "Broker";
        }
        return agentName;
    }

    private String extractPerformative(String interaction) {
        int start = interaction.indexOf("[") + 1;
        int end = interaction.indexOf("]");
        return (start > 0 && end > start) ? 
            interaction.substring(start, end) : 
            "UNKNOWN";
    }

    private void drawArrowHead(Graphics2D g2d, int senderX, int receiverX, int yPos) {
        Path2D path = new Path2D.Double();
        int arrowSize = 7;

        if (senderX < receiverX) { // Right arrow
            path.moveTo(receiverX, yPos);
            path.lineTo(receiverX - arrowSize, yPos - arrowSize);
            path.moveTo(receiverX, yPos);
            path.lineTo(receiverX - arrowSize, yPos + arrowSize);
        } else { // Left arrow
            path.moveTo(receiverX, yPos);
            path.lineTo(receiverX + arrowSize, yPos - arrowSize);
            path.moveTo(receiverX, yPos);
            path.lineTo(receiverX + arrowSize, yPos + arrowSize);
        }
        g2d.draw(path);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1000, 600);
    }
}