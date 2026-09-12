package ru.fsp.balloon.platform.round;

public record RoundResult(
        Long roundId,
        RoundState state,
        double multiplier,
        long payout,
        long pointsEarned,
        boolean boosterHit,
        String reward,
        String message
) {
}
