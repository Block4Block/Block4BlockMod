package hasjamon.block4block;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Block4BlockModClient implements ClientModInitializer {
	private final Set<Block> blockForBlockBlocks = new HashSet<>();
	private final Set<Block> freeToBreakBlocks = new HashSet<>();
	private final Set<Block> freeInClaimsBlocks = new HashSet<>();

	private boolean useAdvancedTooltip = true;
	private boolean useLore = false;
	private String blockForBlockText = "§cBlock for Block";
	private String freeToBreakText = "§aFree to Break";
	private String freeInClaimsText = "§bFree in Claims";

	// Config file names
	private static final String BLOCK_LISTS_FILE = "block_lists.yml";
	private static final String CONFIG_FILE = "config.yml";

	// Method to create default config.yml if it doesn't exist
	private void createDefaultConfig() {
		Path configPath = Path.of(FabricLoader.getInstance().getConfigDirectory().toString(), CONFIG_FILE);
		if (Files.notExists(configPath)) {
			try {
				Files.writeString(configPath, CONFIG_FILE, StandardOpenOption.CREATE);
				System.out.println("Default config.yml created.");
			} catch (IOException e) {
				System.err.println("Error creating default config.yml: " + e.getMessage());
			}
		}
	}

	// Method to create default block_lists.yml if it doesn't exist
	private void createDefaultBlockLists() {
		Path blockListsPath = Path.of(FabricLoader.getInstance().getConfigDirectory().toString(), BLOCK_LISTS_FILE);
		if (Files.notExists(blockListsPath)) {
			try {
				Files.writeString(blockListsPath, BLOCK_LISTS_FILE, StandardOpenOption.CREATE);
				System.out.println("Default block_lists.yml created.");
			} catch (IOException e) {
				System.err.println("Error creating default block_lists.yml: " + e.getMessage());
			}
		}
	}

	@Override
	public void onInitializeClient() {
		System.out.println("BlockStatus Client Mod Initializer loaded!");

		// Create default config files if they don't exist
		createDefaultConfig();
		createDefaultBlockLists();

		loadConfig();

		ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
			Item item = itemStack.getItem();
			if (item == null) return;

			Block block = Block.getBlockFromItem(item);
			if (block == null) return;

			if (blockForBlockBlocks.contains(block)) {
				addTooltip(list, blockForBlockText);
			} else if (freeToBreakBlocks.contains(block)) {
				addTooltip(list, freeToBreakText);
			} else if (freeInClaimsBlocks.contains(block)) {
				addTooltip(list, freeInClaimsText);
			}
		});
	}

	private void addTooltip(List<Text> tooltipList, String tooltipText) {
		tooltipList.add(Text.literal(tooltipText));
	}

	private void loadConfig() {
        // Load block lists from resources
        InputStream blockListsStream = getClass().getClassLoader().getResourceAsStream("block_lists.yml");
        if (blockListsStream != null) {
            loadBlockLists(blockListsStream);
        } else {
            System.err.println("Error: " + BLOCK_LISTS_FILE + " not found in resources.");
        }

        // Load other config file (config.yml) from resources
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (configStream != null) {
            loadConfigFile(configStream);
        } else {
            System.err.println("Error: " + CONFIG_FILE + " not found in resources.");
        }

        // Determine "Block for Block" blocks (all remaining blocks)
        for (Block block : Registries.BLOCK) {
            if (!freeToBreakBlocks.contains(block) && !freeInClaimsBlocks.contains(block)) {
                blockForBlockBlocks.add(block);
            }
        }

        System.out.println("Free in claims blocks loaded:");
        freeInClaimsBlocks.forEach(b -> System.out.println(" - " + Registries.BLOCK.getId(b)));

        System.out.println("Free to break blocks loaded:");
        freeToBreakBlocks.forEach(b -> System.out.println(" - " + Registries.BLOCK.getId(b)));

        System.out.println("BlockStatus: Loaded " + blockForBlockBlocks.size() + " Block for Block blocks, " +
                freeToBreakBlocks.size() + " free to break blocks, " +
                freeInClaimsBlocks.size() + " free in claims blocks");
    }

	private void loadConfigFile(InputStream configStream) {
		Yaml yaml = new Yaml();
		try (InputStreamReader reader = new InputStreamReader(configStream)) {
			// Load the config.yml file
			Map<String, Object> config = yaml.load(reader);

			// Process the config
			if (config != null) {
				// Load display configuration options
				if (config.containsKey("display")) {
					Map<String, Object> display = (Map<String, Object>) config.get("display");
					useAdvancedTooltip = (boolean) display.getOrDefault("useAdvancedTooltip", true);
					useLore = (boolean) display.getOrDefault("useLore", false);
					blockForBlockText = (String) display.getOrDefault("blockForBlockText", "§cBlock for Block");
					freeToBreakText = (String) display.getOrDefault("freeToBreakText", "§aFree to Break");
					freeInClaimsText = (String) display.getOrDefault("freeInClaimsText", "§bFree in Claims");
				}

				// Print loaded values (for debugging purposes)
				System.out.println("Config loaded:");
				System.out.println(" - useAdvancedTooltip: " + useAdvancedTooltip);
				System.out.println(" - useLore: " + useLore);
				System.out.println(" - blockForBlockText: " + blockForBlockText);
				System.out.println(" - freeToBreakText: " + freeToBreakText);
				System.out.println(" - freeInClaimsText: " + freeInClaimsText);
			} else {
				System.err.println("Error: config.yml is empty or not correctly formatted.");
			}
		} catch (IOException e) {
			System.err.println("Error loading config.yml from resources: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadBlockLists(InputStream blockListsStream) {
		Yaml yaml = new Yaml();
		try (InputStreamReader reader = new InputStreamReader(blockListsStream)) {
			Map<String, Object> lists = yaml.load(reader);

			// Clear previous sets
			freeInClaimsBlocks.clear();
			freeToBreakBlocks.clear();
			blockForBlockBlocks.clear();

			// Load blocks into respective sets
			List<String> claimBlocks = getListFromConfig(lists, "blacklisted-claim-blocks");
			List<String> breakBlocks = getListFromConfig(lists, "blacklisted-blocks");

			loadBlockListFromIds(claimBlocks, freeInClaimsBlocks);
			loadBlockListFromIds(breakBlocks, freeToBreakBlocks);

		} catch (IOException e) {
			System.err.println("Error loading BlockStatus block lists from resources: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadBlockListFromIds(List<String> blockIds, Set<Block> blockSet) {
		for (String blockId : blockIds) {
			try {
				Identifier id = Identifier.tryParse(blockId);
				if (id == null) {
					System.err.println("BlockStatus: Invalid block ID format: " + blockId);
					continue;
				}

				Block block = Registries.BLOCK.get(id);
				if (block != null) {
					blockSet.add(block);
					System.out.println("BlockStatus: Successfully added block: " + id);
				} else {
					System.err.println("BlockStatus: Unknown block ID: " + blockId);
				}
			} catch (Exception e) {
				System.err.println("BlockStatus: Error processing block ID: " + blockId);
				e.printStackTrace();
			}
		}
	}

	private List<String> getListFromConfig(Map<String, Object> config, String key) {
		List<String> result = new ArrayList<>();

		if (config.containsKey(key)) {
			Object obj = config.get(key);
			if (obj instanceof List<?>) {
				for (Object item : (List<?>) obj) {
					if (item instanceof String) {
						result.add((String) item);
					} else {
						System.err.println("BlockStatus: Invalid item in list for key " + key + ": " + item);
					}
				}
			} else {
				System.err.println("BlockStatus: Value for key " + key + " is not a list.");
			}
		} else {
			System.err.println("BlockStatus: Key " + key + " not found in config.");
		}

		return result;
	}
}