package com.bgsoftware.superiorprison.plugin.tasks;

public class TasksStarter {

    public TasksStarter() {
        new MineResetTask();
        new RankupTask();
        new BoosterTask();
        new MessagesTask();
        new StatisticSaveTask();
        new SellMessageTask();
    }
}
