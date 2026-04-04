package com.github.epsilon.utils.math;

import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {

    private static int ensureMinMax(int min, int max) {
        if (min <= max) return min;
        return max;
    }

    private static int ensureMaxMin(int min, int max) {
        if (min <= max) return max;
        return min;
    }

    private static double ensureMinMax(double min, double max) {
        if (min <= max) return min;
        return max;
    }

    private static double ensureMaxMin(double min, double max) {
        if (min <= max) return max;
        return min;
    }

    private static float ensureMinMax(float min, float max) {
        if (min <= max) return min;
        return max;
    }

    private static float ensureMaxMin(float min, float max) {
        if (min <= max) return max;
        return min;
    }

    public static int getRandom(int min, int max) {
        if (min == max) return min;
        int actualMin = ensureMinMax(min, max);
        int actualMax = ensureMaxMin(min, max);
        return ThreadLocalRandom.current().nextInt(actualMin, actualMax);
    }

    public static double getRandom(double min, double max) {
        if (min == max) return min;
        double actualMin = ensureMinMax(min, max);
        double actualMax = ensureMaxMin(min, max);
        return ThreadLocalRandom.current().nextDouble(actualMin, actualMax);
    }

    public static float getRandom(float min, float max) {
        if (min == max) return min;
        float actualMin = ensureMinMax(min, max);
        float actualMax = ensureMaxMin(min, max);
        return (float) ThreadLocalRandom.current().nextDouble(actualMin, actualMax);
    }

    public static int getRandomLogNormal(int min, int max) {
        if (min == max) return min;
        double value = getRandomLogNormal((double) min, (double) max);
        return (int) Math.round(value);
    }

    public static double getRandomLogNormal(double min, double max) {
        if (min == max) return min;
        double actualMin = ensureMinMax(min, max);
        double actualMax = ensureMaxMin(min, max);

        double mean = (actualMin + actualMax) / 2.0;
        double sigma = 0.3;

        double mu = Math.log(mean) - sigma * sigma / 2.0;

        double u1 = ThreadLocalRandom.current().nextDouble();
        double u2 = ThreadLocalRandom.current().nextDouble();

        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        double value = Math.exp(mu + sigma * z);

        value = Math.max(actualMin, Math.min(actualMax, value));

        return value;
    }

    public static float getRandomLogNormal(float min, float max) {
        if (min == max) return min;
        double value = getRandomLogNormal((double) min, (double) max);
        return (float) value;
    }

    public static int getRandomLongTail(int min, int max) {
        return getRandomLogNormal(min, max);
    }

    public static double getRandomLongTail(double min, double max) {
        return getRandomLogNormal(min, max);
    }

    public static float getRandomLongTail(float min, float max) {
        return getRandomLogNormal(min, max);
    }

}
