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
@ScriptManifest(author = "he1zen", name = "AIO Miner", version = 1.0, description = "Mines ores with configurable options", category = Category.MINING)
public class AIOMining extends AbstractScript {

    // === SETTINGS ===
    private final int TILE_RADIUS = 10; // Search radius from starting tile
    private String oreName = "Clay"; // Default ore type (will be overwritten by GUI)
    private boolean dropOres = true; // Default to dropping ores (will be overwritten by GUI)

    private String status = "Starting...";
    private Tile startTile;
    private boolean isMining = false;
    private long startTime;
    private int startXp;
    private int oresMined;
    private boolean isGuiComplete = false; // Flag to track GUI completion
    private boolean searchLogged = false; // Flag to log search status once

    @Override
    public void onStart() {
        // Show GUI for ore and drop/bank selection
        SwingUtilities.invokeLater(() -> {
            try {
                String[] oreOptions = {"Clay", "Copper", "Tin", "Blurite", "Silver", "Daeyalt", "Coal", "Gold", "Mithril", "Adamantite", "Runite", "Amethyst"};
                String selectedOre = (String) JOptionPane.showInputDialog(
                        null,
                        "Select the ore type to mine:",
                        "Ore Selection",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        oreOptions,
                        oreOptions[0]
                );

                // Update oreName based on user selection
                if (selectedOre != null && !selectedOre.isEmpty()) {
                    oreName = selectedOre.trim();
                    log("Selected ore type: " + oreName);
                } else {
                    oreName = "Clay";
                    log("Ore selection canceled or invalid, defaulting to Clay.");
                }

                // Validate Mining level
                int requiredLevel = getRequiredLevel();
                if (Skills.getRealLevel(Skill.MINING) < requiredLevel) {
                    log("Error: You need level " + requiredLevel + " Mining to mine " + oreName + ". Current level: " + Skills.getRealLevel(Skill.MINING));
                    JOptionPane.showMessageDialog(null, "You need level " + requiredLevel + " Mining to mine " + oreName + ".\nCurrent level: " + Skills.getRealLevel(Skill.MINING), "Level Requirement", JOptionPane.ERROR_MESSAGE);
                    stop();
                    return;
                }

                // Check for pickaxe
                if (!hasPickaxe()) {
                    log("Error: No suitable pickaxe found in inventory or equipped.");
                    JOptionPane.showMessageDialog(null, "No suitable pickaxe found in inventory or equipped.", "Pickaxe Requirement", JOptionPane.ERROR_MESSAGE);
                    stop();
                    return;
                }

                String[] dropBankOptions = {"Drop Ores", "Bank Ores"};
                int dropBankChoice = JOptionPane.showOptionDialog(
                        null,
                        "Select what to do with ores:",
                        "Drop/Bank Selection",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        dropBankOptions,
                        dropBankOptions[0]
                );

                if (dropBankChoice != -1) {
                    dropOres = dropBankChoice == 0; // 0 = Drop, 1 = Bank
                    log("Selected action: " + (dropOres ? "Dropping" : "Banking"));
                } else {
                    dropOres = true;
                    log("Drop/Bank selection canceled, defaulting to dropping ores.");
                }
            } catch (Exception e) {
                log("Error in GUI setup: " + e.getMessage());
                log("Stack trace: " + Arrays.toString(e.getStackTrace()));
                oreName = "Clay";
                dropOres = true;
                log("GUI setup failed. Defaulting to Clay and dropping ores.");
            } finally {
                isGuiComplete = true;
            }
        });

        // Wait for GUI to complete before proceeding
        sleepUntil(() -> isGuiComplete, 30000, 500);

        startTile = Players.getLocal().getTile();
        log("Starting tile set to: " + startTile.toString());
        startTime = System.currentTimeMillis();
        startXp = Skills.getExperience(Skill.MINING);
        oresMined = 0;
        log("Started AIO Miner for " + oreName + " with " + (dropOres ? "dropping" : "banking") + " ores.");
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Get the required Mining level for the selected ore
    private int getRequiredLevel() {
        switch (oreName.toLowerCase()) {
            case "clay":
            case "copper":
            case "tin":
                return 1;
            case "blurite":
                return 10;
            case "silver":
            case "daeyalt":
                return 20;
            case "coal":
                return 30;
            case "gold":
                return 40;
            case "mithril":
                return 55;
            case "adamantite":
                return 70;
            case "runite":
                return 85;
            case "amethyst":
                return 92;
            default:
                return 1; // Default to Clay level
        }
    }

    // Get the ore name for inventory (e.g., "Copper ore")
    private String getOreItemName() {
        switch (oreName.toLowerCase()) {
            case "clay":
                return "Clay";
            case "copper":
                return "Copper ore";
            case "tin":
                return "Tin ore";
            case "blurite":
                return "Blurite ore";
            case "silver":
                return "Silver ore";
            case "daeyalt":
                return "Daeyalt shard";
            case "coal":
                return "Coal";
            case "gold":
                return "Gold ore";
            case "mithril":
                return "Mithril ore";
            case "adamantite":
                return "Adamantite ore";
            case "runite":
                return "Runite ore";
            case "amethyst":
                return "Amethyst";
            default:
                return oreName + " ore"; // Fallback
        }
    }

    // Get XP per ore
    private double getXpPerOre() {
        switch (oreName.toLowerCase()) {
            case "clay":
                return 5.0;
            case "copper":
            case "tin":
            case "blurite":
                return 17.5;
            case "silver":
                return 40.0;
            case "daeyalt":
                return 48.0;
            case "coal":
                return 50.0;
            case "gold":
                return 65.0;
            case "mithril":
                return 80.0;
            case "adamantite":
                return 95.0;
            case "runite":
                return 125.0;
            case "amethyst":
                return 240.0;
            default:
                return 5.0; // Default to Clay XP
        }
    }

    // Check for a suitable pickaxe
    private boolean hasPickaxe() {
        String[] pickaxes = {"Bronze pickaxe", "Iron pickaxe", "Steel pickaxe", "Mithril pickaxe", "Adamant pickaxe", "Rune pickaxe", "Dragon pickaxe"};
        for (String pickaxe : pickaxes) {
            if (Inventory.contains(pickaxe) || Players.getLocal().getEquipment().contains(pickaxe)) {
                return true;
            }
        }
        return false;
    }

    // Anti-ban methods
    private void performAntiBan() {
        int action = Calculations.random(0, 100);
        if (action < 15) { // 15% chance
            status = "Anti-ban: Moving camera...";
            Camera.rotateTo(Calculations.random(1, 360), Calculations.random(100, 200));
            sleep(Calculations.random(300, 600));
        } else if (action < 25) { // 10% chance
            status = "Anti-ban: Moving mouse...";
            Mouse.moveOutsideScreen();
            sleep(Calculations.random(500, 1500));
        } else if (action < 29) { // 4% chance
            status = "Anti-ban: Checking skills...";
            Tabs.open(Tab.SKILLS);
            sleepUntil(() -> Tabs.isOpen(Tab.SKILLS), 2000, 600);

            int parentId = 320;
            int miningChildId = 21; // Mining skill widget
            WidgetChild miningWidget = Widgets.get(parentId, miningChildId);

            if (miningWidget != null && miningWidget.isVisible()) {
                miningWidget.interact("Hover");
                sleep(Calculations.random(800, 1200));
            } else {
                log("Mining widget not found or not visible!");
            }

            Tabs.open(Tab.INVENTORY);
            sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), 2000, 600);
        } else if (action < 34) { // 5% chance
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
        // Random offset for dynamic tile selection
        int WALK_OFFSET = 2;
        int xOffset = Calculations.random(-WALK_OFFSET, WALK_OFFSET);
        int yOffset = Calculations.random(-WALK_OFFSET, WALK_OFFSET);
        return new Tile(target.getX() + xOffset, target.getY() + yOffset, target.getZ());
    }

    @Override
    public int onLoop() {
        // Perform anti-ban occasionally
        if (Calculations.random(0, 100) < 10) {
            performAntiBan();
        }

        // If currently mining, wait until the animation stops
        if (isMining) {
            status = "Mining rock...";
            int xpBefore = Skills.getExperience(Skill.MINING);
            sleepUntil(() -> !Players.getLocal().isAnimating(), 10000, 600);
            int xpAfter = Skills.getExperience(Skill.MINING);
            int xpGained = xpAfter - xpBefore;
            double xpPerOre = getXpPerOre();
            int oresGained = (int) Math.round((double) xpGained / xpPerOre);
            oresMined += oresGained;
            log("Gained " + xpGained + " XP, mined " + oresGained + " ores, total: " + oresMined + ", xpPerOre: " + xpPerOre);
            isMining = false;
            return 100;
        }

        // Handle inventory when not mining
        if (Inventory.isFull()) {
            String oreItemName = getOreItemName();
            if (dropOres) {
                status = "Dropping " + oreItemName + "...";
                Inventory.dropAll(oreItemName);
                sleepUntil(() -> !Inventory.isFull(), 5000, 600);
                if (Inventory.isFull()) {
                    log("Failed to drop " + oreItemName + "! Inventory still full. Attempting to drop all items...");
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
                status = "Banking " + oreItemName + "...";
                if (!Bank.isOpen()) {
                    Bank.open();
                    sleepUntil(Bank::isOpen, 5000, 600);
                }
                if (Bank.isOpen()) {
                    status = "Depositing " + oreItemName + "...";
                    Bank.depositAll(oreItemName);
                    sleepUntil(Inventory::isEmpty, 5000, 600);
                    if (!Inventory.isEmpty()) {
                        log("Failed to deposit " + oreItemName + "! Inventory not empty. Attempting to deposit all...");
                        Bank.depositAll(oreItemName);
                        sleepUntil(Inventory::isEmpty, 5000, 600);
                        if (!Inventory.isEmpty()) {
                            log("Critical error: Unable to clear inventory. Stopping script.");
                            stop();
                        } else {
                            Bank.close();
                            sleep(300, 600);
                        }
                    } else {
                        Bank.close();
                        sleep(300, 600);
                    }

                    // Walk back to the starting tile with dynamic path
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

        // Look for a rock within the radius when not mining
        String rockName; // e.g., "Copper rocks"
        if (oreName.equalsIgnoreCase("Daeyalt")) {
            rockName = "Daeyalt essence rocks";
        } else if (oreName.equalsIgnoreCase("Amethyst")) {
            rockName = "Amethyst crystals";
        } else {
            rockName = oreName + " rocks";
        }

        GameObject rock = GameObjects.closest(t ->
                t != null &&
                        t.getName().equalsIgnoreCase(rockName) &&
                        t.exists() &&
                        t.getTile().distance(startTile) <= TILE_RADIUS &&
                        t.hasAction("Mine")
        );
        if (rock != null && !searchLogged) {
            log("Searching for " + rockName + " at " + startTile.toString() + ", found: true");
            searchLogged = true;
        } else if (rock == null && !searchLogged) {
            log("Searching for " + rockName + " at " + startTile.toString() + ", found: false");
            searchLogged = true;
        }

        if (rock != null) {
            // Found a rock within radius, start mining
            if (!Players.getLocal().isAnimating() && !Players.getLocal().isMoving()) {
                status = "Mining rock...";
                rock.interact("Mine");
                isMining = true;
                sleepUntil(Players.getLocal()::isAnimating, 2000, 600);
            }
        } else {
            // No rock found within radius, wait for respawn
            status = "Waiting for rock to respawn...";
            sleep(2000, 3000);
        }

        return 100;
    }

    @Override
    public void onPaint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Antialiasing for smoother rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate runtime and XP gained
        long runTimeMillis = System.currentTimeMillis() - startTime;
        String runTime = formatTime(runTimeMillis);
        int currentXp = Skills.getExperience(Skill.MINING);
        int xpGained = currentXp - startXp;

        // Layout
        int x = 10;
        int y = 340;
        int width = 250;
        int height = 150;
        int arc = 16;

        // Background and border
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(x, y, width, height, arc, arc);

        g2.setColor(new Color(100, 180, 255));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, width, height, arc, arc);

        // Text styling
        g2.setFont(new Font("Verdana", Font.PLAIN, 12));
        g2.setColor(Color.WHITE);

        int lineHeight = 18;
        int textX = x + 12;
        int textY = y + 22;

        // Paint info lines
        g2.drawString("[he1zen AIO Miner]", textX, textY);
        g2.drawString("[Status] " + status, textX, textY + lineHeight * 2);
        g2.drawString("[Runtime] " + runTime, textX, textY + lineHeight * 3);
        g2.drawString("[Ores] " + oresMined, textX, textY + lineHeight * 4);
        g2.drawString("[XP Gained] " + xpGained, textX, textY + lineHeight * 5);
        g2.drawString("[Ore] " + oreName, textX, textY + lineHeight * 6);
        g2.drawString("[Action] " + (dropOres ? "Dropping" : "Banking"), textX, textY + lineHeight * 7);
    }
}