package org.simpmc.simpRewards.data;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRewardRepository {
    PlayerRewardState load(UUID uuid, String name);

    Optional<PlayerRewardState> findLoaded(UUID uuid);

    Collection<PlayerRewardState> loadedStates();

    void save(PlayerRewardState state);

    void saveAll();

    void unload(UUID uuid);
}
