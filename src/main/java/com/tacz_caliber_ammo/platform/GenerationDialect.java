package com.tacz_caliber_ammo.platform;

import com.google.gson.JsonObject;

public final class GenerationDialect {

    public static String recipeDirectory() {
        //? if forge {
        return "recipes";
        //?} else {
        /*return "recipe";
        *///?}
    }

    public static String copperIngotTag() {
        //? if forge {
        return "forge:ingots/copper";
        //?} else {
        /*return "c:ingots/copper";
        *///?}
    }

    public static String ironIngotTag() {
        //? if forge {
        return "forge:ingots/iron";
        //?} else {
        /*return "c:ingots/iron";
        *///?}
    }

    public static String gunpowderTag() {
        //? if forge {
        return "forge:gunpowder";
        //?} else {
        /*return "c:gunpowders";
        *///?}
    }

    public static String ironNuggetTag() {
        //? if forge {
        return "forge:nuggets/iron";
        //?} else {
        /*return "c:nuggets/iron";
        *///?}
    }

    public static JsonObject ammoIcon(String ammoId) {
        JsonObject icon = new JsonObject();
        //? if forge {
        icon.addProperty("item", "tacz:ammo");
        JsonObject nbt = new JsonObject();
        nbt.addProperty("AmmoId", ammoId);
        icon.add("nbt", nbt);
        //?} else {
        /*icon.addProperty("id", "tacz:ammo");
        JsonObject components = new JsonObject();
        components.addProperty("minecraft:custom_data", "{AmmoId: \"" + ammoId + "\"}");
        icon.add("components", components);
        *///?}
        return icon;
    }

    public static JsonObject workbenchResult(String blockId) {
        JsonObject result = new JsonObject();
        //? if forge {
        result.addProperty("item", "tacz:workbench_a");
        JsonObject nbt = new JsonObject();
        nbt.addProperty("BlockId", blockId);
        result.add("nbt", nbt);
        //?} else {
        /*result.addProperty("id", "tacz:workbench_a");
        JsonObject components = new JsonObject();
        components.addProperty("minecraft:custom_data", "{BlockId: \"" + blockId + "\"}");
        result.add("components", components);
        *///?}
        return result;
    }

    private GenerationDialect() {
    }
}