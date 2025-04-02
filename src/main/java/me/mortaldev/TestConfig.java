package me.mortaldev;

import org.bukkit.plugin.java.JavaPlugin;

public class TestConfig extends AbstractConfig {

  private ConfigValue<Integer> savingLength = new ConfigValue<>("savingLength", 3);
  private ConfigValue<Integer> afkThreshold = new ConfigValue<>("afkThreshold", 2);

  private static class Singleton {
    private static final TestConfig INSTANCE = new TestConfig();
  }

  public static TestConfig getInstance() {
    return Singleton.INSTANCE;
  }

  private TestConfig() {}

  @Override
  public void log(String message) {}

  @Override
  public String getName() {
    return "";
  }

  @Override
  public JavaPlugin getMain() {
    return null;
  }

  @Override
  public void loadData() {
    savingLength = getConfigValue(savingLength);
    afkThreshold = getConfigValue(afkThreshold);
  }

  public int getAfkThreshold() {
    return afkThreshold.getValue();
  }

  public void setAfkThreshold(int afkThreshold) {
    saveValue(this.afkThreshold.getId(), afkThreshold);
    this.afkThreshold.setValue(afkThreshold);
  }

  public int getSavingLength() {
    return savingLength.getValue();
  }

  public void setSavingLength(int savingLength) {
    saveValue(this.savingLength.getId(), savingLength);
    this.savingLength.setValue(savingLength);
  }
}
