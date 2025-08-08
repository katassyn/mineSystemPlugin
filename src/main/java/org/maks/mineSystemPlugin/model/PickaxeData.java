package org.maks.mineSystemPlugin.model;

import java.util.UUID;

public record PickaxeData(UUID uuid, String material, int durability, String enchants) {}
