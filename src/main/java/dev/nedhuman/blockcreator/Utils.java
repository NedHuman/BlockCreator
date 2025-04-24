package dev.nedhuman.blockcreator;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class Utils {

    private Utils() {}

    public static UUID getUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public static int compressChunkCoords(int x, int z, int y) {
        if(x > 15 || z > 15 || y > 319 || y < -64) {
            throw new IllegalArgumentException("Illegal arguments for chunk coords");
        }

        return x | (z << 4) | ( (y+64) << 8);
    }

    public static int[] decompressChunkCoords(int chunkCoords) {
        int x = chunkCoords & 0xF;
        int z = (chunkCoords >> 4) & 0xF;
        int y = (chunkCoords >> 8) & 0x1FF + 64;

        return new int[] {x, z, y};
    }

    public static class Hexadecant {
        private int placement;
        public UUID[] contents;

        public Hexadecant(int placement) {
            this.placement = placement;
            contents = new UUID[16];
        }

        public int getPlacement() {
            return placement;
        }


        public void setPlacement(int placement) {
            this.placement = placement;
        }
    }
}
