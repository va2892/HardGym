package com.hardgym.player;

/** Temporary, server-approved curl weight; 0 means no accepted approach. */
public interface DumbbellPlayerState {
    int hardgym$getDumbbellTrainingWeight();

    void hardgym$setDumbbellTrainingWeight(int weightKg);
}
