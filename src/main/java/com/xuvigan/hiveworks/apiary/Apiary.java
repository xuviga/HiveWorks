package com.xuvigan.hiveworks.apiary;

import com.xuvigan.hiveworks.hive.HivePos;

import java.util.UUID;

/**
 * A player-owned apiary: a named center with 3+ hives nearby.
 */
public record Apiary(UUID id, UUID owner, String name, HivePos center, long createdAt) {
}
