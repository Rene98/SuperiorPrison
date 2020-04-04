package com.bgsoftware.superiorprison.plugin.menu.flags;

import com.bgsoftware.superiorprison.api.data.mine.flags.Flag;
import com.bgsoftware.superiorprison.plugin.object.mine.area.SArea;
import com.bgsoftware.superiorprison.plugin.object.player.SPrisoner;
import com.bgsoftware.superiorprison.plugin.util.menu.*;
import com.oop.orangeengine.main.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlagsEditMenu extends OPagedMenu<Flag> implements OMenu.Templateable {

    private SArea area;
    public FlagsEditMenu(SPrisoner viewer, SArea area) {
        super("areaFlags", viewer);
        this.area = area;

        ClickHandler
                .of("flag click")
                .handle(event -> {
                    Flag flag = requestObject(event.getRawSlot());
                    boolean current = area.getFlagState(flag);
                    handleToggleable(event, current, () -> area.setFlagState(flag, !current));
                    area.getMine().save(true);
                })
                .apply(this);
    }

    @Override
    public OMenu getMenu() {
        return this;
    }

    @Override
    public List<Flag> requestObjects() {
        return new ArrayList<>(area.getFlags().keySet());
    }

    @Override
    public OMenuButton toButton(Flag obj) {
        Optional<OMenuButton> flagTemplate = getTemplateButtonFromTemplate("flag template");
        if (!flagTemplate.isPresent()) return null;

        OMenuButton button = flagTemplate.get().clone();
        OMenuButton.ButtonItemBuilder toggleableState = getToggleableState(button, area.getFlagState(obj)).clone();
        toggleableState.itemBuilder().replaceDisplayName("{flag_name}", Helper.beautify(obj.name()));
        return button.currentItem(toggleableState.getItemStackWithPlaceholdersMulti(getViewer(), area));
    }

    private OMenuButton.ButtonItemBuilder getToggleableState(OMenuButton button, boolean state) {
        if (state)
            return button.getStateItem("enabled");

        else
            return button.getStateItem("disabled");
    }

    public void handleToggleable(ButtonClickEvent event, Boolean current, Runnable toggle) {
        toggle.run();
        event.getWhoClicked().sendMessage(Helper.color("You've " + (current ? "disabled" : "enabled") + " '" + event.getButton().action() + "'"));
        refresh();
    }

    @Override
    public Object[] getBuildPlaceholders() {
        return new Object[]{area, area.getMine()};
    }
}
