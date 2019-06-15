package lv.mtm123.cvcancer;

import com.earth2me.essentials.Essentials;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.listeners.MessageListener;
import lv.mtm123.cvcancer.listeners.ChatListener;
import lv.mtm123.cvcancer.listeners.PlayerListener;
import lv.mtm123.cvcancer.listeners.ServerStatusListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

public final class CVCancer extends JavaPlugin {
    private Config config;
    private JDA jda;
    private Essentials essentials;

    @Override
    public void onEnable() {

        if (!getServer().getPluginManager().isPluginEnabled("Essentials")) {
            getLogger().log(Level.SEVERE, "Essentials not found! Disabling...");
            return;
        }
        essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");

        registerCustomRecipes();

        try {
            config = loadConfig();
        } catch (ObjectMappingException | IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load config!", e);
            return;
        }

        initJDA(config.getBotToken());
    
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, config.getWebhookUrl()), this);
		Bukkit.getPluginManager().registerEvents(new ServerStatusListener(this, jda), this);
    }

    @Override
    public void onDisable() {
        jda.shutdownNow();
    }

    private Config loadConfig() throws ObjectMappingException, IOException {

        File mainDir = getDataFolder().getAbsoluteFile();
        if (!mainDir.exists()) {
            mainDir.mkdirs();
        }

        File cfgFile = new File(getDataFolder().getAbsoluteFile(), "config.yml");

        ObjectMapper<Config>.BoundInstance instance = ObjectMapper.forClass(Config.class).bindToNew();
        YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
                .setFile(cfgFile).setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();

        //Pretty sure I'm doing this part wrong
        SimpleCommentedConfigurationNode node = SimpleCommentedConfigurationNode.root();
        if (!cfgFile.exists()) {
            instance.serialize(node);
            loader.save(node);
        }

        instance.populate(loader.load());

        return instance.getInstance();

    }

    private void initJDA(String token) {
        try {
            jda = new JDABuilder(token).build();
            jda.addEventListener(new MessageListener(this));
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }


    private void registerCustomRecipes() {
        registerCarpetRecipes();
        //TODO: Add more recipes
    }

    private void registerCarpetRecipes() {

        Map<Material, Material> carpetMaterials = new EnumMap<>(Material.class);

        carpetMaterials.put(Material.INK_SAC, Material.BLACK_CARPET);
        carpetMaterials.put(Material.BONE_MEAL, Material.WHITE_CARPET);
        carpetMaterials.put(Material.ROSE_RED, Material.RED_CARPET);
        carpetMaterials.put(Material.CACTUS_GREEN, Material.GREEN_CARPET);
        carpetMaterials.put(Material.COCOA_BEANS, Material.BROWN_CARPET);
        carpetMaterials.put(Material.LAPIS_LAZULI, Material.BLUE_CARPET);
        carpetMaterials.put(Material.MAGENTA_DYE, Material.MAGENTA_CARPET);
        carpetMaterials.put(Material.CYAN_DYE, Material.CYAN_CARPET);
        carpetMaterials.put(Material.LIGHT_GRAY_DYE, Material.LIGHT_GRAY_CARPET);
        carpetMaterials.put(Material.GRAY_DYE, Material.GRAY_CARPET);
        carpetMaterials.put(Material.PINK_DYE, Material.PINK_CARPET);
        carpetMaterials.put(Material.LIME_DYE, Material.LIME_CARPET);
        carpetMaterials.put(Material.DANDELION_YELLOW, Material.YELLOW_CARPET);
        carpetMaterials.put(Material.LIGHT_BLUE_DYE, Material.LIGHT_BLUE_CARPET);
        carpetMaterials.put(Material.PURPLE_DYE, Material.PURPLE_CARPET);
        carpetMaterials.put(Material.ORANGE_DYE, Material.ORANGE_CARPET);

        carpetMaterials.forEach((k, v) -> carpetMaterials.values().forEach(base -> {
            NamespacedKey key = new NamespacedKey(this, String.format("%s_%s_%s", k.name().toLowerCase(),
                    base.name().toLowerCase(),
                    v.name().toLowerCase()));

            ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(v, 8));
            recipe.addIngredient(8, base);
            recipe.addIngredient(1, k);

            getServer().addRecipe(recipe);
        }));

    }

    public Essentials getEssentials() {
        return essentials;
    }

    public String getPlayerDiscordDisplayName(Player player) {
		String nickname = essentials.getUser(player).getNickname();
		return nickname == null ? player.getName() : nickname;
	}
}
