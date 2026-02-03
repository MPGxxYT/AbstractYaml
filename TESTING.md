# AbstractYaml - Testing Guide

## Test Setup

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>

    <!-- MockBukkit for Bukkit API mocking -->
    <dependency>
        <groupId>com.github.seeseemelk</groupId>
        <artifactId>MockBukkit-v1.20</artifactId>
        <version>3.80.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

### Test Directory Structure

```
src/
├── main/java/...
└── test/java/me/mortaldev/abstractyaml/
    ├── ConfigSchemaTest.java
    ├── ConfigTest.java
    ├── ConfigLoaderTest.java
    ├── ConfigManagerTest.java
    ├── ConfigSectionTest.java
    ├── validators/
    │   ├── ScalarValidatorTest.java
    │   └── ListValidatorTest.java
    ├── values/
    │   ├── ScalarValuesTest.java
    │   ├── ListValuesTest.java
    │   └── MapValuesTest.java
    ├── writer/
    │   └── YamlConfigWriterTest.java
    └── testutil/
        ├── TestConfigSchema.java
        └── TestPlugin.java
```

---

## Automated Tests (JUnit/Mockito)

### 1. ConfigSchemaTest.java

```java
class ConfigSchemaTest {

    // --- Value Definition ---

    @Test
    void add_intValue_registersCorrectly()

    @Test
    void add_doubleValue_registersCorrectly()

    @Test
    void add_stringValue_registersCorrectly()

    @Test
    void add_booleanValue_registersCorrectly()

    @Test
    void add_stringListValue_registersCorrectly()

    @Test
    void add_intListValue_registersCorrectly()

    @Test
    void add_mapListValue_registersCorrectly()

    // --- Sections ---

    @Test
    void section_createsNestedPrefix()

    @Test
    void section_multipleNested_correctPath()

    @Test
    void parent_returnsToParentSection()

    @Test
    void parent_atRoot_returnsRoot()

    // --- Comments ---

    @Test
    void fileHeader_setsHeaderComment()

    @Test
    void sectionHeader_setsSectionComment()

    @Test
    void valueComment_setsInlineComment()

    // --- Defaults ---

    @Test
    void getDefaults_returnsAllDefaultValues()

    @Test
    void getDefaults_nestedSections_correctPaths()
}
```

### 2. ConfigTest.java

```java
class ConfigTest {

    // --- Direct Access ---

    @Test
    void getInt_withExistingPath_returnsValue()

    @Test
    void getInt_withMissingPath_returnsDefault()

    @Test
    void getDouble_withExistingPath_returnsValue()

    @Test
    void getString_withExistingPath_returnsValue()

    @Test
    void getBoolean_withExistingPath_returnsValue()

    @Test
    void getStringList_withExistingPath_returnsList()

    @Test
    void getIntList_withExistingPath_returnsList()

    @Test
    void getDoubleList_withExistingPath_returnsList()

    @Test
    void getMapList_withExistingPath_returnsList()

    // --- Optional Access ---

    @Test
    void get_withExistingPath_returnsOptionalWithValue()

    @Test
    void get_withMissingPath_returnsEmptyOptional()

    @Test
    void getValue_withExistingPath_returnsOptionalValue()

    @Test
    void getOrThrow_withExistingPath_returnsValue()

    @Test
    void getOrThrow_withMissingPath_throwsException()

    // --- Type Mismatches ---

    @Test
    void getInt_withStringValue_throwsOrReturnsDefault()

    @Test
    void getString_withIntValue_convertsToString()

    // --- Immutability ---

    @Test
    void with_returnsNewConfigInstance()

    @Test
    void with_originalUnchanged()

    @Test
    void withAll_appliesMultipleChanges()

    @Test
    void withAll_originalUnchanged()
}
```

### 3. ConfigSectionTest.java

```java
class ConfigSectionTest {

    // --- Section Creation ---

    @Test
    void section_createsWithPrefix()

    @Test
    void section_nestedSections_correctPrefix()

    // --- Relative Access ---

    @Test
    void getInt_withRelativePath_resolvesFully()

    @Test
    void getString_withRelativePath_resolvesFully()

    @Test
    void getStringList_withRelativePath_resolvesFully()

    // --- Section Modification ---

    @Test
    void set_withRelativePath_updatesCorrectly()

    @Test
    void set_returnsNewConfig()
}
```

### 4. ConfigLoaderTest.java

```java
@ExtendWith(MockitoExtension.class)
class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Mock
    JavaPlugin mockPlugin;

    // --- Loading ---

    @Test
    void load_withExistingFile_loadsValues()

    @Test
    void load_withMissingFile_usesDefaults()

    @Test
    void load_withAutoGenerate_createsFile()

    @Test
    void load_withoutAutoGenerate_noFileCreated()

    @Test
    void load_withPartialFile_mergesWithDefaults()

    @Test
    void load_withMalformedYaml_usesDefaults()

    // --- Validation on Load ---

    @Test
    void load_withValidateOnLoad_validatesValues()

    @Test
    void load_withInvalidValues_throwsConfigLoadException()

    @Test
    void load_withValidateOnLoadFalse_skipsValidation()

    // --- Reloading ---

    @Test
    void reload_reloadsFromDisk()

    @Test
    void reload_updatesConfigInstance()

    // --- Custom Paths ---

    @Test
    void load_withCustomFilePath_usesPath()

    @Test
    void load_createsParentDirectories()
}
```

### 5. ConfigManagerTest.java

```java
class ConfigManagerTest {

    @Mock
    JavaPlugin mockPlugin;

    @BeforeEach
    void setUp() {
        ConfigManager.reset(); // Clear singleton for test isolation
    }

    // --- Initialization ---

    @Test
    void init_setsPluginInstance()

    @Test
    void init_calledTwice_throwsIllegalStateException()

    @Test
    void getInstance_beforeInit_throwsIllegalStateException()

    @Test
    void getInstance_afterInit_returnsSameInstance()

    // --- Loading ---

    @Test
    void loadAll_loadsAllRegisteredConfigs()

    @Test
    void loadAll_respectsPriorityOrder()

    @Test
    void get_withValidName_returnsConfig()

    @Test
    void get_withInvalidName_returnsNull()

    // --- Reloading ---

    @Test
    void reloadAll_reloadsAllConfigs()

    @Test
    void reload_withName_reloadsSpecificConfig()

    @Test
    void reload_withInvalidName_handlesGracefully()
}
```

### 6. ScalarValidatorTest.java

```java
class ScalarValidatorTest {

    // --- Range ---

    @Test
    void range_withinRange_passes()

    @Test
    void range_belowMin_fails()

    @Test
    void range_aboveMax_fails()

    @Test
    void range_atBoundaries_passes()

    // --- Min/Max ---

    @Test
    void min_aboveMin_passes()

    @Test
    void min_belowMin_fails()

    @Test
    void max_belowMax_passes()

    @Test
    void max_aboveMax_fails()

    // --- String Validators ---

    @Test
    void notEmpty_withContent_passes()

    @Test
    void notEmpty_withEmpty_fails()

    @Test
    void notEmpty_withNull_fails()

    @Test
    void length_withinRange_passes()

    @Test
    void length_tooShort_fails()

    @Test
    void length_tooLong_fails()

    @Test
    void matches_validPattern_passes()

    @Test
    void matches_invalidPattern_fails()

    // --- OneOf ---

    @Test
    void oneOf_validValue_passes()

    @Test
    void oneOf_invalidValue_fails()

    // --- NotNull ---

    @Test
    void notNull_withValue_passes()

    @Test
    void notNull_withNull_fails()

    // --- Chaining ---

    @Test
    void and_bothPass_passes()

    @Test
    void and_firstFails_fails()

    @Test
    void and_secondFails_fails()

    // --- Custom ---

    @Test
    void of_customPredicate_works()

    @Test
    void of_customMessage_inResult()
}
```

### 7. ListValidatorTest.java

```java
class ListValidatorTest {

    // --- Size Validators ---

    @Test
    void minSize_aboveMin_passes()

    @Test
    void minSize_belowMin_fails()

    @Test
    void maxSize_belowMax_passes()

    @Test
    void maxSize_aboveMax_fails()

    @Test
    void sizeRange_withinRange_passes()

    @Test
    void sizeRange_outsideRange_fails()

    // --- Duplicate Check ---

    @Test
    void noDuplicates_unique_passes()

    @Test
    void noDuplicates_withDuplicates_fails()

    // --- Element Validators ---

    @Test
    void allStringsNotEmpty_allValid_passes()

    @Test
    void allStringsNotEmpty_hasEmpty_fails()

    @Test
    void allStringsMatch_allMatch_passes()

    @Test
    void allStringsMatch_someDontMatch_fails()

    @Test
    void allIntegersInRange_allValid_passes()

    @Test
    void allIntegersInRange_someOutOfRange_fails()

    @Test
    void allDoublesInRange_allValid_passes()

    @Test
    void allDoublesInRange_someOutOfRange_fails()

    // --- Custom Element Validator ---

    @Test
    void allMatch_customPredicate_works()
}
```

### 8. ScalarValuesTest.java

```java
class ScalarValuesTest {

    // --- Int ---

    @Test
    void int_createsWithDefaultValue()

    @Test
    void int_withComment_hasComment()

    @Test
    void int_withValidator_validatesOnWith()

    // --- Double ---

    @Test
    void double_createsWithDefaultValue()

    @Test
    void double_withValidator_validatesOnWith()

    // --- String ---

    @Test
    void string_createsWithDefaultValue()

    @Test
    void string_withValidator_validatesOnWith()

    // --- Boolean ---

    @Test
    void boolean_createsWithDefaultValue()

    @Test
    void boolean_trueAndFalse_workCorrectly()
}
```

### 9. ListValuesTest.java

```java
class ListValuesTest {

    // --- StringList ---

    @Test
    void stringList_createsWithDefaultValue()

    @Test
    void stringList_emptyDefault_works()

    @Test
    void stringList_withValidator_validates()

    // --- IntList ---

    @Test
    void intList_createsWithDefaultValue()

    @Test
    void intList_withValidator_validates()

    // --- DoubleList ---

    @Test
    void doubleList_createsWithDefaultValue()

    @Test
    void doubleList_withValidator_validates()
}
```

### 10. MapValuesTest.java

```java
class MapValuesTest {

    // --- MapValue ---

    @Test
    void mapValue_createsWithDefaultValue()

    @Test
    void mapValue_nestedMaps_work()

    // --- MapList ---

    @Test
    void mapList_createsWithDefaultValue()

    @Test
    void mapList_emptyDefault_works()

    @Test
    void mapList_withComplexMaps_works()

    // --- StringMap ---

    @Test
    void stringMap_createsWithDefaultValue()

    @Test
    void stringMap_accessByKey_works()

    // --- IntMap ---

    @Test
    void intMap_createsWithDefaultValue()

    @Test
    void intMap_accessByKey_works()
}
```

### 11. YamlConfigWriterTest.java

```java
class YamlConfigWriterTest {

    @TempDir
    Path tempDir;

    // --- Basic Writing ---

    @Test
    void write_createsValidYamlFile()

    @Test
    void write_scalarValues_correctFormat()

    @Test
    void write_nestedSections_correctIndentation()

    // --- Comments ---

    @Test
    void write_fileHeader_atTop()

    @Test
    void write_sectionHeaders_beforeSections()

    @Test
    void write_inlineComments_alignedCorrectly()

    // --- List Formatting ---

    @Test
    void write_stringList_multiLineFormat()

    @Test
    void write_intList_inlineFormat()

    @Test
    void write_doubleList_inlineFormat()

    @Test
    void write_emptyList_bracketFormat()

    @Test
    void write_mapList_multiLineFormat()

    // --- String Escaping ---

    @Test
    void write_stringWithQuotes_escaped()

    @Test
    void write_stringWithBackslash_escaped()

    @Test
    void write_stringWithSpecialChars_quoted()

    // --- Indentation ---

    @Test
    void write_twoSpaceIndent_perLevel()

    @Test
    void write_deeplyNested_correctIndent()

    // --- Blank Lines ---

    @Test
    void write_blankLinesBetweenTopLevelSections()
}
```

### 12. ValueContainerTest.java

```java
class ValueContainerTest {

    // --- Container Loading ---

    @Test
    void loadAllContainers_loadsAnnotatedContainers()

    @Test
    void container_withPrefix_resolvesPathsCorrectly()

    @Test
    void container_nestedValues_accessible()

    // --- Container Values ---

    @Test
    void container_getters_returnCorrectValues()

    @Test
    void container_afterReload_valuesUpdated()
}
```

---

## Manual Tests (Require Paper Server)

These tests require MockBukkit or a real Paper server because they depend on:
- Bukkit's YamlConfiguration internals
- Plugin lifecycle (onEnable/onDisable)
- File watcher behavior
- Real concurrent access

### Plugin Lifecycle

- [ ] ConfigManager initializes in onEnable
- [ ] All schemas load without errors
- [ ] `/reload` reloads configs correctly
- [ ] Config changes persist across restarts

### File System Integration

- [ ] Config auto-generates on first run
- [ ] Manual edits to file reflected after reload
- [ ] Invalid YAML doesn't crash plugin
- [ ] Missing parent directories created

### Real-World Validation

- [ ] Invalid config values show clear error messages
- [ ] Validation errors include path information
- [ ] Multiple validation errors reported together

### Edge Cases

- [ ] Very large config files (1000+ values)
- [ ] Unicode characters in strings
- [ ] Multi-line string values
- [ ] Empty config file (all defaults used)

### Performance

- [ ] Config access fast after initial load (caching)
- [ ] Large configs don't cause startup lag
- [ ] Memory stable with many configs

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ConfigLoaderTest

# Run with coverage
mvn test jacoco:report
```

---

## Version Checklist

Before tagging a release:

- [ ] All automated tests pass (`mvn test`)
- [ ] Manual server tests pass
- [ ] No compiler warnings
- [ ] README.md version updated
- [ ] CHANGELOG.md updated
