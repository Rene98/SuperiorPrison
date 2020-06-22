package com.bgsoftware.superiorprison.plugin.commands.prisoner.ranks;

import com.oop.orangeengine.command.OCommand;

public class CmdRanks extends OCommand {
    public CmdRanks() {
        label("ranks");
        description("Add, remove, clear, view ranks");
        permission("superiorprison.admin");
    }
}