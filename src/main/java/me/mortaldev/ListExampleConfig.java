package me.mortaldev;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Example configuration demonstrating List-type ConfigValues with validators.
 */
public class ListExampleConfig extends AbstractConfig {
  private JavaPlugin plugin;

  // === List<String> Examples ===

  /** Simple string list */
  private ConfigValue<List> allowedCommands =
      new ConfigValue<>("allowed-commands", List.class, Arrays.asList("help", "info", "stats"));

  /** String list with size validation */
  private ConfigValue<List> blockedWords =
      new ConfigValue<>(
              "blocked-words",
              List.class,
              Arrays.asList("spam", "hack"))
          .setValidator(ListValidators.sizeRange(1, 50));

  /** String list with non-empty validation */
  private ConfigValue<List> playerNames =
      new ConfigValue<>(
              "whitelist.players",
              List.class,
              Arrays.asList("Steve", "Alex"))
          .setValidator(
              ListValidators.stringList(1, 100)
                  .addRule(
                      list -> {
                        for (Object obj : list) {
                          String s = (String) obj;
                          if (s.trim().isEmpty()) {
                            return false;
                          }
                        }
                        return true;
                      },
                      "Player names cannot be empty"));

  /** String list with regex pattern validation */
  private ConfigValue<List> itemCodes =
      new ConfigValue<>(
              "item-codes",
              List.class,
              Arrays.asList("ITEM_001", "ITEM_002"))
          .setValidator(ListValidators.allStringsMatch("^ITEM_\\d{3}$"));

  // === List<Integer> Examples ===

  /** Simple integer list */
  private ConfigValue<List> rewardLevels =
      new ConfigValue<>("reward-levels", List.class, Arrays.asList(1, 5, 10, 25, 50, 100));

  /** Integer list with range validation */
  private ConfigValue<List> teamSizes =
      new ConfigValue<>(
              "game.team-sizes",
              List.class,
              Arrays.asList(2, 4, 8))
          .setValidator(ListValidators.integerList(1, 10, 1, 16));

  /** Integer list with custom validation */
  private ConfigValue<List> experienceRewards =
      new ConfigValue<>(
              "rewards.experience",
              List.class,
              Arrays.asList(10, 25, 50, 100))
          .setValidator(
              ListValidators.allIntegersInRange(1, 1000)
                  .addRule(list -> list.size() >= 1, "Must have at least one reward tier")
                  .addRule(
                      list -> {
                        // Validate ascending order
                        for (int i = 1; i < list.size(); i++) {
                          if ((Integer) list.get(i) <= (Integer) list.get(i - 1)) {
                            return false;
                          }
                        }
                        return true;
                      },
                      "Rewards must be in ascending order"));

  // === List<Double> Examples ===

  /** Double list for multipliers */
  private ConfigValue<List> difficultyMultipliers =
      new ConfigValue<>(
              "difficulty.multipliers",
              List.class,
              Arrays.asList(0.5, 1.0, 1.5, 2.0))
          .setValidator(ListValidators.allDoublesInRange(0.1, 5.0));

  // === List<Boolean> Examples ===

  /** Boolean list for feature flags */
  private ConfigValue<List> featureToggles =
      new ConfigValue<>(
              "features.enabled",
              List.class,
              Arrays.asList(true, false, true, true))
          .setValidator(
              ListValidators.allBooleansValid()
                  .addRule(list -> list.size() == 4, "Must have exactly 4 feature flags"));

  // === Complex Validation Examples ===

  /** No duplicates allowed */
  private ConfigValue<List> uniqueRegions =
      new ConfigValue<>(
              "regions",
              List.class,
              Arrays.asList("spawn", "arena", "shop"))
          .setValidator(
              ListValidators.stringList(1, 20)
                  .addRule(
                      list -> list.size() == list.stream().distinct().count(),
                      "Region names must be unique"));

  /** Combination of validators */
  private ConfigValue<List> bannedItems =
      new ConfigValue<>(
              "banned-items",
              List.class,
              Arrays.asList("TNT", "LAVA_BUCKET"))
          .setValidator(
              ListValidators.minSize(1)
                  .addRule(
                      list -> list.stream().allMatch(obj -> obj instanceof String),
                      "All items must be strings")
                  .addRule(
                      list -> {
                        for (Object obj : list) {
                          String s = (String) obj;
                          if (!s.matches("^[A-Z_]+$")) {
                            return false;
                          }
                        }
                        return true;
                      },
                      "Item names must be uppercase with underscores only"));

  // === Singleton Pattern ===

  private static final ListExampleConfig INSTANCE = new ListExampleConfig();

  public static ListExampleConfig getInstance() {
    return INSTANCE;
  }

  private ListExampleConfig() {}

  public void setPlugin(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public JavaPlugin getMain() {
    return plugin;
  }

  @Override
  public String getName() {
    return "list-config";
  }

  @Override
  public void log(String message) {
    if (plugin != null) {
      plugin.getLogger().info(message);
    }
  }

  @Override
  public void loadData() {
    // Load all config values
    allowedCommands = getConfigValue(allowedCommands);
    blockedWords = getConfigValue(blockedWords);
    playerNames = getConfigValue(playerNames);
    itemCodes = getConfigValue(itemCodes);

    rewardLevels = getConfigValue(rewardLevels);
    teamSizes = getConfigValue(teamSizes);
    experienceRewards = getConfigValue(experienceRewards);

    difficultyMultipliers = getConfigValue(difficultyMultipliers);

    featureToggles = getConfigValue(featureToggles);

    uniqueRegions = getConfigValue(uniqueRegions);
    bannedItems = getConfigValue(bannedItems);

    // Log validation failures
    if (!experienceRewards.isValid()) {
      log(
          "Warning: experienceRewards failed validation: "
              + experienceRewards.validate().getErrorMessage());
    }
  }

  // === Getters ===

  @SuppressWarnings("unchecked")
  public List<String> getAllowedCommands() {
    return (List<String>) (List<?>) allowedCommands.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<String> getBlockedWords() {
    return (List<String>) (List<?>) blockedWords.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<String> getPlayerNames() {
    return (List<String>) (List<?>) playerNames.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<Integer> getRewardLevels() {
    return (List<Integer>) (List<?>) rewardLevels.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<Integer> getTeamSizes() {
    return (List<Integer>) (List<?>) teamSizes.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<Integer> getExperienceRewards() {
    return (List<Integer>) (List<?>) experienceRewards.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<Double> getDifficultyMultipliers() {
    return (List<Double>) (List<?>) difficultyMultipliers.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<Boolean> getFeatureToggles() {
    return (List<Boolean>) (List<?>) featureToggles.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<String> getUniqueRegions() {
    return (List<String>) (List<?>) uniqueRegions.getValue();
  }

  @SuppressWarnings("unchecked")
  public List<String> getBannedItems() {
    return (List<String>) (List<?>) bannedItems.getValue();
  }

  // === Setters ===

  public void setAllowedCommands(List<String> commands) {
    allowedCommands.setValue((List) commands);
    saveValue(allowedCommands);
  }

  public void setRewardLevels(List<Integer> levels) {
    rewardLevels.setValue((List) levels);
    saveValue(rewardLevels);
  }

  public void setFeatureToggles(List<Boolean> toggles) {
    featureToggles.setValue((List) toggles);
    saveValue(featureToggles);
  }
}
