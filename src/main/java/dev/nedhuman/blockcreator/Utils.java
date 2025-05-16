package dev.nedhuman.blockcreator;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class Utils {

    private Utils() {}

    public static UUID getUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public static byte compressChunkCoords(int x, int z) {
        x &= 0xf;
        z &= 0xf;
        return (byte) (x | (z << 4));
    }

    public static int[] decompressChunkCoords(byte chunkCoords) {
        int x = chunkCoords & 0xF;
        int z = (chunkCoords >> 4) & 0xF;

        return new int[] {x, z};
    }

    public static String arrayToString(byte[] arr) {
        StringBuilder builder = new StringBuilder();
        for(byte i : arr) {
            builder.append("0x").append(Integer.toHexString(i & 0xff)).append(" ");
        }
        return builder.toString();
    }

    public static class Hexadecant {
        private int placement;
        public byte[] contents;

        public Hexadecant(int placement) {
            this.placement = placement;
            contents = new byte[16];
        }

        public int getPlacement() {
            return placement;
        }


        public void setPlacement(int placement) {
            this.placement = placement;
        }
    }
}
