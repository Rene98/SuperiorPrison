package com.bgsoftware.superiorprison.plugin.object.mine.area;

import com.bgsoftware.superiorprison.api.data.mine.area.Area;
import com.bgsoftware.superiorprison.api.data.mine.area.AreaEnum;
import com.bgsoftware.superiorprison.api.data.mine.flags.Flag;
import com.bgsoftware.superiorprison.api.util.SPLocation;
import com.bgsoftware.superiorprison.plugin.object.mine.SNormalMine;
import com.bgsoftware.superiorprison.plugin.util.Attachable;
import com.bgsoftware.superiorprison.plugin.util.ClassDebugger;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.oop.orangeengine.main.gson.GsonUpdateable;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

public class SArea implements Area, Attachable<SNormalMine> {

    @Getter
    private transient SNormalMine mine;

    @SerializedName(value = "min_point")
    private SPLocation minPoint;

    @SerializedName(value = "high_point")
    private SPLocation highPoint;

    @SerializedName(value = "flags")
    @Getter
    private Map<Flag, Boolean> flags = Maps.newConcurrentMap();

    @SerializedName(value = "type")
    private AreaEnum type;

    public SArea(SPLocation pos1, SPLocation pos2, AreaEnum type) {
        if (pos1.y() > pos2.y()) {
            this.highPoint = pos1;
            this.minPoint = pos2;

        } else {
            this.highPoint = pos2;
            this.minPoint = pos1;
        }
        this.type = type;
    }

    @Override
    public SPLocation getMinPoint() {
        return minPoint;
    }

    @Override
    public SPLocation getHighPoint() {
        return highPoint;
    }

    @Override
    public World getWorld() {
        return minPoint.getWorld();
    }

    public boolean isInside(SPLocation location) {
        if (!getWorld().getName().contentEquals(location.getWorld().getName())) return false;

        int x1 = Math.min(getMinPoint().xBlock(), getHighPoint().xBlock());
        int z1 = Math.min(getMinPoint().zBlock(), getHighPoint().zBlock());
        int x2 = Math.max(getMinPoint().xBlock(), getHighPoint().xBlock());
        int z2 = Math.max(getMinPoint().zBlock(), getHighPoint().zBlock());
        return location.x() >= x1 && location.x() <= x2 && location.z() >= z1 && location.z() <= z2;
    }

    public boolean isInside(Location location) {
        return isInside(new SPLocation(location));
    }

    @Override
    public boolean getFlagState(Flag flag) {
        return flags.get(flag);
    }

    @Override
    public void setFlagState(Flag flag, boolean state) {
        flags.remove(flag);
        flags.put(flag, state);
    }

    @Override
    public AreaEnum getType() {
        return type;
    }

    @Override
    public void attach(SNormalMine obj) {
        ClassDebugger.debug("Attached Mine");
        this.mine = obj;

        for (Flag flag : Flag.values())
            if (!flags.containsKey(flag))
                flags.put(flag, flag.getDefaultValue());
    }
}
