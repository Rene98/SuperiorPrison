package com.bgsoftware.superiorprison.plugin.config.backpack;

import com.bgsoftware.superiorprison.plugin.object.backpack.SBackPack;
import com.bgsoftware.superiorprison.plugin.object.player.SPrisoner;
import com.bgsoftware.superiorprison.plugin.requirement.RequirementController;
import com.bgsoftware.superiorprison.plugin.requirement.RequirementHolder;
import com.bgsoftware.superiorprison.plugin.requirement.RequirementMigrator;
import com.bgsoftware.superiorprison.plugin.util.script.variable.GlobalVariableMap;
import com.bgsoftware.superiorprison.plugin.util.script.variable.VariableHelper;
import com.oop.orangeengine.yaml.ConfigSection;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BackPackUpgrade<T extends BackPackConfig<T>> {
    private List<String> description = new ArrayList<>();
    private final RequirementHolder requirementHolder;
    private final T config;

    private final GlobalVariableMap variableMap = new GlobalVariableMap();

    public BackPackUpgrade(ConfigSection section, T config) {
        this.config = config;

        variableMap.newOrPut("prisoner", () -> VariableHelper.createNullVariable(SPrisoner.class));
        variableMap.newOrPut("backpack", () -> VariableHelper.createNullVariable(SBackPack.class));

        section.ifValuePresent("description", List.class, desc -> this.description = desc);

        RequirementMigrator.migrate(section);
        requirementHolder = RequirementController.initializeRequirementsSection(section.getSection("requirements").get(), variableMap);
    }
}
