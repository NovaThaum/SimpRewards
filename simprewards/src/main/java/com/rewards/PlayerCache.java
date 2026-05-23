package com.example.rewards;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerCache {
    
    private final PlayerRewardsData data;
    private final AtomicBoolean dirty;
    
    public PlayerCache(PlayerRewardsData data) {
        this.data = data;
        this.dirty = new AtomicBoolean(false);
    }
    
    public PlayerRewardsData getData() {
        return data;
    }
    
    public boolean isDirty() {
        return dirty.get();
    }
    
    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }
    
    public void markDirty() {
        dirty.set(true);
    }
}
