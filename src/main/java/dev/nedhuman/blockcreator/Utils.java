package dev.nedhuman.blockcreator;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class Utils {

    private Utils() {}

    public static UUID getUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
