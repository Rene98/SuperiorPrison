package com.bgsoftware.superiorprison.plugin.util.menu;

import com.google.common.collect.Sets;
import com.oop.orangeengine.menu.events.ButtonEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Accessors(fluent = true, chain = true)
public class ClickHandler {

    private String action;
    private Set<ClickType> acceptsTypes = Sets.newHashSet(ClickType.values());

    private Consumer<ButtonClickEvent> consumer;

    private ClickHandler() {}

    public static ClickHandler of(OMenuButton button) {
        ClickHandler clickHandler = new ClickHandler();
        clickHandler.action = button.action();

        return clickHandler;
    }

    public static ClickHandler of(String action) {
        ClickHandler clickHandler = new ClickHandler();
        clickHandler.action = action;

        return clickHandler;
    }

    public void apply(OMenu menu) {
        menu.getClickHandlers().put(Objects.requireNonNull(action.toLowerCase(), "Cannot put action as null"), this);
    }

    public ClickHandler clearClickTypes() {
        acceptsTypes.clear();
        return this;
    }

    public ClickHandler acceptsClickType(ClickType ...type) {
        acceptsTypes.addAll(Arrays.asList(type));
        return this;
    }

    public boolean doesAcceptEvent(ButtonClickEvent event) {
        if (!acceptsTypes.contains(event.getClick()))
            return false;
        System.out.println("Accepts types");

        System.out.println("Button action: " + event.getButton().action());
        System.out.println("ItemStack: " + event.getButton().currentItem());
        if (action != null && !event.getButton().action().equalsIgnoreCase(action))
            return false;

        System.out.println("Accepts action");
        return true;
    }

    public ClickHandler handle(Consumer<ButtonClickEvent> consumer) {
        this.consumer = consumer;
        return this;
    }

    public void handle(ButtonClickEvent event) {
        if (consumer != null)
            consumer.accept(event);
    }
}