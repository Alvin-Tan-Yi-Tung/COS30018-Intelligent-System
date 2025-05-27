package IntelligentProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Configuration dialog for creating Dealer Agents
 * Ensures valid car listing parameters
 */
public class DealerFormDialog extends JDialog {
    // Form components
    private JTextField carField;        // Car type/ model input
    private JTextField priceField;      // Initial asking price
    private boolean submitted = false;  // Submission status

    /**
     * Initializes dialog configuration
     * @param parent Parent frame for modal positioning
     */
    public DealerFormDialog(Frame parent) {
        super(parent, "Create Dealer Agent", true);
        setupUI();
    }

    /**
     * Constructs and arranges UI components
     * Uses GridLayout for consistent form structure
     */
    private void setupUI() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));  // 3 rows, 2 columns
        
        // Car type input
        panel.add(new JLabel("Car Type:"));
        carField = new JTextField();
        panel.add(carField);
        
        // Price input
        panel.add(new JLabel("List Price:"));
        priceField = new JTextField();
        panel.add(priceField);
        
        // Action buttons
        JButton submit = new JButton("Create");
        submit.addActionListener(this::onSubmit);
        panel.add(submit);
        
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        panel.add(cancel);
        
        // Finalize UI
        add(panel);
        setSize(300, 150);
        setLocationRelativeTo(null);
    }

    /**
     * Handles form submission with validation
     * @param e Action event from submit button
     */
    private void onSubmit(ActionEvent e) {
        try {
            // Validate price is a number
            int price = Integer.parseInt(priceField.getText()); 
            if (price <= 0) throw new NumberFormatException();

            // Mark as valid submission
            submitted = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid price! Must be a positive number.");
        }
    }

    /**
     * Retrieves validated form data
     * @return String array with [carType, listPrice] or null if not submitted
     */
    public String[] getData() {
        if(!submitted) return null;
        return new String[] {
            carField.getText(),
            priceField.getText()
        };
    }
}