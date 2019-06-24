package lv.mtm123.cvcancer;

import co.aikar.commands.CommandConfig;
import co.aikar.commands.JDACommandManager;
import com.earth2me.essentials.Essentials;
import lv.mtm123.cvcancer.cmd.MentionsCommand;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.listeners.MessageListener;
import lv.mtm123.cvcancer.listeners.ChatListener;
import lv.mtm123.cvcancer.listeners.PlayerListener;
import lv.mtm123.cvcancer.listeners.ServerStatusListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

public final class CVCancer extends JavaPlugin {

    private Config config;
    @Nullable
    private JDA jda;
    private Essentials essentials;
    private ObjectMapper<Config>.BoundInstance configMapperInstance;
    private YAMLConfigurationLoader configLoader;

    public Config getPluginConfig() {
        return config;
    }

    public ObjectMapper<Config>.BoundInstance getConfigMapperInstance() {
        return configMapperInstance;
    }

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("Essentials")) {
            getLogger().log(Level.SEVERE, "Essentials not found! Disabling...");
            setEnabled(false);
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


        if (config.getStatusChannel() == 0) {
            warnErrorConfigProblem("Server status channel");
            return;
        }
        if (config.getChatLinkChannel() == 0) {
            warnErrorConfigProblem("Server chat link channel");
            return;
        }

        initJDA(config.getBotToken());

        if (jda == null) {
            getLogger().log(Level.SEVERE, "Unable to connect to JDA! Disabling...");
            setEnabled(false);
            return;
        }

        if (checkJdaChannels()) return;
        registerEvents();
    }

    private boolean checkJdaChannels() {
        assert jda != null;
        if (jda.getTextChannelById(config.getStatusChannel()) == null) {
            warnErrorConfigProblem("Server status channel");
            return true;
        }
        if (jda.getTextChannelById(config.getChatLinkChannel()) == null) {
            warnErrorConfigProblem("Server chat link channel");
            return true;
        }
        return false;
    }

    private void warnErrorConfigProblem(String config) {
        getLogger().log(Level.SEVERE, config + " is not configured. Please check your config!");
        setEnabled(false);
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, jda, config), this);
        Bukkit.getPluginManager().registerEvents(new ServerStatusListener(this, jda, config), this);
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdownNow();
        }
    }

    public YAMLConfigurationLoader getConfigLoader() {
        return configLoader;
    }

    private Config loadConfig() throws ObjectMappingException, IOException {
        File mainDir = getDataFolder().getAbsoluteFile();
        if (!mainDir.exists()) {
            //noinspection ResultOfMethodCallIgnored - Not interested in knowing the result
            mainDir.mkdirs();
        }

        File cfgFile = new File(getDataFolder().getAbsoluteFile(), "config.yml");

        configMapperInstance = ObjectMapper.forClass(Config.class).bindToNew();
        ObjectMapper<Config>.BoundInstance instance = configMapperInstance;

        configLoader = YAMLConfigurationLoader.builder()
                .setFile(cfgFile).setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
        YAMLConfigurationLoader loader = configLoader;

        //Pretty sure I'm doing this part wrong
        SimpleCommentedConfigurationNode node = SimpleCommentedConfigurationNode.root();
        if (!cfgFile.exists()) {
            instance.serialize(node);
            loader.save(node);
        }

        instance.populate(loader.load());

        return instance.getInstance();
    }

    public void savePluginConfig() {
        SimpleCommentedConfigurationNode node = SimpleCommentedConfigurationNode.root();

        try {
            configMapperInstance.serialize(node);
            configLoader.save(node);
        } catch (IOException | ObjectMappingException e) {
            getLogger().severe("An error occured while trying to save the config: " + e.getMessage());
        }
    }


    private void initJDA(String token) {
        try {
            jda = new JDABuilder(token).build();
            jda.awaitReady();
            jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);

            JDACommandManager commandManager = new JDACommandManager(jda);
            //Set prefix of the bot commands
            commandManager.setConfigProvider((CommandConfig) () -> Collections.singletonList("-"));
            registerJdaCommands(commandManager);

            jda.addEventListener(new MessageListener(this, config));
        } catch (LoginException | InterruptedException e) {
            getLogger().severe("Unable to init JDA. Reason: " + e.getMessage());
        }
    }

    private void registerJdaCommands(JDACommandManager commandManager) {
        commandManager.registerCommand(new MentionsCommand(this));
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
        return nickname == null ? player.getName() : ChatColor.stripColor(nickname);
    }

}
