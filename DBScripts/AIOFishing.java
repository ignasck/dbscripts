import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
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
import org.dreambot.api.wrappers.interactive.GameObject;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

@ScriptManifest(author = "he1zen", name = "AIO Fisher", version = 1.0, description = "Fishes with configurable options including bait system", category = Category.FISHING)
public class AIOFishing extends AbstractScript {

    // === SETTINGS ===
    private final int TILE_RADIUS = 10; // Search radius from starting tile
    private String fishType = "Shrimp"; // Default fish type
    private String fishingAction = "Net"; // Default fishing action
    private int baitId = 0; // Default to no bait (0 means no bait required)
    private String baitName = null; // Name of bait item (null if no bait)
    private boolean dropFish = true; // Default to dropping fish
    private String status = "Starting...";
    private Tile startTile;
    private boolean isFishing = false;
    private long startTime;
    private int startXp;
    private int fishCaught;
    private boolean isGuiComplete = false;
    private boolean searchLogged = false;

    @Override
    public void onStart() {
        // Show GUI for fish type, bait, and drop/bank selection
        SwingUtilities.invokeLater(() -> {
            try {
                String[] fishOptions = {"Shrimp", "Sardine", "Herring", "Anchovies", "Trout", "Salmon", "Tuna", "Lobster", "Swordfish", "Monkfish", "Shark"};
                String selectedFish = (String) JOptionPane.showInputDialog(
                        null,
                        "Select the fish type to catch:",
                        "Fish Selection",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        fishOptions,
                        fishOptions[0]
                );

                if (selectedFish != null && !selectedFish.isEmpty()) {
                    fishType = selectedFish.trim();
                    log("Selected fish type: " + fishType);
                } else {
                    fishType = "Shrimp";
                    log("Fish selection canceled or invalid, defaulting to Shrimp.");
                }

                // Set fishing action and bait requirements
                configureFishSettings();

                // Validate Fishing level
                int requiredLevel = getRequiredLevel();
                if (Skills.getRealLevel(Skill.FISHING) < requiredLevel) {
                    log("Error: You need level " + requiredLevel + " Fishing to catch " + fishType + ". Current level: " + Skills.getRealLevel(Skill.FISHING));
                    JOptionPane.showMessageDialog(null, "You need level " + requiredLevel + " Fishing to catch " + fishType + ".\nCurrent level: " + Skills.getRealLevel(Skill.FISHING), "Level Requirement", JOptionPane.ERROR_MESSAGE);
                    stop();
                    return;
                }

                // Check for fishing equipment
                if (!hasFishingEquipment()) {
                    log("Error: Required fishing equipment or bait not found.");
                    JOptionPane.showMessageDialog(null, "Required fishing equipment or bait not found.", "Equipment Requirement", JOptionPane.ERROR_MESSAGE);
                    stop();
                    return;
                }

                String[] dropBankOptions = {"Drop Fish", "Bank Fish"};
                int dropBankChoice = JOptionPane.showOptionDialog(
                        null,
                        "Select what to do with fish:",
                        "Drop/Bank Selection",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        dropBankOptions,
                        dropBankOptions[0]
                );

                if (dropBankChoice != -1) {
                    dropFish = dropBankChoice == 0;
                    log("Selected action: " + (dropFish ? "Dropping" : "Banking"));
                } else {
                    dropFish = true;
                    log("Drop/Bank selection canceled, defaulting to dropping fish.");
                }

                // Prompt for bait amount if bait is required
                if (requiresBait()) {
                    String baitInput = JOptionPane.showInputDialog(
                            null,
                            "Enter the number of bait to use (0 for no bait):",
                            "Bait Selection",
                            JOptionPane.PLAIN_MESSAGE
                    );
                    try {
                        baitId = Integer.parseInt(baitInput.trim());
                        if (baitId < 0) {
                            baitId = 0;
                            log("Invalid bait input, defaulting to no bait.");
                        } else {
                            log("Selected bait amount: " + baitId);
                        }
                    } catch (NumberFormatException e) {
                        baitId = 0;
                        log("Invalid bait input, defaulting to no bait.");
                    }
                }
            } catch (Exception e) {
                log("Error in GUI setup: " + e.getMessage());
                log("Stack trace: " + Arrays.toString(e.getStackTrace()));
                fishType = "Shrimp";
                dropFish = true;
                configureFishSettings();
                log("GUI setup failed. Defaulting to Shrimp and dropping fish.");
            } finally {
                isGuiComplete = true;
            }
        });

        // Wait for GUI to complete
        sleepUntil(() -> isGuiComplete, 30000, 500);

        startTile = Players.getLocal().getTile();
        log("Starting tile set to: " + startTile.toString());
        startTime = System.currentTimeMillis();
        startXp = Skills.getExperience(Skill.FISHING);
        fishCaught = 0;
        log("Started AIO Fisher for " + fishType + " with " + (dropFish ? "dropping" : "banking") + " fish.");
    }

    private void configureFishSettings() {
        switch (fishType.toLowerCase()) {
            case "shrimp":
            case "anchovies":
                fishingAction = "Net";
                baitName = null;
                break;
            case "sardine":
            case "herring":
                fishingAction = "Bait";
                baitName = "Fishing bait";
                break;
            case "trout":
            case "salmon":
                fishingAction = "Lure";
                baitName = "Feather";
                break;
            case "tuna":
            case "swordfish":
                fishingAction = "Harpoon";
                baitName = null;
                break;
            case "lobster":
                fishingAction = "Cage";
                baitName = null;
                break;
            case "monkfish":
                fishingAction = "Net";
                baitName = null;
                break;
            case "shark":
                fishingAction = "Harpoon";
                baitName = null;
                break;
            default:
                fishingAction = "Net";
                baitName = null;
        }
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private int getRequiredLevel() {
        switch (fishType.toLowerCase()) {
            case "shrimp":
            case "anchovies":
                return 1;
            case "sardine":
            case "herring":
                return 10;
            case "trout":
                return 20;
            case "salmon":
                return 30;
            case "tuna":
                return 35;
            case "lobster":
                return 40;
            case "swordfish":
                return 50;
            case "monkfish":
                return 62;
            case "shark":
                return 76;
            default:
                return 1;
        }
    }

    private String getFishItemName() {
        switch (fishType.toLowerCase()) {
            case "shrimp":
                return "Raw shrimps";
            case "sardine":
                return "Raw sardine";
            case "herring":
                return "Raw herring";
            case "anchovies":
                return "Raw anchovies";
            case "trout":
                return "Raw trout";
            case "salmon":
                return "Raw salmon";
            case "tuna":
                return "Raw tuna";
            case "lobster":
                return "Raw lobster";
            case "swordfish":
                return "Raw swordfish";
            case "monkfish":
                return "Raw monkfish";
            case "shark":
                return "Raw shark";
            default:
                return "Raw " + fishType.toLowerCase();
        }
    }

    private double getXpPerFish() {
        switch (fishType.toLowerCase()) {
            case "shrimp":
                return 10.0;
            case "anchovies":
                return 40.0;
            case "sardine":
                return 20.0;
            case "herring":
                return 30.0;
            case "trout":
                return 50.0;
            case "salmon":
                return 70.0;
            case "tuna":
                return 80.0;
            case "lobster":
                return 90.0;
            case "swordfish":
                return 100.0;
            case "monkfish":
                return 120.0;
            case "shark":
                return 110.0;
            default:
                return 10.0;
        }
    }

    private boolean requiresBait() {
        return baitName != null;
    }

    private boolean hasFishingEquipment() {
        String[] equipment = getRequiredEquipment();
        for (String item : equipment) {
            if (!Inventory.contains(item) && !Players.getLocal().getEquipment().contains(item)) {
                return false;
            }
        }
        if (requiresBait() && baitId > 0) {
            return Inventory.count(baitName) >= baitId;
        }
        return true;
    }

    private String[] getRequiredEquipment() {
        switch (fishingAction.toLowerCase()) {
            case "net":
                return new String[]{"Small fishing net"};
            case "bait":
                return new String[]{"Fishing rod"};
            case "lure":
                return new String[]{"Fly fishing rod"};
            case "harpoon":
                return new String[]{"Harpoon"};
            case "cage":
                return new String[]{"Lobster pot"};
            default:
                return new String[]{"Small fishing net"};
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

            int parentId = 320;
            int fishingChildId = 22; // Fishing skill widget
            WidgetChild fishingWidget = Widgets.get(parentId, fishingChildId);

            if (fishingWidget != null && fishingWidget.isVisible()) {
                fishingWidget.interact("Hover");
                sleep(Calculations.random(800, 1200));
            } else {
                log("Fishing widget not found or not visible!");
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
        if (Calculations.random(0, 100) < 10) {
            performAntiBan();
        }

        if (isFishing) {
            status = "Fishing...";
            int xpBefore = Skills.getExperience(Skill.FISHING);
            sleepUntil(() -> !Players.getLocal().isAnimating(), 10000, 600);
            int xpAfter = Skills.getExperience(Skill.FISHING);
            int xpGained = xpAfter - xpBefore;
            double xpPerFish = getXpPerFish();
            int fishGained = (int) Math.round((double) xpGained / xpPerFish);
            fishCaught += fishGained;
            log("Gained " + xpGained + " XP, caught " + fishGained + " fish, total: " + fishCaught + ", xpPerFish: " + xpPerFish);
            isFishing = false;
            return 100;
        }

        if (Inventory.isFull()) {
            String fishItemName = getFishItemName();
            if (dropFish) {
                status = "Dropping " + fishItemName + "...";
                Inventory.dropAll(fishItemName);
                sleepUntil(() -> !Inventory.isFull(), 5000, 600);
                if (Inventory.isFull()) {
                    log("Failed to drop " + fishItemName + "! Inventory still full. Attempting to drop all items...");
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
                status = "Banking " + fishItemName + "...";
                if (!Bank.isOpen()) {
                    Bank.open();
                    sleepUntil(Bank::isOpen, 5000, 600);
                }
                if (Bank.isOpen()) {
                    status = "Depositing " + fishItemName + "...";
                    Bank.depositAll(fishItemName);
                    sleepUntil(Inventory::isEmpty, 5000, 600);
                    if (!Inventory.isEmpty()) {
                        log("Failed to deposit " + fishItemName + "! Inventory not empty. Attempting to deposit all...");
                        Bank.depositAll(fishItemName);
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

        if (requiresBait() && baitId > 0 && Inventory.count(baitName) == 0) {
            log("No bait left in inventory! Stopping script.");
            stop();
            return 100;
        }

        GameObject fishingSpot = GameObjects.closest(t ->
                t != null &&
                        t.getName().equalsIgnoreCase("Fishing spot") &&
                        t.exists() &&
                        t.getTile().distance(startTile) <= TILE_RADIUS &&
                        t.hasAction(fishingAction)
        );
        if (fishingSpot != null && !searchLogged) {
            log("Searching for Fishing spot at " + startTile.toString() + ", found: true");
            searchLogged = true;
        } else if (fishingSpot == null && !searchLogged) {
            log("Searching for Fishing spot at " + startTile.toString() + ", found: false");
            searchLogged = true;
        }

        if (fishingSpot != null) {
            if (!Players.getLocal().isAnimating() && !Players.getLocal().isMoving()) {
                status = "Fishing spot...";
                fishingSpot.interact(fishingAction);
                isFishing = true;
                sleepUntil(Players.getLocal()::isAnimating, 2000, 600);
            }
        } else {
            status = "Waiting for fishing spot to respawn...";
            sleep(2000, 3000);
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
        int currentXp = Skills.getExperience(Skill.FISHING);
        int xpGained = currentXp - startXp;

        int x = 10;
        int y = 340;
        int width = 250;
        int height = 150;
        int arc = 16;

        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(x, y, width, height, arc, arc);

        g2.setColor(new Color(100, 180, 255));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, width, height, arc, arc);

        g2.setFont(new Font("Verdana", Font.PLAIN, 12));
        g2.setColor(Color.WHITE);

        int lineHeight = 18;
        int textX = x + 12;
        int textY = y + 22;

        g2.drawString("[he1zen AIO Fisher]", textX, textY);
        g2.drawString("[Status] " + status, textX, textY + lineHeight * 2);
        g2.drawString("[Runtime] " + runTime, textX, textY + lineHeight * 3);
        g2.drawString("[Fish Caught] " + fishCaught, textX, textY + lineHeight * 4);
        g2.drawString("[XP Gained] " + xpGained, textX, textY + lineHeight * 5);
        g2.drawString("[Fish] " + fishType, textX, textY + lineHeight * 6);
        g2.drawString("[Action] " + (dropFish ? "Dropping" : "Banking"), textX, textY + lineHeight * 7);
    }
}