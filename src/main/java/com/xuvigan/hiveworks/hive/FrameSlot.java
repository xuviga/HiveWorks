package com.xuvigan.hiveworks.hive;

/**
 * A frame sitting in a hive slot.
 */
public record FrameSlot(FrameType type, int usesLeft) {

    public FrameSlot wear() {
        int remaining = usesLeft - 1;
        return remaining <= 0 ? null : new FrameSlot(type, remaining);
    }
}
