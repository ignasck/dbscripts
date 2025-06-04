import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.widget.Widgets;

import javax.swing.*;
import java.awt.*;

@ScriptManifest(author = "he1zen", name = "AIO GEHelper", version = 1.6, description = "Most GE Tasks, supports tool and item-on-item combinations with make-all handling", category = Category.MONEYMAKING)
public class GEHelper extends AbstractScript {
    private String status = "Starting...";
    private long startTime = System.currentTimeMillis();
    private String firstName = "";
    private String secondName = "";
    private String outputItemName = "";
    private int firstAmount = 1;
    private int secondAmount = 14;
    private boolean isGuiComplete = false;
    private int totalProcessed = 0;
    private boolean isToolBased = true;

    private String firstNameNorm = "";
    private String secondNameNorm = "";
    private String outputItemNameNorm = "";

    @Override
    public void onStart() {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("GEHelper Setup");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(400, 250);
                frame.setLayout(new GridLayout(7, 2));

                JLabel firstLabel = new JLabel("First Item (e.g., Knife or Pizza base):");
                JTextField firstField = new JTextField("Knife");
                JLabel firstAmountLabel = new JLabel("First Item Amount (1 for tool, >1 for item):");
                JTextField firstAmountField = new JTextField("1");
                JLabel secondLabel = new JLabel("Second Item (e.g., Chocolate bar or Tomato):");
                JTextField secondField = new JTextField("Chocolate bar");
                JLabel secondAmountLabel = new JLabel("Second Item Amount:");
                JTextField secondAmountField = new JTextField("14");
                JLabel outputLabel = new JLabel("Output Item (e.g., Chocolate dust or Incomplete pizza):");
                JTextField outputField = new JTextField("Chocolate dust");
                JButton startButton = new JButton("Start");

                startButton.addActionListener(e -> {
                    try {
                        firstName = firstField.getText().trim();
                        if (firstName.isEmpty()) {
                            firstName = "Knife";
                            log("First item input empty, defaulting to 'Knife'.");
                        }

                        firstAmount = Integer.parseInt(firstAmountField.getText().trim());
                        if (firstAmount <= 0) firstAmount = 1;
                        isToolBased = (firstAmount == 1);

                        secondName = secondField.getText().trim();
                        if (secondName.isEmpty()) {
                            secondName = isToolBased ? "Chocolate bar" : "Tomato";
                            log("Second item input empty, defaulting to '" + secondName + "'.");
                        }

                        secondAmount = Integer.parseInt(secondAmountField.getText().trim());
                        if (secondAmount <= 0) secondAmount = 14;

                        outputItemName = outputField.getText().trim();
                        if (outputItemName.isEmpty()) {
                            outputItemName = isToolBased ? "Chocolate dust" : "Incomplete pizza";
                            log("Output item input empty, defaulting to '" + outputItemName + "'.");
                        }

                        firstNameNorm = capitalizeName(firstName);
                        secondNameNorm = capitalizeName(secondName);
                        outputItemNameNorm = capitalizeName(outputItemName);
                        isGuiComplete = true;
                        frame.dispose();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid amount entered. Using defaults.");
                        firstName = isToolBased ? "Knife" : "Pizza base";
                        secondName = isToolBased ? "Chocolate bar" : "Tomato";
                        outputItemName = isToolBased ? "Chocolate dust" : "Incomplete pizza";
                        firstAmount = isToolBased ? 1 : 14;
                        secondAmount = 14;
                        firstNameNorm = capitalizeName(firstName);
                        secondNameNorm = capitalizeName(secondName);
                        outputItemNameNorm = capitalizeName(outputItemName);
                        isGuiComplete = true;
                        frame.dispose();
                    }
                });

                frame.add(firstLabel);
                frame.add(firstField);
                frame.add(firstAmountLabel);
                frame.add(firstAmountField);
                frame.add(secondLabel);
                frame.add(secondField);
                frame.add(secondAmountLabel);
                frame.add(secondAmountField);
                frame.add(outputLabel);
                frame.add(outputField);
                frame.add(new JLabel());
                frame.add(startButton);

                frame.setVisible(true);
            } catch (Exception e) {
                log("GUI setup error: " + e.getMessage());
                firstName = isToolBased ? "Knife" : "Pizza base";
                secondName = isToolBased ? "Chocolate bar" : "Tomato";
                outputItemName = isToolBased ? "Chocolate dust" : "Incomplete pizza";
                firstAmount = isToolBased ? 1 : 14;
                secondAmount = 14;
                firstNameNorm = capitalizeName(firstName);
                secondNameNorm = capitalizeName(secondName);
                outputItemNameNorm = capitalizeName(outputItemName);
                isGuiComplete = true;
            }
        });

        sleepUntil(() -> isGuiComplete, 60000, 1000);
        if (!isGuiComplete) {
            log("GUI not completed within 60 seconds. Stopping script.");
            stop();
        }
        log("Starting with: " + firstAmount + " " + firstName + ", " + secondAmount + " " + secondName + ", output: " + outputItemName);
    }

    private String capitalizeName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

    @Override
    public int onLoop() {
        if (!isGuiComplete) {
            status = "Waiting for GUI...";
            return 200;
        }



        if (firstAmount > 0 && !Inventory.contains(firstNameNorm)) {
            status = "Need " + firstNameNorm + " in inventory.";
            withdrawItems();
            return 200;
        }

        if (Inventory.count(secondNameNorm) < secondAmount) {
            status = "Need more " + secondNameNorm + " in inventory.";
            withdrawItems();
            return 200;
        }

        if (!isToolBased && Inventory.count(firstNameNorm) < firstAmount) {
            status = "Need more " + firstNameNorm + " in inventory.";
            withdrawItems();
            return 200;
        }

        status = "Combining items...";
        int initialCount = isToolBased ? Inventory.count(secondNameNorm) : Math.min(Inventory.count(firstNameNorm), Inventory.count(secondNameNorm));
        combineItems();
        waitForCombination(initialCount);

        if (Inventory.contains(outputItemNameNorm)) {
            bankOutputItems();
        }

        return 200;
    }

    private void combineItems() {
        if (isToolBased) {
            if (!Inventory.contains(firstNameNorm) || !Inventory.contains(secondNameNorm)) {
                log("Missing tool or material.");
                return;
            }
        } else {
            if (!Inventory.contains(firstNameNorm) || !Inventory.contains(secondNameNorm)) {
                log("Missing one or both items for combination.");
                return;
            }
        }

        int maxCombinations = isToolBased ? Inventory.count(secondNameNorm) : Math.min(Inventory.count(firstNameNorm), Inventory.count(secondNameNorm));
        log("Combining up to " + maxCombinations + " sets of items.");

        if (Inventory.interact(firstNameNorm, "Use")) {
            sleep(Calculations.random(600, 1200));
            if (Inventory.interact(secondNameNorm, "Use")) {
                log("Used " + firstNameNorm + " on " + secondNameNorm);
                handleMakeAll();
            } else {
                log("Failed to use " + secondNameNorm);
            }
        } else {
            log("Failed to use " + firstNameNorm);
        }
    }

    private void handleMakeAll() {
        status = "Checking for make-all interface...";
        sleepUntil(() -> Widgets.getWidget(270) != null && Widgets.getWidget(270).isVisible(), 5000, 500);
        if (Widgets.getWidget(270) != null && Widgets.getWidget(270).isVisible()) {
            log("Make-all interface detected, selecting all...");
            Dialogues.chooseOption(1); // Select first option (usually "Make All")
            sleep(Calculations.random(300, 600));
        } else {
            log("No make-all interface detected.");
        }
    }

    private void waitForCombination(final int initialCount) {
        status = "Waiting for combination result...";
        int initialOutputCount = Inventory.count(outputItemNameNorm);

        sleepUntil(() ->
                        (isToolBased ? Inventory.count(secondNameNorm) < initialCount : Math.min(Inventory.count(firstNameNorm), Inventory.count(secondNameNorm)) < initialCount) ||
                                Inventory.count(outputItemNameNorm) > initialOutputCount,
                20000, 500
        );

        int used = isToolBased ? initialCount - Inventory.count(secondNameNorm) : initialCount - Math.min(Inventory.count(firstNameNorm), Inventory.count(secondNameNorm));
        if (used > 0) {
            totalProcessed += used;
            log("Combined " + used + " items. Total processed: " + totalProcessed);
        } else {
            log("No items combined. Possible game lag or error.");
        }
    }

    private void withdrawItems() {
        log("Opening bank to withdraw items...");
        if (!Bank.isOpen()) {
            Bank.open();
            sleepUntil(Bank::isOpen, 5000, 500);
        }
        if (!Bank.isOpen()) {
            log("Could not open bank. Stopping.");
            stop();
            return;
        }

        if (Inventory.contains(outputItemNameNorm)) {
            Bank.depositAll(outputItemNameNorm);
            sleepUntil(() -> !Inventory.contains(outputItemNameNorm), 2000, 500);
            log("Deposited all " + outputItemNameNorm);
        }

        if (firstAmount > 0 && Inventory.count(firstNameNorm) < firstAmount) {
            if (Bank.contains(firstNameNorm)) {
                int space = Inventory.getEmptySlots();
                int toWithdraw = Math.min(firstAmount - Inventory.count(firstNameNorm), space);
                if (toWithdraw > 0) {
                    Bank.withdraw(firstNameNorm, toWithdraw);
                    sleepUntil(() -> Inventory.count(firstNameNorm) >= toWithdraw, 2000, 500);
                } else {
                    log("Not enough inventory space for " + firstNameNorm + ".");
                    bankOutputItems();
                }
            } else {
                log("Item " + firstNameNorm + " not found in bank. Stopping script.");
                stop();
                return;
            }
        }

        if (secondAmount > 0 && Inventory.count(secondNameNorm) < secondAmount) {
            if (Bank.contains(secondNameNorm)) {
                int space = Inventory.getEmptySlots();
                int toWithdraw = Math.min(secondAmount - Inventory.count(secondNameNorm), space);
                if (toWithdraw > 0) {
                    Bank.withdraw(secondNameNorm, toWithdraw);
                    sleepUntil(() -> Inventory.count(secondNameNorm) >= toWithdraw, 2000, 500);
                } else {
                    log("Not enough inventory space for " + secondNameNorm + ".");
                    bankOutputItems();
                }
            } else {
                log("No more " + secondNameNorm + " in bank. Stopping.");
                stop();
                return;
            }
        }

        Bank.close();
        sleepUntil(() -> !Bank.isOpen(), 3000, 500);
    }

    private void bankOutputItems() {
        log("Banking output items...");
        if (!Bank.isOpen()) {
            Bank.open();
            sleepUntil(Bank::isOpen, 3000, 500);
        }
        if (!Bank.isOpen()) {
            log("Cannot open bank to deposit items.");
            return;
        }

        if (Inventory.contains(outputItemNameNorm)) {
            Bank.depositAll(outputItemNameNorm);
            sleepUntil(() -> !Inventory.contains(outputItemNameNorm), 2000, 500);
            log("Deposited all " + outputItemNameNorm);
        }

        if (isToolBased && firstAmount == 1 && Inventory.contains(firstNameNorm)) {
            Bank.depositAll(firstNameNorm);
            sleepUntil(() -> !Inventory.contains(firstNameNorm), 2000, 500);
            log("Deposited tool " + firstNameNorm);
        }

        Bank.close();
        sleepUntil(() -> !Bank.isOpen(), 3000, 500);
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public void onPaint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        long runTimeMillis = System.currentTimeMillis() - startTime;
        String runTime = formatTime(runTimeMillis);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 14));

        int x = 10;
        int y = 50;
        int lineHeight = 18;

        g2.drawString("AIO GEHelper by he1zen (v1.6)", x, y);
        g2.drawString("Time Running: " + runTime, x, y + lineHeight);
        g2.drawString("Status: " + status, x, y + lineHeight * 2);
        g2.drawString("Total Processed: " + totalProcessed + " " + outputItemNameNorm, x, y + lineHeight * 3);
    }
}