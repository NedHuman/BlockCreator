package dev.nedhuman.blockcreator;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Service class for block creator stuff
 *
 * @author NedHuman
 */
public class BlockCreatorService {

    private final NamespacedKey data;
    private final NamespacedKey users;


    private Map<Chunk, ChunkCache> chunkCache;

    public BlockCreatorService(Plugin plugin)
    {
        data = new NamespacedKey(plugin, "data");
        users = new NamespacedKey(plugin, "users");
        chunkCache = new HashMap<>();

        new BlockCreatorServiceListeners(this, plugin);
    }

    protected void fireChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if(pdc.has(data) && pdc.has(users)) {

            ChunkCache cache = new ChunkCache();

            // create a temporary user map to store byte-uuid
            Map<Byte, UUID> userMap = createUserMap(chunk, pdc);

            loadChunkData(cache, pdc, userMap);
            
        } else {
            // Chunk has no previous data, create new chunk cache.
            chunkCache.put(chunk, new ChunkCache());
        }
    }

    protected void fireChunkUnload(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        if(chunkCache.containsKey(chunk)) {
            ChunkCache cache = chunkCache.get(chunk);
            Map<UUID, Byte> userMap = new HashMap<>();

            ByteArrayOutputStream deposit = new ByteArrayOutputStream();

            // todo: nvm this wont work cus i changed it
            for(
                    Map.Entry<Integer,UUID> l
                    :
                    cache.layers.entrySet()
            ) {
                int layerY = l.getKey();

                List<Utils.Hexadecant> hexadecants = new ArrayList<>();

                // cycle through each of the hexadecants
                for(int n = 0; n < 16; n++) {

                    Utils.Hexadecant hexadecant = new Utils.Hexadecant(n);
                    boolean proceed = false;

                    // cycle through the 16 blocks in the hexadecant
                    for(int i = 0; i < 16; i++) {
                        int chunkX = (i >> 2) + (n >> n)*4;
                        int chunkY = (i & 0x3) + (n & 0x03)*4;


                    }
                }

            }
        }
    }


    private static class ChunkCache {

        // the 32 bit integer is the chunk coordinate
        private final Map<Integer, UUID> layers;
        public boolean dump; // True if the data has been modified and needs to be dumped to chunk NBT storage.

        public ChunkCache() {
            layers = new HashMap<>();
            dump = false;
        }

    }

    /**
     * Takes the users byte array from the provided PDC and loads it into a byte-uuid map
     * @param chunk the chunk
     * @param pdc the PDC
     * @return a byte-uuid map
     */
    private Map<Byte, UUID> createUserMap(Chunk chunk, PersistentDataContainer pdc) {
        Map<Byte, UUID> userMap = new HashMap<>();

        byte[] usersData = pdc.get(users, PersistentDataType.BYTE_ARRAY);
        // 17 because 1 byte index and 16 byte UUID
        if((usersData.length % 17) != 0) {
            throw new IllegalStateException("Corrupt chunk found at "+chunk.getX()*16+" "+chunk.getZ()*16+"; invalid users array length");
        }

        int i = 0;
        do {
            byte index = usersData[i++];

            byte[] uuidBytes = new byte[16];
            for(int l = 0; l < 16; l++) {
                uuidBytes[l] = usersData[i++];
            }

            UUID uuid = Utils.getUUID(uuidBytes);
            userMap.put(index, uuid);
        } while(i < usersData.length);

        return userMap;
    }

    /**
     * Load the chunk's data into memory
     * @param cache
     * @param pdc
     * @param userMap
     */
    private void loadChunkData(
            ChunkCache cache,
            PersistentDataContainer pdc,
            Map<Byte, UUID> userMap
    ) {
        byte[] layersData = pdc.get(data, PersistentDataType.BYTE_ARRAY);

        int i = 0;
        do { // Reading each layer

            int header = ((layersData[i++] << 8) | layersData[i++]); // Read the first two bytes

            // The 9 rightmost bits indicate the Y layer, plus 64. we mask em out
            int layer = (header & (0x1ff))-64;
            // The next 4 bits are the amount of parts (1-16) in the layer, minus one
            int hexadecantsNum = ((header >> 9) & 0xf)+1;

            // key is hexadecents by order, value is hexadecent position
            int[] hexadecentMap = new int[hexadecantsNum];
            int l = 0;
            while(l++ < hexadecantsNum) {
                if(l % 2 != 0) {
                    hexadecentMap[l-1] = layersData[i++] >> 4;
                }else{
                    hexadecentMap[l-1] = layersData[i] & 0xf;
                    if(l == hexadecantsNum) i++;
                }
            }

            // Now for reading each hexadecant
            for(int n = 0; n < hexadecantsNum; n++) {
                byte[] data = new byte[16];
                for(int o = 0; o < 16; o++) {
                    data[o] = layersData[i++];
                }

                readHexadecant(data, hexadecentMap[n], cache.layers, userMap, layer);

            }

        } while (i < layersData.length);
    }

    /**
     * Convert the read hexadecent byte data to UUIDs based on the provided user map
     * @param data the 16 bytes of data from the hexadecant
     * @param hexadecant which of the 16 hexadecants is this
     * @param layers the deposit
     * @param userMap the user map to take info from
     * @param y the y coordinate
     */
    private static void readHexadecant(
            byte[] data,
            int hexadecant,
            Map<Integer, UUID> layers,
            Map<Byte, UUID> userMap,
            int y
    ) {

        int hexadecantX = (hexadecant >> 2) * 4; // this hexadecant's position on the x axis
        int hexadecantZ = (hexadecant & 0x2) * 4; // on the z one

        int l = 0;
        for(int p = 0; p < 4; p++) {
            for(int m = 0; m < 4; m++) {
                byte id = data[l++];
                if(id != 0) { // zero indicates no owner

                    if(!userMap.containsKey(id)) throw new IllegalStateException("Corrupt chunk found; invalid user id "+id);

                    layers.put(
                            Utils.compressChunkCoords(hexadecantX + p, hexadecantZ + m, y),
                            userMap.get(id));
                }
            }
        }
    }







    /*
    PUBLIC API METHODS
     */

    public boolean hasOwner(Location location) throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {
            throw new IllegalStateException("Attempted to read data on an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkY = location.getBlockY() & 0xf;

        return chunkCache.get(chunk).layers.get(location.getBlockY()).contains(chunkX, chunkY);
    }

    public void setOwner(Location location, UUID uuid) throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {
            throw new IllegalStateException("Attempted to write data on an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkY = location.getBlockY() & 0xf;

        chunkCache.get(chunk).layers.get(location.getBlockY()).put(chunkX, chunkY, uuid);
        chunkCache.get(chunk).dump = true;
    }

    public UUID getOwner(Location location)  throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {
            throw new IllegalStateException("Attempted to read data on an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkY = location.getBlockY() & 0xf;

        return chunkCache.get(chunk).layers.get(location.getBlockY()).get(chunkX, chunkY);
    }

    public void removeOwner(Location location)  throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {
            throw new IllegalStateException("Attempted to write data to an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkY = location.getBlockY() & 0xf;

        chunkCache.get(chunk).layers.get(location.getBlockY()).remove(chunkX, chunkY);
        chunkCache.get(chunk).dump = true;
    }
}
