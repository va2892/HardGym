package com.hardgym.player;

public interface GymPlayerData {
    int hardgym$getStrengthXp();
    void hardgym$setStrengthXp(int value);

    int hardgym$getStrengthIdleSeconds();
    void hardgym$setStrengthIdleSeconds(int value);

    int hardgym$getBicepsXp();
    void hardgym$setBicepsXp(int value);

    int hardgym$getChestXp();
    void hardgym$setChestXp(int value);

    int hardgym$getTricepsXp();
    void hardgym$setTricepsXp(int value);

    int hardgym$getLegsXp();
    void hardgym$setLegsXp(int value);


    int hardgym$getBenchXp();
    void hardgym$setBenchXp(int value);

    int hardgym$getSquatXp();
    void hardgym$setSquatXp(int value);

    int hardgym$getDeadliftXp();
    void hardgym$setDeadliftXp(int value);

    int hardgym$getStamina();
    void hardgym$setStamina(int value);

    int hardgym$getFatigue();
    void hardgym$setFatigue(int value);

    int hardgym$getMissedSleepNights();
    void hardgym$setMissedSleepNights(int value);

    long hardgym$getLastSleepCheckDay();
    void hardgym$setLastSleepCheckDay(long value);

    int hardgym$getTotalReps();
    void hardgym$setTotalReps(int value);

    int hardgym$getBenchPressReps();
    void hardgym$setBenchPressReps(int value);

    int hardgym$getBenchPressPr();
    void hardgym$setBenchPressPr(int value);

    int hardgym$getSquatReps();
    void hardgym$setSquatReps(int value);

    int hardgym$getSquatPr();
    void hardgym$setSquatPr(int value);

    int hardgym$getDeadliftReps();
    void hardgym$setDeadliftReps(int value);

    int hardgym$getDeadliftPr();
    void hardgym$setDeadliftPr(int value);

    double hardgym$getBenchEstimated1Rm();
    void hardgym$setBenchEstimated1Rm(double value);

    double hardgym$getSquatEstimated1Rm();
    void hardgym$setSquatEstimated1Rm(double value);

    double hardgym$getDeadliftEstimated1Rm();
    void hardgym$setDeadliftEstimated1Rm(double value);
}
