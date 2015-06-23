package net.dongliu.vcdiff.utils;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Utils for direct byte buffer
 */
public class ByteBufferUtils {

    /**
     * Try deallocate direct byte buffer, do nothing if buffer is not direct
     */
    public static boolean free(ByteBuffer byteBuffer) {
        if (byteBuffer == null || !byteBuffer.isDirect()) {
            return false;
        }

        //call ((DirectBuffer)byteBuffer).cleaner().clean() by reflection
        try {
            Method getCleanerMethod = byteBuffer.getClass().getDeclaredMethod("cleaner");
            getCleanerMethod.setAccessible(true);
            Object cleaner = getCleanerMethod.invoke(byteBuffer);
            if (cleaner != null) {
                Method cleanMethod = cleaner.getClass().getDeclaredMethod("clean");
                cleanMethod.setAccessible(true);
                cleanMethod.invoke(cleaner);
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }
}
