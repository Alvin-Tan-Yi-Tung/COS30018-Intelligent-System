package IntelligentProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DealerFormDialog extends JDialog {
    private JTextField carField;
    private JTextField priceField;
    private boolean submitted = false;

    public DealerFormDialog(Frame parent) {
        super(parent, "Create Dealer Agent", true);
        setupUI();
    }

    private void setupUI() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        panel.add(new JLabel("Car Type:"));
        carField = new JTextField();
        panel.add(carField);
        
        panel.add(new JLabel("List Price:"));
        priceField = new JTextField();
        panel.add(priceField);
        
        JButton submit = new JButton("Create");
        submit.addActionListener(this::onSubmit);
        panel.add(submit);
        
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        panel.add(cancel);
        
        add(panel);
        setSize(300, 150);
        setLocationRelativeTo(null);
    }

    private void onSubmit(ActionEvent e) {
        try {
            // Validate price is a number
            int price = Integer.parseInt(priceField.getText()); 
            if (price <= 0) throw new NumberFormatException();
            submitted = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid price! Must be a positive number.");
        }
    }

    public String[] getData() {
        if(!submitted) return null;
        return new String[] {
            carField.getText(),
            priceField.getText()
        };
    }
}