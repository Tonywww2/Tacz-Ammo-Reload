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

    private GenerationDialect() {
    }
}