package org.windy.hologram.display;

public enum DisplayEntityType {

    TEXT_DISPLAY,
    ITEM_DISPLAY,
    BLOCK_DISPLAY,
    ENTITY,
    HEAD,
    SMALLHEAD;

    public static DisplayEntityType fromConfig(String type) {
        if (type == null) return TEXT_DISPLAY;
        switch (type.toLowerCase()) {
            case "item": return ITEM_DISPLAY;
            case "block": return BLOCK_DISPLAY;
            case "entity": return ENTITY;
            case "head": return HEAD;
            case "smallhead": return SMALLHEAD;
            default: return TEXT_DISPLAY;
        }
    }

    public String toConfig() {
        switch (this) {
            case TEXT_DISPLAY: return "text";
            case ITEM_DISPLAY: return "item";
            case BLOCK_DISPLAY: return "block";
            case ENTITY: return "entity";
            case HEAD: return "head";
            case SMALLHEAD: return "smallhead";
            default: return "text";
        }
    }
}
