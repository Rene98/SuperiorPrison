package com.bgsoftware.superiorprison.plugin.ladder.generator.manual;

import com.bgsoftware.superiorprison.plugin.SuperiorPrisonPlugin;
import com.bgsoftware.superiorprison.plugin.object.player.SPrisoner;
import com.bgsoftware.superiorprison.plugin.ladder.ObjectSupplier;
import com.bgsoftware.superiorprison.plugin.ladder.ParsedObject;
import com.bgsoftware.superiorprison.plugin.ladder.LadderTemplate;
import com.bgsoftware.superiorprison.plugin.requirement.RequirementMigrator;
import com.bgsoftware.superiorprison.plugin.util.NumberUtil;
import com.bgsoftware.superiorprison.plugin.util.script.variable.GlobalVariableMap;
import com.bgsoftware.superiorprison.plugin.util.script.variable.VariableHelper;
import com.oop.orangeengine.yaml.Config;
import com.oop.orangeengine.yaml.ConfigSection;
import lombok.Getter;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ManualObjectGenerator implements ObjectSupplier {

    @Getter
    private BigInteger maxIndex;

    public ManualObjectGenerator(Config config) {
        GlobalVariableMap variableMap = new GlobalVariableMap();
        variableMap.newOrReplace("prisoner", VariableHelper.createNullVariable(SPrisoner.class));
        variableMap.newOrReplace("index", VariableHelper.createVariable(1));
        variableMap.newOrReplace("index_formatted", VariableHelper.createNullVariable(String.class));
        handleVariableMapCreation(variableMap);

        AtomicReference<String> defaultPrefix = new AtomicReference<>(null);
        config.ifValuePresent("default prefix", String.class, p -> {
            defaultPrefix.set(defaultPrefixReplacer(p));
            config.get("default prefix").get().setObject(defaultPrefix.get());
        });

        // Load the prestiges
        for (ConfigSection ladderObjectSection : config.getSections().values()) {
            if (ladderObjectSection.getKey().length() > 1) {
                SuperiorPrisonPlugin.getInstance().getOLogger().printWarning(
                        "Found non supported rank type by name " + ladderObjectSection.getKey() + ", please remove it.");
                continue;
            }

            try {
                // Try to migrate requirements
                RequirementMigrator.migrate(ladderObjectSection);

                // Migrate old placeholders
                migratePlaceholders(ladderObjectSection);

                // Initialize prestige
                GlobalVariableMap prestigeMap = variableMap.clone();

                // Make sure prestige key is an number
                int index = ladderObjectSection.getAs("index", int.class);
                maxIndex = BigInteger.valueOf(index);

                // Replace the old index of the prestige to current
                prestigeMap.newOrReplace("index", VariableHelper.createVariable(index));
                prestigeMap.newOrReplace("index_formatted", VariableHelper.createVariable(NumberUtil.formatBigInt(BigInteger.valueOf(index))));
                handleVariableMapClone(prestigeMap, ladderObjectSection);

                // Get the template
                LadderTemplate ladderTemplate = new LadderTemplate(ladderObjectSection, prestigeMap);
                if (ladderTemplate.getPrefix() == null && defaultPrefix.get() != null)
                    ladderTemplate.setPrefix(defaultPrefix.get());

                ladderTemplate.initialize(prestigeMap);

                Function<SPrisoner, ParsedObject> parser = prisoner -> {
                    GlobalVariableMap prisonerMap = prestigeMap.clone();
                    prisonerMap.newOrReplace("prisoner", VariableHelper.createVariable(prisoner));
                    return ParsedObject.of(
                            ladderObjectSection.getKey(),
                            ladderTemplate,
                            prisonerMap,
                            () -> this.getParser(index + 1).map(f -> f.apply(prisoner)).orElse(null),
                            () -> this.getParser(index - 1).map(f -> f.apply(prisoner)).orElse(null),
                            BigInteger.valueOf(index)
                    );
                };

                registerObject(ladderObjectSection, index, parser);
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to load ladder at " + ladderObjectSection.getPath(), throwable);
            }
        }

        // Save the config
        config.save();
    }

    private void migratePlaceholders(ConfigSection prestigeSection) {
        prestigeSection
                .get("commands")
                .ifPresent(cv -> {
                    List<String> asList = cv.getAsList(String.class);
                    cv.setObject(
                            asList
                                    .stream()
                                    .map(v -> v.replace("{prisoner}", "%prisoner#player#name%"))
                                    .collect(Collectors.toList())
                    );
                });

        prestigeSection.ifValuePresent("order", int.class, v -> {
            prestigeSection.set("index", v);
            prestigeSection.set("order", null);
        });
    }

    // Register new ParsedObject function
    protected abstract void registerObject(ConfigSection section, int index, Function<SPrisoner, ParsedObject> parser);

    // Handle new global var map creation
    protected abstract void handleVariableMapCreation(GlobalVariableMap map);

    // Handle global var clone
    protected abstract void handleVariableMapClone(GlobalVariableMap map, ConfigSection section);


    public String defaultPrefixReplacer(String in) {
        return in;
    }
}
