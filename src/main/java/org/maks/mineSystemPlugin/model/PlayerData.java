package org.maks.mineSystemPlugin.model;

import java.util.UUID;

public record PlayerData(UUID uuid, int stamina, long resetTimestamp) {}
