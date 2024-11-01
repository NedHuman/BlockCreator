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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for block creator stuff
 *
 * @author NedHuman
 */
public class BlockCreatorService {

    private final NamespacedKey data;
    private final NamespacedKey users;


    private Map<Chunk, ChunkCache> chunkCache;
    private boolean dumpData; // If cached data was modified and needs to be updated in chunk NBT

    public BlockCreatorService(Plugin plugin)
    {
        data = new NamespacedKey(plugin, "data");
        users = new NamespacedKey(plugin, "users");
        dumpData = false;
        chunkCache = new HashMap<>();

        new BlockCreatorServiceListeners(this, plugin);
    }

    protected void fireChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if(pdc.has(data) && pdc.has(users)) {

            ChunkCache cache = new ChunkCache();

            // create a temporary user map to store byte-uuid
            Map<Byte, UUID> userMap = createUserMap(cache, chunk, pdc);

            loadChunkData(cache, pdc, userMap);
            
        } else {
            // Chunk has no previous data, create new chunk cache.
            chunkCache.put(chunk, new ChunkCache());
        }
    }

    protected void fireChunkUnload(Chunk chunk) {

    }


    private static class ChunkCache {

        private final Map<Short, Table<Integer, Integer, UUID>> layers;
        public boolean dump; // True if the data has been modified and needs to be dumped to chunk NBT storage.

        public ChunkCache() {
            layers = new HashMap<>();
            dump = false;
        }

    }

    private Map<Byte, UUID> createUserMap(ChunkCache cache, Chunk chunk, PersistentDataContainer pdc) {
        Map<Byte, UUID> userMap = new HashMap<>();

        byte[] usersData = pdc.get(users, PersistentDataType.BYTE_ARRAY);
        // 17 because 1 byte index and 16 byte UUID
        if((usersData.length % 17) != 0) {
            throw new IllegalStateException("Corrupt chunk found at "+chunk.getX()*16+" "+chunk.getZ()*16+"; invalid users array length");
        }

        int i = 0;
        while(i < usersData.length) {
            byte index = usersData[i++];

            byte[] uuidBytes = new byte[16];
            for(int l = 0; l < 16; l++) {
                uuidBytes[l] = usersData[i++];
            }

            UUID uuid = Utils.getUUID(uuidBytes);
            userMap.put(index, uuid);
        }

        return userMap;
    }

    private void loadChunkData(
            ChunkCache cache,
            PersistentDataContainer pdc,
            Map<Byte, UUID> userMap
    ) {
        byte[] layersData = pdc.get(data, PersistentDataType.BYTE_ARRAY);

        int i = 0;
        while(i < layersData.length) { // Reading each layer

            int header = ((layersData[i++] << 8) | layersData[i++]); // Read the first two bytes

            // The 9 rightmost bits indicate the Y layer. we mask em out
            short layer = (short) (header & (0x1ff));
            cache.layers.put(layer, HashBasedTable.create()); // initialise the table
            // The next 4 bits are the amount of hexadecants in the layer, minus one
            int hexadecantsNum = ((header >> 9) & 0xf)+1;

            // key is hexadecents by order, value is hexadecent position
            int[] hexadecentMap = new int[hexadecantsNum];
            int l = 0;
            while(l++ < hexadecantsNum) {
                if(l % 2 == 0) {
                    hexadecentMap[l-1] = layersData[i] >> 4;
                    i++;
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

                readHexadecant(data, hexadecentMap[n], cache.layers.get(layer), userMap);

            }


        }
    }

    private static void readHexadecant(
            byte[] data,
            int hexadecant,
            Table<Integer, Integer, UUID> layer,
            Map<Byte, UUID> userMap
    ) {

        int hexadecantX = (hexadecant >> 2) * 4;
        int hexadecantY = (hexadecant & 0x2) * 4;

        int l = 0;
        for(int p = 0; p < 4; p++) {
            for(int m = 0; m < 4; m++) {
                if(data[l] != 0) { // zero indicates no owner
                    byte id = data[l++];

                    if(!userMap.containsKey(id)) throw new IllegalStateException("Corrupt chunk found; invalid user id "+id);

                    layer.put(hexadecantX + p, hexadecantY + m, userMap.get(id));
                }
            }
        }
    }







    /*
    PUBLIC API METHODS
     */

    public boolean hasOwner(Location location) throws IllegalArgumentException
    {
        return false;
    }

    public void setOwner(Location location, UUID uuid) throws IllegalArgumentException
    {

    }

    public UUID getOwner(Location location)  throws IllegalArgumentException
    {
        return null;
    }

    public void removeOwner(Location location)  throws IllegalArgumentException
    {

    }
}
