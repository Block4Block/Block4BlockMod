package hasjamon.block4block;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Block4BlockModClient implements ClientModInitializer {
	// Sets to store blocks in different categories
	private final Set<Block> BlockForBlockBlocks = new HashSet<>();
	private final Set<Block> freeToBreakBlocks = new HashSet<>();
	private final Set<Block> freeInClaimsBlocks = new HashSet<>();

	// Config options
	private boolean useAdvancedTooltip = true;
	private boolean useLore = false;
	private String BlockForBlockText = "§cBlock for Block";
	private String freeToBreakText = "§aFree to Break";
	private String freeInClaimsText = "§bFree in Claim";

	@Override
	public void onInitializeClient() {
		System.out.println("BlockStatus Client Mod Initializer loaded!");

		// Load configuration
		loadConfig();

		// Register tooltip callback
		ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
			Item item = itemStack.getItem();
			if (item == null) return;

			// Get the block associated with this item, if any
			Block block = Block.getBlockFromItem(item);
			if (block == null) return;

			// Check which category the block belongs to and add appropriate tooltip
			if (BlockForBlockBlocks.contains(block)) {
				addTooltip(list, BlockForBlockText);
			} else if (freeToBreakBlocks.contains(block)) {
				addTooltip(list, freeToBreakText);
			} else if (freeInClaimsBlocks.contains(block)) {
				addTooltip(list, freeInClaimsText);
			}
		});
	}

	/**
	 * Adds tooltip text based on configuration (either as lore or advanced tooltip)
	 */
	private void addTooltip(java.util.List<Text> tooltipList, String tooltipText) {
		tooltipList.add(Text.literal(tooltipText));
	}

	/**
	 * Loads configuration from config file or creates default one if not present
	 */
	private void loadConfig() {
		File configDir = FabricLoader.getInstance().getConfigDir().toFile();
		if (!configDir.exists()) {
			configDir.mkdirs();
		}

		File configFile = new File(configDir, "blockstatus.json");
		if (!configFile.exists()) {
			createDefaultConfig(configFile);
		}

		try (FileReader reader = new FileReader(configFile)) {
			Gson gson = new Gson();
			JsonObject config = gson.fromJson(reader, JsonObject.class);

			// Load display settings
			if (config.has("display")) {
				JsonObject display = config.getAsJsonObject("display");
				useAdvancedTooltip = display.has("useAdvancedTooltip") ? display.get("useAdvancedTooltip").getAsBoolean() : true;
				useLore = display.has("useLore") ? display.get("useLore").getAsBoolean() : false;

				if (display.has("BlockForBlockText")) BlockForBlockText = display.get("BlockForBlockText").getAsString();
				if (display.has("freeToBreakText")) freeToBreakText = display.get("freeToBreakText").getAsString();
				if (display.has("freeInClaimsText")) freeInClaimsText = display.get("freeInClaimsText").getAsString();
			}

			// Load block lists
			loadBlockList(config, "BlockForBlockBlocks", BlockForBlockBlocks);
			loadBlockList(config, "freeToBreakBlocks", freeToBreakBlocks);
			loadBlockList(config, "freeInClaimsBlocks", freeInClaimsBlocks);

			System.out.println("BlockStatus: Loaded " + BlockForBlockBlocks.size() + " tool blocks, " +
					freeToBreakBlocks.size() + " free blocks, " +
					freeInClaimsBlocks.size() + " free in claims blocks");

		} catch (IOException e) {
			System.err.println("Error loading BlockStatus config: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Creates default configuration file
	 */
	private void createDefaultConfig(File configFile) {
		try (FileWriter writer = new FileWriter(configFile)) {
			JsonObject config = new JsonObject();

			// Display settings
			JsonObject display = new JsonObject();
			display.addProperty("useAdvancedTooltip", true);
			display.addProperty("useLore", false);
			display.addProperty("BlockForBlockText", "§cBlock for Block");
			display.addProperty("freeToBreakText", "§aFree to Break");
			display.addProperty("freeInClaimsText", "§bFree in Claims");
			config.add("display", display);

			// Example block lists
			JsonArray BlockForBlockList = new JsonArray();
			BlockForBlockList.add("minecraft:stone");
			BlockForBlockList.add("minecraft:iron_ore");
			config.add("BlockForBlockBlocks", BlockForBlockList);

			JsonArray freeToBreakList = new JsonArray();
			freeToBreakList.add("minecraft:dirt");
			freeToBreakList.add("minecraft:sand");
			config.add("freeToBreakBlocks", freeToBreakList);

			JsonArray freeInClaimsList = new JsonArray();
			freeInClaimsList.add("minecraft:grass_block");
			freeInClaimsList.add("minecraft:gravel");
			config.add("freeInClaimsBlocks", freeInClaimsList);

			Gson gson = new Gson();
			writer.write(gson.toJson(config));

			System.out.println("BlockStatus: Created default config file");
		} catch (IOException e) {
			System.err.println("Error creating default BlockStatus config: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Loads a list of blocks from config into a Set
	 */
	private void loadBlockList(JsonObject config, String listName, Set<Block> blockSet) {
		if (!config.has(listName)) return;

		JsonArray blockList = config.getAsJsonArray(listName);
		for (JsonElement element : blockList) {
			String blockId = element.getAsString();
			try {
				Identifier id = Identifier.tryParse(blockId);
				if (id == null) {
					System.err.println("BlockStatus: Invalid block ID format: " + blockId);
					continue;
				}
				Block block = Registries.BLOCK.get(id);
				if (block != null) {
					blockSet.add(block);
				} else {
					System.err.println("BlockStatus: Unknown block ID: " + blockId);
				}
			} catch (Exception e) {
				System.err.println("BlockStatus: Invalid block ID format: " + blockId);
			}
		}
	}
}