package IntelligentProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Custom dialog for creating Buyer Agent configuration
 * Handles input validation and data collection
 */
public class BuyerFormDialog extends JDialog {
    // Form input components
    private JTextField carField;            // Car type/model input
    private JTextField initialOfferField;   // Initial offer amount
    private JTextField maxBudgetField;      // Maximum spending limit
    private boolean submitted = false;      // Submission status flag

    /**
     * Initializes dialog configuration
     * @param parent Parent frame for modal positioning
     */
    public BuyerFormDialog(Frame parent) {
        super(parent, "Create Buyer Agent", true);  // Modal dialog
        setupUI();
    }

    /**
     * Creates and arranges UI components
     * Uses GridLayout for form structure
     */
    private void setupUI() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));  // 4 rows, 2 cols
        
        // Car type input
        panel.add(new JLabel("Car Type:"));
        carField = new JTextField();
        panel.add(carField);
        
        // Initial offer input
        panel.add(new JLabel("Initial Offer:"));
        initialOfferField = new JTextField();
        panel.add(initialOfferField);
        
        // Maximum budget input
        panel.add(new JLabel("Max Budget:"));
        maxBudgetField = new JTextField();
        panel.add(maxBudgetField);
        
        // Action buttons
        JButton submit = new JButton("Create");
        submit.addActionListener(this::onSubmit);
        panel.add(submit);
        
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        panel.add(cancel);
        
        // Finalize UI
        add(panel);
        setSize(300, 200);
        setLocationRelativeTo(null);    // Center on screen
    }

    /**
     * Handles form submission with validation
     * @param e Action event from submit button
     */
    private void onSubmit(ActionEvent e) {
        try {
            // Get and sanitize inputs
            String car = carField.getText().trim();
            String offer = initialOfferField.getText().trim();
            String budget = maxBudgetField.getText().trim();

            if (car.isEmpty() || offer.isEmpty() || budget.isEmpty()) {
                throw new IllegalArgumentException("All fields must be filled!");
            }

            if (!offer.matches("\\d+") || !budget.matches("\\d+")) {
                throw new IllegalArgumentException("Offer and budget must be positive integers!");
            }

            // Validation checks
            int offerValue = Integer.parseInt(offer);
            int budgetValue = Integer.parseInt(budget);

            if (offerValue <= 0 || budgetValue <= 0) {
                throw new IllegalArgumentException("Values must be greater than 0!");
            }

            if (offerValue >= budgetValue) {
                throw new IllegalArgumentException("Initial offer must be less than max budget!");
            }

            submitted = true;
            dispose();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format: Must be integers only!");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    /**
     * Retrieves validated form data
     * @return String array with [carType, initialOffer, maxBudget]
     *         or null if not submitted
     */
    public String[] getData() {
        if(!submitted) return null;
        return new String[] {
            carField.getText(),
            initialOfferField.getText(),
            maxBudgetField.getText()
        };
    }
}