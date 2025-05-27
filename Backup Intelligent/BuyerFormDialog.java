package IntelligentProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BuyerFormDialog extends JDialog {
    private JTextField carField;
    private JTextField initialOfferField;
    private JTextField maxBudgetField;
    private boolean submitted = false;

    public BuyerFormDialog(Frame parent) {
        super(parent, "Create Buyer Agent", true);
        setupUI();
    }

    private void setupUI() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        
        panel.add(new JLabel("Car Type:"));
        carField = new JTextField();
        panel.add(carField);
        
        panel.add(new JLabel("Initial Offer:"));
        initialOfferField = new JTextField();
        panel.add(initialOfferField);
        
        panel.add(new JLabel("Max Budget:"));
        maxBudgetField = new JTextField();
        panel.add(maxBudgetField);
        
        JButton submit = new JButton("Create");
        submit.addActionListener(this::onSubmit);
        panel.add(submit);
        
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        panel.add(cancel);
        
        add(panel);
        setSize(300, 200);
        setLocationRelativeTo(null);
    }

    private void onSubmit(ActionEvent e) {
        try {
            String car = carField.getText().trim();
            String offer = initialOfferField.getText().trim();
            String budget = maxBudgetField.getText().trim();

            // Enhanced validation
            if (car.isEmpty() || offer.isEmpty() || budget.isEmpty()) {
                throw new IllegalArgumentException("All fields must be filled!");
            }

            if (!offer.matches("\\d+") || !budget.matches("\\d+")) {
                throw new IllegalArgumentException("Offer and budget must be positive integers!");
            }

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

    public String[] getData() {
        if(!submitted) return null;
        return new String[] {
            carField.getText(),
            initialOfferField.getText(),
            maxBudgetField.getText()
        };
    }
}