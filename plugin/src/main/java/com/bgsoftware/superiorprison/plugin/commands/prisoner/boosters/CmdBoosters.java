package com.bgsoftware.superiorprison.plugin.commands.prisoner.boosters;

import com.oop.orangeengine.command.OCommand;

public class CmdBoosters extends OCommand {
    public CmdBoosters() {
        label("boosters");
        description("add, remove boosters");
        permission("superiorprison.admin");

        subCommand(new CmdAdd());
        subCommand(new CmdClear());
        subCommand(new CmdRemove());
        subCommand(new CmdList());
    }
}
