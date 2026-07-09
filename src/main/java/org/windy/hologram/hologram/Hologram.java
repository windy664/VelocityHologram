package org.windy.hologram.hologram;

import org.windy.hologram.api.IHologram;
import org.windy.hologram.api.IHologramLine;
import org.windy.hologram.display.TextDisplayFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 悬浮字核心实现。
 * <p>管理一组 {@link HologramLine}，维护观察者列表，通过 packetevents 发包。
 */
public class Hologram implements IHologram {

    private final UUID id;
    private final String name;
    private final HologramPos position;
    private final List<HologramLine> lines = new ArrayList<>();
    private final Set<UUID> observers = ConcurrentHashMap.newKeySet();

    public Hologram(String name, HologramPos position) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.position = position;
    }

    @Override public UUID getId() { return id; }
    @Override public String getName() { return name; }
    @Override public HologramPos getPosition() { return position; }

    @Override
    public List<IHologramLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public void addLine(String text) {
        lines.add(new HologramLine(lines.size(), text));
    }

    @Override
    public void setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.get(index).setText(text);
        }
    }

    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            HologramLine removed = lines.remove(index);
            // 先对所有观察者销毁该行
            for (UUID playerId : observers) {
                TextDisplayFactory.despawn(playerId, removed.getEntityId());
            }
            // 重建索引
            for (int i = 0; i < lines.size(); i++) {
                lines.get(i).setIndex(i);
            }
        }
    }

    @Override
    public void showTo(UUID playerId) {
        if (observers.add(playerId)) {
            for (HologramLine line : lines) {
                double y = line.getWorldY(position.y());
                TextDisplayFactory.spawn(playerId, line.getEntityId(),
                        position.x(), y, position.z(), line.getText());
            }
        }
    }

    @Override
    public void hideFrom(UUID playerId) {
        if (observers.remove(playerId)) {
            for (HologramLine line : lines) {
                TextDisplayFactory.despawn(playerId, line.getEntityId());
            }
        }
    }

    @Override
    public void update() {
        for (UUID playerId : observers) {
            for (HologramLine line : lines) {
                TextDisplayFactory.updateText(playerId, line.getEntityId(), line.getText());
            }
        }
    }

    @Override
    public void destroy() {
        for (UUID playerId : observers) {
            for (HologramLine line : lines) {
                TextDisplayFactory.despawn(playerId, line.getEntityId());
            }
        }
        observers.clear();
    }

    public Set<UUID> getObservers() { return observers; }
    public boolean isObserver(UUID playerId) { return observers.contains(playerId); }
}
