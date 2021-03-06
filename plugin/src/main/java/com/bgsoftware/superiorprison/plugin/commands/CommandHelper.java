package com.bgsoftware.superiorprison.plugin.commands;

import com.bgsoftware.superiorprison.plugin.SuperiorPrisonPlugin;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.oop.orangeengine.command.WrappedCommand;
import com.oop.orangeengine.main.util.data.pair.OPair;
import com.oop.orangeengine.message.OMessage;
import com.oop.orangeengine.message.Replaceable;
import com.oop.orangeengine.message.Sendable;
import com.oop.orangeengine.message.impl.OChatMessage;
import com.oop.orangeengine.message.impl.chat.ChatLine;
import com.oop.orangeengine.message.impl.chat.LineContent;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerEvent;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CommandHelper {
    public static void sendMessage(WrappedCommand command, Sendable message, Object... objects) {
        message = message instanceof OMessage ? ((OMessage) message).clone() : message instanceof ChatLine ? ((ChatLine) message).clone() : message;

        if (message instanceof Replaceable) {
            for (Object object : objects)
                ((Replaceable) message).replace(object, SuperiorPrisonPlugin.getInstance().getPlaceholderController().findPlaceholdersFor(object));
        }

        message.send(command.getSender());
    }

    public static void send(CommandSender sender, Sendable sendable) {
        sendable.send(sender);
    }

    private static void sendMessage(CommandSender sender, Sendable sendable) {
        sendable.send(sender);
    }

    public static MessageBuilder messageBuilder(Sendable message, boolean clone) {
        return new MessageBuilder(clone ? message instanceof OMessage ? ((OMessage) message).clone() : message instanceof ChatLine ? ((ChatLine) message).clone() : message : message);
    }

    public static MessageBuilder messageBuilder(Sendable message) {
        return messageBuilder(message, true);
    }

    public static <T> ListedBuilder<T> listedBuilder(Class<T> clazz) {
        return new ListedBuilder<>();
    }

    public static Map<String, Object> mapOfArray(Object... array) {
        if (array.length % 2 != 0)
            throw new IllegalStateException("Failed to convert array to map, because the size is not even!");

        Map<String, Object> map = new HashMap<>();

        int len = array.length;
        int i = 0;

        boolean inside = true;
        while (inside) {
            Object key = array[i++];
            Object value = array[i++];

            map.put(
                    key == null ? "null" : key.toString(),
                    value
            );
            if (i == len)
                inside = false;
        }

        return map;
    }

    public static class MessageBuilder {
        private final Sendable sendable;

        private MessageBuilder(Sendable sendable) {
            this.sendable = sendable;
        }

        public MessageBuilder replace(Object... objects) {
            if (sendable instanceof Replaceable) {
                for (Object object : objects)
                    ((Replaceable) sendable).replace(object, SuperiorPrisonPlugin.getInstance().getPlaceholderController().findPlaceholdersFor(object));
            } else
                replace(mapOfArray(objects));
            return this;
        }

        public MessageBuilder replace(String key, Object value) {
            return replace(mapOfArray(key, value));
        }

        public MessageBuilder replace(Map<String, Object> placeholders) {
            if (sendable instanceof Replaceable)
                ((Replaceable) sendable).replace(placeholders);
            return this;
        }

        public <T extends Sendable> T getAs() {
            return (T) sendable;
        }

        public void send(WrappedCommand command) {
            sendMessage(command.getSender(), sendable);
        }

        public void send(CommandSender sender) {
            sendMessage(sender, sendable);
        }

        public void send(PlayerEvent playerEvent) {
            send(playerEvent.getPlayer());
        }
    }

    @Accessors(fluent = true, chain = true)
    public static class ListedBuilder<T> {

        private final Map<Class, Set<OPair<String, Function<Object, String>>>> placeholders = Maps.newHashMap();

        private @NonNull OMessage message;

        @Setter
        private @NonNull String identifier;

        @Setter
        private Set<T> objects = Sets.newHashSet();

        @Setter
        private Set<Object> placeholderObjects = Sets.newHashSet();

        @Setter
        private BiConsumer<LineContent, T> objectContentModifier;

        public ListedBuilder<T> addObject(T... objects) {
            this.objects.addAll(Arrays.asList(objects));
            return this;
        }

        public ListedBuilder<T> addPlaceholderObject(Object... objects) {
            this.placeholderObjects.addAll(Arrays.asList(objects));
            return this;
        }

        public <E> ListedBuilder<T> addPlaceholder(E object, String placeholder, Function<E, String> function) {
            Set<OPair<String, Function<Object, String>>> pairs = placeholders.computeIfAbsent(object.getClass(), clazz -> new HashSet<>());
            pairs.add(new OPair<>(placeholder, (Function<Object, String>) function));
            return this;
        }

        public ListedBuilder<T> message(OMessage message) {
            this.message = message.clone();
            return this;
        }

        private Set<OPair<String, Function<Object, String>>> findFor(Class clazz, Map<Class, Set<OPair<String, Function<Object, String>>>> allPlaceholders) {
            Optional<Class> first = allPlaceholders.keySet().stream().filter(clazz2 -> clazz == clazz2 || clazz.isAssignableFrom(clazz2) || clazz2.isAssignableFrom(clazz)).findFirst();
            if (!first.isPresent())
                return new HashSet<>();

            return allPlaceholders.getOrDefault(first.get(), new HashSet<>());
        }

        private List<Class> findAllParents(Class clazz) {
            List<Class> classes = new ArrayList<>();
            classes.add(clazz);

            if (clazz.getSuperclass() == null || clazz.getSuperclass() == Object.class)
                return classes;

            clazz = clazz.getSuperclass();
            while (clazz != Object.class) {
                classes.add(clazz);
                clazz = clazz.getSuperclass();
            }

            return classes;
        }

        private Class findParent(Class clazz) {
            Class found = null;

            Class temp = clazz;
            while (found == null) {
                Class newTemp = temp.getSuperclass();
                if (newTemp == null)
                    return temp;

                if (newTemp.isInterface() || newTemp == Object.class)
                    return temp;

                else
                    temp = newTemp;
            }
            return temp;
        }

        public OMessage build() {
            Map<Class, Set<OPair<String, Function<Object, String>>>> allPlaceholders = Maps.newHashMap();
            // Initialize placeholders for objects
            Set<Object> allObjects = new HashSet<>(placeholderObjects);

            if (!objects.isEmpty()) allObjects.add(objects.toArray()[0]);
            for (Object placeholderObject : allObjects) {
                Set<OPair<String, Function<Object, String>>> set = Sets.newHashSet();
                set.addAll(placeholders.getOrDefault(placeholderObject.getClass(), new HashSet<>()));
                set.addAll(SuperiorPrisonPlugin.getInstance().getPlaceholderController().findPlaceholdersFor(placeholderObject));
                allPlaceholders.put(findParent(placeholderObject.getClass()), set);
            }

            if (!(message instanceof OChatMessage)) return message;

            // Handle message stuff
            OPair<ChatLine, LineContent> line1 = ((OChatMessage) message).findContent(content -> content.text().contains(identifier));
            if (line1.getFirst() == null) return message;

            ChatLine messageLine = line1.getFirst().clone();
            messageLine.removeContentIf(lineContent -> lineContent.text().contentEquals(line1.getSecond().text()));

            // Replace placeholders for placeholder objects
            for (Object placeholderObject : placeholderObjects)
                messageLine.replace(placeholderObject, findFor(placeholderObject.getClass(), allPlaceholders));

            LineContent lineContent = line1.getSecond().clone();
            lineContent.replace(identifier, "");

            if (!objects.isEmpty()) {
                int count = 0;
                for (T object : objects) {
                    LineContent objectContent = lineContent.clone();
                    objectContent.replace(object, findFor(object.getClass(), allPlaceholders));
                    if (objectContentModifier != null)
                        objectContentModifier.accept(objectContent, object);

                    messageLine.append(objectContent);

                    count++;
                    if (count != objects.size())
                        messageLine.append(", ");
                }
            } else messageLine.append("None");

            return new OChatMessage(messageLine);
        }

        public void send(WrappedCommand command) {
            sendMessage(command.getSender(), build());
        }

        public void send(CommandSender sender) {
            sendMessage(sender, build());
        }
    }
}
