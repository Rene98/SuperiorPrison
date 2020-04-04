package com.bgsoftware.superiorprison.plugin.commands.mines;

import com.bgsoftware.superiorprison.plugin.commands.args.MinesArg;
import com.bgsoftware.superiorprison.plugin.object.mine.SNormalMine;
import com.oop.orangeengine.command.OCommand;
import org.bukkit.entity.Player;

public class CmdEdit extends OCommand {

    public CmdEdit() {
        label("edit");
        argument(new MinesArg().setRequired(true));
        ableToExecute(Player.class);
        permission("superiorprison.admin");
        onCommand(command -> {
            Player player = (Player) command.getSender();
            SNormalMine mine = (SNormalMine) command.getArg("mine").get();
        });
    }

}
