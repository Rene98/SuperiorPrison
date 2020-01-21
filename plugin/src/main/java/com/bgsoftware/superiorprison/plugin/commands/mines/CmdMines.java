package com.bgsoftware.superiorprison.plugin.commands.mines;

import com.bgsoftware.superiorprison.plugin.SuperiorPrisonPlugin;
import com.bgsoftware.superiorprison.plugin.menu.MinesListMenu;
import com.oop.orangeengine.command.OCommand;
import org.bukkit.entity.Player;

public class CmdMines extends OCommand {

    public CmdMines() {
        label("mines");
        ableToExecute(Player.class);
        onCommand(command -> {
            MinesListMenu minesListMenu = new MinesListMenu(SuperiorPrisonPlugin.getInstance().getDataController().insertIfAbsent(command.getSenderAsPlayer()));
            command.getSenderAsPlayer().openInventory(minesListMenu.getInventory());
        });
    }
}
