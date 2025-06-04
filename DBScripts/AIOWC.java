import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.Category;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.wrappers.widgets.WidgetChild;
import java.util.Arrays;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("unused")
@ScriptManifest(author = "he1zen", name = "AIO Woodcutter", version = 1.0, description = "Chops trees with configurable options", category = Category.WOODCUTTING)
public class AIOWC extends AbstractScript {

    // === SETTINGS ===
    private final int TILE_RADIUS = 10;
    private String treeName = "Tree";
    private boolean dropLogs = true;

    private String status = "Starting...";
    private Tile startTile;
    private boolean isChopping = false;
    private long startTime;
    private int startXp;
    private int logsChopped;
    private boolean isGuiComplete = false;
    private boolean searchLogged = false;
    private long lastChopTime = 0; // Track the last time a tree was chopped
    private long idleStartTime = 0; // Track when idling started
    private final int IDLE_THRESHOLD = 5000; // 5 seconds without a tree

    @Override
    public void onStart() {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] treeOptions = {"Tree", "Oak tree", "Willow tree", "Maple tree", "Yew tree", "Magic tree", "Redwood tree"};
                String selectedTree = (String) JOptionPane.showInputDialog(
                        null,
                        "Select the tree type to chop:",
                        "Tree Selection",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        treeOptions,
                        treeOptions[0]
                );

                if (selectedTree != null && !selectedTree.isEmpty()) {
                    treeName = selectedTree.trim();
                    log("Selected tree type: " + treeName);
                } else {
                    treeName = "Oak tree";
                    log("Tree selection canceled or invalid, defaulting to Oak tree.");
                }

                String[] dropBankOptions = {"Drop Logs", "Bank Logs"};
                int dropBankChoice = JOptionPane.showOptionDialog(
                        null,
                        "Select what to do with logs:",
                        "Drop/Bank Selection",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        dropBankOptions,
                        dropBankOptions[0]
                );

                if (dropBankChoice != -1) {
                    dropLogs = dropBankChoice == 0;
                    log("Selected action: " + (dropLogs ? "Dropping" : "Banking"));
                } else {
                    dropLogs = true;
                    log("Drop/Bank selection canceled, defaulting to dropping logs.");
                }
            } catch (Exception e) {
                log("Error in GUI setup: " + e.getMessage());
                log("Stack trace: " + Arrays.toString(e.getStackTrace()));
                treeName = "Oak tree";
                dropLogs = true;
                log("GUI setup failed. Defaulting to Oak tree and dropping logs.");
            } finally {
                isGuiComplete = true;
            }
        });

        sleepUntil(() -> isGuiComplete, 30000, 500);

        startTile = Players.getLocal().getTile();
        log("Starting tile set to: " + startTile.toString());
        startTime = System.currentTimeMillis();
        startXp = Skills.getExperience(Skill.WOODCUTTING);
        logsChopped = 0;
        log("Started AIO Woodcutter for " + treeName + " with " + (dropLogs ? "dropping" : "banking") + " logs.");
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String getLogName() {
        String baseTreeName = treeName.toLowerCase().replace(" tree", "");
        switch (baseTreeName) {
            case "tree": return "Logs";
            case "oak": return "Oak logs";
            case "willow": return "Willow logs";
            case "maple": return "Maple logs";
            case "yew": return "Yew logs";
            case "magic": return "Magic logs";
            case "redwood": return "Redwood logs";
            default: return baseTreeName.substring(0, 1).toUpperCase() + baseTreeName.substring(1) + " logs";
        }
    }

    private double getXpPerLog() {
        String baseTreeName = treeName.toLowerCase().replace(" tree", "");
        switch (baseTreeName) {
            case "tree": return 25.0;
            case "oak": return 37.5;
            case "willow": return 67.5;
            case "maple": return 100.0;
            case "yew": return 175.0;
            case "magic": return 250.0;
            case "redwood": return 380.0;
            default: return 25.0;
        }
    }

    private void performAntiBan() {
        int action = Calculations.random(0, 100);
        if (action < 15) {
            status = "Anti-ban: Moving camera...";
            Camera.rotateTo(Calculations.random(1, 360), Calculations.random(100, 200));
            sleep(Calculations.random(300, 600));
        } else if (action < 25) {
            status = "Anti-ban: Moving mouse...";
            Mouse.moveOutsideScreen();
            sleep(Calculations.random(500, 1500));
        } else if (action < 29) {
            status = "Anti-ban: Checking skills...";
            Tabs.open(Tab.SKILLS);
            sleepUntil(() -> Tabs.isOpen(Tab.SKILLS), 2000, 600);
            WidgetChild woodcuttingWidget = Widgets.get(320, 22);
            if (woodcuttingWidget != null && woodcuttingWidget.isVisible()) {
                woodcuttingWidget.interact("Hover");
                sleep(Calculations.random(800, 1200));
            } else {
                log("Woodcutting widget not found or not visible!");
            }
            Tabs.open(Tab.INVENTORY);
            sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), 2000, 600);
        } else if (action < 34) {
            status = "Anti-ban: Idling...";
            sleep(Calculations.random(2000, 5000));
        }
    }

    private void performAntiBanWhileWalking() {
        if (Calculations.random(0, 100) < 30) {
            int action = Calculations.random(0, 100);
            if (action < 33) {
                status = "Anti-ban: Moving camera...";
                Camera.rotateTo(Calculations.random(1, 360), Calculations.random(100, 200));
                sleep(Calculations.random(200, 400));
            } else if (action < 66) {
                status = "Anti-ban: Moving mouse...";
                Mouse.move(new java.awt.Point(Calculations.random(0, 765), Calculations.random(0, 503)));
                sleep(Calculations.random(300, 600));
            } else {
                status = "Anti-ban: Idling...";
                sleep(Calculations.random(500, 1500));
            }
        }
    }

    private Tile getDynamicTile(Tile target) {
        int WALK_OFFSET = 2;
        int xOffset = Calculations.random(-WALK_OFFSET, WALK_OFFSET);
        int yOffset = Calculations.random(-WALK_OFFSET, WALK_OFFSET);
        return new Tile(target.getX() + xOffset, target.getY() + yOffset, target.getZ());
    }

    @Override
    public int onLoop() {
        // Handle chopping state
        if (isChopping) {
            status = "Chopping tree...";
            int xpBefore = Skills.getExperience(Skill.WOODCUTTING);
            sleepUntil(() -> !Players.getLocal().isAnimating(), 10000, 600);
            int xpAfter = Skills.getExperience(Skill.WOODCUTTING);
            int xpGained = xpAfter - xpBefore;
            if (xpGained > 0) { // Confirm a chop occurred
                double xpPerLog = getXpPerLog();
                int logsGained = (int) Math.round((double) xpGained / xpPerLog);
                logsChopped += logsGained;
                log("Gained " + xpGained + " XP, chopped " + logsGained + " logs, total: " + logsChopped + ", xpPerLog: " + xpPerLog);
                lastChopTime = System.currentTimeMillis(); // Update last chop time
                // Perform anti-ban after successful chop with 50% chance
                if (Calculations.random(0, 100) < 75) {
                    performAntiBan();
                }
            }
            isChopping = false;
            return 100;
        }

        // Handle inventory when not chopping
        if (Inventory.isFull()) {
            String logName = getLogName();
            if (dropLogs) {
                status = "Dropping " + logName + "...";
                Inventory.dropAll(logName);
                sleepUntil(() -> !Inventory.isFull(), 5000, 600);
                if (Inventory.isFull()) {
                    log("Failed to drop " + logName + "! Inventory still full. Attempting to drop all items...");
                    Inventory.dropAll();
                    sleepUntil(() -> !Inventory.isFull(), 5000, 600);
                    if (Inventory.isFull()) {
                        log("Critical error: Unable to clear inventory. Stopping script.");
                        stop();
                    } else {
                        sleep(800, 1200);
                    }
                } else {
                    sleep(800, 1200);
                }
            } else {
                status = "Banking " + logName + "...";
                if (!Bank.isOpen()) {
                    Bank.open();
                    sleepUntil(Bank::isOpen, 5000, 600);
                }
                if (Bank.isOpen()) {
                    status = "Depositing " + logName + "...";
                    Bank.depositAll(logName);
                    sleepUntil(() -> Inventory.count(logName) == 0, 5000, 600);
                    if (Inventory.count(logName) > 0) {
                        log("Failed to deposit " + logName + "! Inventory still has logs. Attempting to deposit all...");
                        Bank.depositAll(logName);
                        sleepUntil(() -> Inventory.count(logName) == 0, 5000, 600);
                        if (Inventory.count(logName) > 0) {
                            log("Warning: Unable to fully deposit " + logName + ". Continuing with remaining items (e.g., axe)...");
                        }
                    }
                    Bank.close();
                    sleep(300, 600);

                    status = "Returning to start location...";
                    Tile targetTile = getDynamicTile(startTile);
                    while (Players.getLocal().getTile().distance(startTile) > 2) {
                        Walking.walk(targetTile);
                        performAntiBanWhileWalking();
                        sleepUntil(() -> !Players.getLocal().isMoving(), 5000, 600);
                        targetTile = getDynamicTile(startTile);
                    }
                }
            }
            return 100;
        }

        // Look for a tree within the radius
        GameObject tree = GameObjects.closest(t ->
                t != null &&
                        t.getName().equals(treeName) &&
                        t.exists() &&
                        t.getTile().distance(startTile) <= TILE_RADIUS
        );
        if (tree != null && !searchLogged) {
            log("Searching for " + treeName + " at " + startTile.toString() + ", found: true");
            searchLogged = true;
        } else if (tree == null && !searchLogged) {
            log("Searching for " + treeName + " at " + startTile.toString() + ", found: false");
            searchLogged = true;
        }

        if (tree != null) {
            if (!Players.getLocal().isAnimating() && !Players.getLocal().isMoving()) {
                status = "Chopping tree...";
                tree.interact("Chop down");
                isChopping = true;
                sleepUntil(Players.getLocal()::isAnimating, 2000, 600);
            }
        } else {
            status = "Waiting for tree to regrowth...";
            if (idleStartTime == 0) {
                idleStartTime = System.currentTimeMillis(); // Start idle timer
            }
            sleep(2000, 3000);
            // Perform anti-ban if idling for at least IDLE_THRESHOLD with 50% chance
            if (System.currentTimeMillis() - idleStartTime >= IDLE_THRESHOLD && Calculations.random(0, 100) < 50) {
                performAntiBan();
                idleStartTime = 0; // Reset timer after anti-ban
            }
        }

        return 100;
    }

    @Override
    public void onPaint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        long runTimeMillis = System.currentTimeMillis() - startTime;
        String runTime = formatTime(runTimeMillis);
        int currentXp = Skills.getExperience(Skill.WOODCUTTING);
        int xpGained = currentXp - startXp;

        int x = 10;
        int y = 50;
        int width = 220;
        int height = 120;
        int arc = 10;

        GradientPaint gradient = new GradientPaint(x, y, new Color(20, 20, 50, 180), x, y + height, new Color(50, 50, 80, 180));
        g2.setPaint(gradient);
        g2.fillRoundRect(x, y, width, height, arc, arc);

        g2.setColor(new Color(150, 200, 255));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x, y, width, height, arc, arc);

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(220, 220, 220));

        int lineHeight = 16;
        int textX = x + 10;
        int textY = y + 18;

        g2.drawString("he1zen AIO Woodcutter", textX, textY);
        g2.drawString("Status: " + status, textX, textY + lineHeight);
        g2.drawString("Time: " + runTime + " | Logs: " + logsChopped, textX, textY + lineHeight * 2);
        g2.drawString("XP: " + xpGained + " | Tree: " + treeName, textX, textY + lineHeight * 3);
        g2.drawString("Action: " + (dropLogs ? "Dropping" : "Banking"), textX, textY + lineHeight * 4);
    }
}