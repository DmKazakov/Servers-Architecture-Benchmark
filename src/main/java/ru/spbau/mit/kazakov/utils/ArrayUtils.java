package ru.spbau.mit.kazakov.utils;

import org.jetbrains.annotations.NotNull;

public class ArrayUtils {
    /**
     * Sorts specified array using bubble sort.
     */
    public static void sort(@NotNull int[] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 1; j < array.length - i; j++) {
                if (array[j - 1] > array[j]) {
                    int temp = array[j - 1];
                    array[j - 1] = array[j];
                    array[j] = temp;
                }
            }
        }
    }

    /**
     * Converts {@link ArrayOuterClass.Array} to int[].
     */
    @NotNull
    public static int[] toIntArray(@NotNull ArrayOuterClass.Array array) {
        int size = array.getSize();
        int[] intArray = new int[size];

        for (int i = 0; i < size; i++) {
            intArray[i] = array.getElement(i);
        }

        return intArray;
    }

    /**
     * Converts int[] to  {@link ArrayOuterClass.Array}.
     */
    @NotNull
    public static ArrayOuterClass.Array toProtoArray(@NotNull int[] intArray) {
        ArrayOuterClass.Array.Builder builder = ArrayOuterClass.Array.newBuilder();

        builder.setSize(intArray.length);
        for (int element : intArray) {
            builder.addElement(element);
        }

        return builder.build();
    }
}
