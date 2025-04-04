package hasjamon.block4block;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
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
	// We'll keep the BlockForBlock set as actual Block instances
	private final Set<Block> blockForBlockBlocks = new HashSet<>();
	// Instead of storing exception blocks as Block instances, we store their registry IDs.
	private final Set<Identifier> freeToBreakIds = new HashSet<>();
	private final Set<Identifier> freeInClaimsIds = new HashSet<>();

	private boolean useAdvancedTooltip = true;
	private boolean useLore = false;
	private String blockForBlockText = "§cBlock for Block";
	private String freeToBreakText = "§aFree to Break";
	private String freeInClaimsText = "§bFree in Claims";

	// Config file names
	private static final String BLOCK_LISTS_FILE = "block_lists.yml";
	private static final String CONFIG_FILE = "config.yml";

	// Create default config.yml if it doesn't exist
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

	// Create default block_lists.yml if it doesn't exist
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

		// Register tooltip callback. Only process items that are BlockItems.
		ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
			Item item = itemStack.getItem();
			if (!(item instanceof BlockItem)) {
				return;
			}
			Block block = ((BlockItem) item).getBlock();
			Identifier id = Registries.BLOCK.getId(block);

			if (freeToBreakIds.contains(id)) {
				addTooltip(list, freeToBreakText);
			} else if (freeInClaimsIds.contains(id)) {
				addTooltip(list, freeInClaimsText);
			} else if (blockForBlockBlocks.contains(block)) {
				addTooltip(list, blockForBlockText);
			}
		});
	}

	private void addTooltip(List<Text> tooltipList, String tooltipText) {
		tooltipList.add(Text.literal(tooltipText));
	}

	private void loadConfig() {
		// Load block lists from resources
		InputStream blockListsStream = getClass().getClassLoader().getResourceAsStream(BLOCK_LISTS_FILE);
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

		// Populate the "Block for Block" set with all placeable blocks
		// that are not explicitly marked as exceptions.
		for (Block block : Registries.BLOCK) {
			// Only consider blocks that are placeable.
			if (!(block.asItem() instanceof BlockItem)) {
				continue;
			}
			Identifier id = Registries.BLOCK.getId(block);
			if (!freeToBreakIds.contains(id) && !freeInClaimsIds.contains(id)) {
				blockForBlockBlocks.add(block);
			}
		}

		System.out.println("Free in claims blocks loaded:");
		freeInClaimsIds.forEach(id -> System.out.println(" - " + id));

		System.out.println("Free to break blocks loaded:");
		freeToBreakIds.forEach(id -> System.out.println(" - " + id));

		System.out.println("BlockStatus: Loaded " + blockForBlockBlocks.size() + " Block for Block blocks, " +
				freeToBreakIds.size() + " free to break blocks, " +
				freeInClaimsIds.size() + " free in claims blocks");
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

				// Debug output for loaded values
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
			freeInClaimsIds.clear();
			freeToBreakIds.clear();
			blockForBlockBlocks.clear();

			// Load block IDs from config for both free in claims and free to break
			List<String> claimBlocks = getListFromConfig(lists, "blacklisted-claim-blocks");
			List<String> breakBlocks = getListFromConfig(lists, "blacklisted-blocks");

			loadBlockListFromIds(claimBlocks, freeInClaimsIds);
			loadBlockListFromIds(breakBlocks, freeToBreakIds);

		} catch (IOException e) {
			System.err.println("Error loading BlockStatus block lists from resources: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Loads a list of block IDs (as Strings) into the provided set.
	 * Only adds the block if it exists in the registry and is placeable.
	 */
	private void loadBlockListFromIds(List<String> blockIds, Set<Identifier> idSet) {
		for (String blockId : blockIds) {
			try {
				Identifier id = Identifier.tryParse(blockId);
				if (id == null) {
					System.err.println("BlockStatus: Invalid block ID format: " + blockId);
					continue;
				}
				Block block = Registries.BLOCK.get(id);
				if (block != null && (block.asItem() instanceof BlockItem)) {
					idSet.add(id);
					System.out.println("BlockStatus: Successfully added block: " + id);
				} else {
					System.out.println("BlockStatus: Block " + id + " is either unknown or not placeable and will not be added.");
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
