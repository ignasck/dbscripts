#  Setup Guide

This guide provides general instructions for setting up and running any script built for the DreamBot API in Old School RuneScape (OSRS). It is designed for beginners to help you get started with running custom scripts, including those for activities like fishing, mining, woodcutting, or other automated tasks.

## Prerequisites

To run a DreamBot script, you need the following:

- **DreamBot Client**: Download the DreamBot client from DreamBot's official website.
- **Java Development Kit (JDK)**: Version 8 or higher. Download from Oracle or use an open-source alternative like OpenJDK.
- **Development Environment** (optional): An IDE like IntelliJ IDEA or Eclipse for compiling scripts from source code.
- **OSRS Account**: An active Old School RuneScape account with the required skills and items for the script you want to run.
- **DreamBot Account**: Required to log in to the DreamBot client.

## Setup Instructions

1. **Install DreamBot Client**:

   - Download `DBLauncher.jar` from the official DreamBot website.
   - Place the `.jar` file in a dedicated folder (e.g., `C:\DreamBot\` on Windows or `~/DreamBot/` on Mac/Linux).

2. **Set Up DreamBot Scripts Folder**:

   - Locate or create the DreamBot scripts folder:
     - Windows: `C:\Users\YourUsername\DreamBot\Scripts\`
     - Mac/Linux: `~/DreamBot/Scripts/`
   - If the `Scripts` folder doesn’t exist, create it manually.

3. **Add the Script**:

   - Obtain the script file (`.java` or compiled `.jar`):
     - For a `.java` file, you need to compile it into a `.jar` file using an IDE or command-line tools.
     - For a pre-compiled `.jar` file, simply place it in the `Scripts` folder.
   - To compile a `.java` file:
     - Open the script in an IDE like IntelliJ IDEA or Eclipse.
     - Ensure the DreamBot API is included in your project dependencies (available via the DreamBot client or forums).
     - Compile the script to generate a `.jar` file.
     - Move the compiled `.jar` to the `Scripts` folder.

4. **Install Java**:

   - Verify that JDK 8 or higher is installed by running `java -version` in a terminal or command prompt.
   - If not installed, download and install it from the Oracle website or another trusted source.

5. **Prepare In-Game Requirements**:

   - Check the script’s documentation or source code for specific requirements (e.g., skill levels, items, or locations).
   - Ensure your OSRS character has the necessary skills, equipment, and items in your inventory or equipped.
   - Position your character in the appropriate in-game location before starting the script (e.g., near a fishing spot, mining rocks, or trees).

## How to Run a Script

1. **Launch DreamBot Client**:

   - Double-click `DBLauncher.jar` to start the DreamBot client.
   - Log in using your DreamBot account credentials.

2. **Add Your OSRS Account**:

   - In the DreamBot client, go to the account manager and add your OSRS account credentials (username and password).

3. **Start the Script**:

   - Open the DreamBot client and navigate to the "Scripts" tab.
   - Select the desired script from the list of available scripts in the `Scripts` folder.
   - Click "Start" to launch the script.

4. **Configure the Script** (if applicable):

   - Many scripts include a graphical user interface (GUI) for configuration when started.
   - Follow the on-screen prompts to select options like resource type, actions (e.g., drop or bank), or other settings.
   - Refer to the script’s documentation for specific configuration details.

5. **Position Your Character**:

   - Ensure your character is in the correct location for the script to function (e.g., near a resource or bank).
   - Some scripts automatically detect nearby resources, while others may require you to be at a specific spot.

## Common Script Features

While features vary by script, many DreamBot scripts include:

- **Resource Selection**: Choose specific resources (e.g., fish types, ore types, tree types).
- **Action Options**: Drop items, bank them, or perform other actions like cooking or smelting.
- **Anti-Ban Measures**: Random camera movements, mouse movements, skill checking, or idle periods to mimic human behavior.
- **Dynamic Movement**: Randomized walking paths to reduce repetitive patterns.
- **GUI Support**: User-friendly interfaces for setting up script options.
- **On-Screen Paint**: Displays runtime, resources gathered, XP gained, and status updates.

## Troubleshooting

- **Script Not Appearing**: Ensure the `.jar` or `.java` file is in the `Scripts` folder. If using a `.java` file, compile it to a `.jar`. Restart the DreamBot client.
- **Script Fails to Start**: Check the DreamBot console for error messages. Verify that you meet the script’s skill and item requirements.
- **Missing Items/Equipment**: Ensure you have the required items in your inventory or equipped, as specified by the script.
- **GUI Not Showing**: Confirm Java is installed correctly and the DreamBot client is up to date. Check the console for errors.
- **Script Stops Unexpectedly**: Review the script’s documentation for location or setup requirements. Ensure you’re in the correct area and have necessary resources.

## Notes

- **Bot Detection Risk**: Using automation scripts in OSRS carries a risk of account bans. Use scripts at your own discretion and consider anti-ban features included in the script.
- **Client Updates**: Regularly update the DreamBot client to ensure compatibility with OSRS and the script.
- **Script-Specific Documentation**: Always check the script’s documentation (e.g., a README file or comments in the source code) for specific requirements or instructions.
- **Support**: For issues with a specific script, contact the script author via the DreamBot forums or other provided channels.

## License

Scripts are typically provided for personal use/educational purposes. Do not distribute or sell scripts without the author’s permission.
