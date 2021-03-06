package com.bgsoftware.superiorprison.plugin.ladder.generator.auto;

import com.bgsoftware.superiorprison.plugin.ladder.generator.auto.options.PrestigeGeneratorOptions;
import com.bgsoftware.superiorprison.plugin.ladder.generator.auto.options.RankGeneratorOptions;
import com.bgsoftware.superiorprison.plugin.util.script.variable.GlobalVariableMap;
import com.oop.orangeengine.yaml.ConfigSection;
import lombok.Getter;

import java.math.BigInteger;

@Getter
public abstract class GeneratorOptions<K> {
    private final GlobalVariableMap variableMap;

    public GeneratorOptions(GlobalVariableMap map) {
        this.variableMap = map;
    }

    public static <G extends GeneratorOptions> G of(ConfigSection options, GlobalVariableMap variableMap) {
        if (options.isValuePresent("range"))
            return (G) new RankGeneratorOptions(options, variableMap);
        return (G) new PrestigeGeneratorOptions(options, variableMap);
    }

    public abstract boolean hasNext(K key);

    public abstract boolean hasPrevious(K key);

    public abstract boolean isValid(K key);

    public abstract BigInteger getIndex(Object in);
}
