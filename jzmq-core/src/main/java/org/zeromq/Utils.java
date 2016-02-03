package org.zeromq;

public class Utils
{
    private Utils()
    {
    }

    public static void checkNotNull(Object obj)
    {
        if (obj == null) {
            throw new IllegalArgumentException("Argument must not be null");
        }
    }
}
