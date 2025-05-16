package dev.nedhuman.blockcreator;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jdk.jshell.execution.Util;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.awt.desktop.SystemSleepEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

        debug("Preparing chunk load at "+chunk.getX()+" "+chunk.getZ());

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        ChunkCache cache = new ChunkCache();
        if(pdc.has(data) && pdc.has(users)) {
            debug("Chunk has previous data, begin load");

            // create a temporary user map to store byte-uuid
            Map<Byte, UUID> userMap = createUserMap(chunk, pdc);

            loadChunkData(cache, pdc, userMap);
        }
        chunkCache.put(chunk, cache);
    }

    protected void fireChunkUnload(Chunk chunk, boolean keep) {
        debug("Preparing chunk save "+chunk.getX()+" "+chunk.getZ());
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        if(chunkCache.containsKey(chunk)) {
            ChunkCache cache = chunkCache.get(chunk);
            Map<UUID, Byte> userMap = new HashMap<>();

            ByteArrayOutputStream deposit = new ByteArrayOutputStream();

            // cycle through each layer
            for(Map.Entry<Short, Map<Byte, UUID>> layer : cache.layers.entrySet()) {
                debug("Layer "+layer.getKey()+" exists");

                // the hexadecants which we will store
                Set<Utils.Hexadecant> hexadecants = new LinkedHashSet<>();

                // cycle through each of the hexadecants
                for(int n = 0; n < 16; n++) {
                    debug("Cycling at hexadecant "+n);

                    Utils.Hexadecant hexadecant = new Utils.Hexadecant(n);
                    hexadecant.setPlacement(n);

                    // cycle through the 16 blocks in the hexadecant
                    for(int i = 0; i < 16; i++) {
                        int chunkX = (i >> 2) + (n >> 2)*4;
                        int chunkZ = (i & 0x3) + (n & 0x03)*4;

                        byte chunkCoord = Utils.compressChunkCoords(chunkX, chunkZ);

                        if(layer.getValue().containsKey(chunkCoord)) { // contains this block
                            UUID uuid = layer.getValue().get(chunkCoord);
                            debug("Block "+chunkX+" "+chunkZ+" exists, mapped to uuid "+uuid.toString());
                            byte id;
                            if(userMap.containsKey(uuid)) {
                                debug("We have already stored this uuid");
                                id = userMap.get(uuid);
                            }else{
                                debug("New uuid, storing");
                                id = (byte) (userMap.size()+1);
                                userMap.put(uuid, id);
                            }

                            hexadecant.contents[i] = id;
                            debug("Set block "+i+" in the hexadecant to "+id);
                            hexadecants.add(hexadecant);
                        }
                    }
                }

                if(!hexadecants.isEmpty()) { // if theres nothing in this layer then we dont write the layer at all
                    // ok now we write to deposit
                    int header = 0;
                    header |= (layer.getKey() + 64) & 0x1ff;
                    header |= (hexadecants.size() - 1 & 0xf) << 9;
                    // write header now (2 bytes)
                    deposit.write(header >> 8);
                    deposit.write(header);
                    debug("Written header "+Integer.toHexString(header & 0xffff)+", hexadecant layer was "+layer.getKey()+" and size was "+hexadecants.size());

                    // ok now comes the thing that shows what each hexadecant's placement is
                    byte work = 0;
                    boolean halfFull = false;
                    for (Utils.Hexadecant h : hexadecants) {
                        if (!halfFull) {
                            work |= (byte) (h.getPlacement() << 4);
                        } else {
                            work = (byte) (h.getPlacement() & 0xf);
                            deposit.write(work);
                            work = 0;
                        }
                        halfFull = !halfFull;
                    }
                    if (halfFull) {
                        deposit.write(work);
                    }

                    // now we write the actual data
                    for(Utils.Hexadecant h : hexadecants) {
                        try {
                            deposit.write(h.contents);
                        }catch (IOException e) {}
                    }
                }
            }

            if(!keep) {
                chunkCache.remove(chunk);
            }

            byte[] depositArray = deposit.toByteArray();
            byte[] userMapArray = userMapToArray(userMap);

            if(depositArray.length != 0) {
                pdc.set(data, PersistentDataType.BYTE_ARRAY, depositArray);
            }else{
                pdc.remove(data);
            }

            if(userMapArray.length != 0) {
                pdc.set(users, PersistentDataType.BYTE_ARRAY, userMapArray);
            }else{
                pdc.remove(users);
            }

        }
    }


    /**
     * A class representing a single chunk
     */
    private static class ChunkCache {

        // the first short is Y coordinate, byte is x and z
        private final Map<Short, Map<Byte, UUID>> layers;
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
        if(usersData.length == 0 || (usersData.length % 17) != 0) {
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
            debug("Loaded UUID "+uuid.toString()+" to ID "+index);
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
        debug("Preparing to load chunk data: "+Utils.arrayToString(layersData));

        int i = 0;
        do { // Reading each layer
            ensureEnoughData(i, 2, layersData.length);
            int header = (((layersData[i++] & 0xff) << 8) | (layersData[i++] & 0xff)); // Read the first two bytes

            // The 9 rightmost bits indicate the Y layer, plus 64. we mask em out
            short layer = (short) ((header & (0x1ff))-64);
            // The next 4 bits are the amount of parts (1-16) in the layer, minus one
            int hexadecantsNum = ((header >> 9) & 0xf)+1;

            debug("First layer header is "+Integer.toHexString(header)+", it is layer "+layer+" and it contains "+hexadecantsNum+" hexadecants");

            ensureEnoughData(i, (int) Math.ceil(hexadecantsNum / 2.0), layersData.length);
            // key is hexadecants by order, value is hexadecent position
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

            debug("Loaded hexadecants map "+Arrays.toString(hexadecentMap));

            // Now for reading each hexadecant
            for(int n = 0; n < hexadecantsNum; n++) {
                ensureEnoughData(i, 16, layersData.length);
                byte[] data = new byte[16];
                for(int o = 0; o < 16; o++) {
                    data[o] = layersData[i++];
                }

                debug("Hexadecant "+n+" data is "+Arrays.toString(data));

                cache.layers.put(layer, new HashMap<>()); // need to create the layer first
                readHexadecant(data, hexadecentMap[n], cache.layers.get(layer), userMap);

            }

        } while (i < layersData.length);
    }

    /**
     * Convert the read hexadecent byte data to UUIDs based on the provided user map
     * @param data the 16 bytes of data from the hexadecant
     * @param hexadecant which of the 16 hexadecants is this
     * @param layer the deposit
     * @param userMap the user map to take info from
     */
    private static void readHexadecant(
            byte[] data,
            int hexadecant,
            Map<Byte, UUID> layer,
            Map<Byte, UUID> userMap
    ) {

        int hexadecantX = (hexadecant >> 2); // this hexadecant's position on the x axis
        int hexadecantZ = (hexadecant & 0x3); // on the z one

        int l = 0;
        for(int n = 0; n < 16; n++) { /// for each block
            byte id = data[l++];
            if(id != 0) { // zero indicates no owner
                int x = (n>>2) + hexadecantX*4; // actual coords (chunk)
                int z = (n & 0x3) + hexadecantZ*4;
                debug("Loaded user "+id+" to block "+x+" "+z+" in the layer");

                if(!userMap.containsKey(id)) throw new IllegalStateException("Corrupt chunk found; invalid user id "+id);

                layer.put(
                        Utils.compressChunkCoords(x, z), userMap.get(id));
            }
        }
    }

    private static byte[] userMapToArray(Map<UUID, Byte> userMap) {
        byte[] userArray = new byte[userMap.size()*17];

        int i = 0;
        for(Map.Entry<UUID, Byte> l : userMap.entrySet()) {
            userArray[i++] = l.getValue();
            UUID uuid = l.getKey();

            byte[] uuidBytes = new byte[16];
            ByteBuffer.wrap(uuidBytes)
                    .putLong(uuid.getMostSignificantBits())
                    .putLong(uuid.getLeastSignificantBits());

            System.arraycopy(uuidBytes, 0, userArray, i, 16);

            i += 16;
        }

        return userArray;
    }

    public void saveChunks() {
        for(Map.Entry<Chunk, ChunkCache> i : chunkCache.entrySet()) {
            if(i.getValue().dump) {
                fireChunkUnload(i.getKey(), true);
            }
        }
    }

    public NamespacedKey getDataKey() {
        return data;
    }

    public NamespacedKey getUsersKey() {
        return users;
    }

    private static void ensureEnoughData(int index, int howMuchIPlanToRead, int data)  throws IllegalStateException {
        int howMuchIsLeft = data-index;
        if (howMuchIPlanToRead > howMuchIsLeft)
            throw new IllegalStateException("Corrupt data, I require "+howMuchIPlanToRead+" more bytes, however only "+howMuchIsLeft+" "+
                    (howMuchIsLeft==1 ? "is" : "are") +" left");
    }

    private static void debug(String msg) {
        if(BlockCreator.getInstance().isDebug()) {
            BlockCreator.getInstance().getLogger().info("[DEBUG] "+msg);
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
        int chunkZ = location.getBlockZ() & 0xf;

        Map<Byte, UUID> layer = chunkCache.get(chunk).layers.get((short) location.getBlockY());
        if(layer == null) return false;
        return layer.containsKey(Utils.compressChunkCoords(chunkX, chunkZ));
    }

    public void setOwner(Location location, UUID uuid) throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {

            throw new IllegalStateException("Attempted to write data on an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkZ = location.getBlockZ() & 0xf;

        byte chunkCoord = Utils.compressChunkCoords(chunkX, chunkZ);
        Map<Byte, UUID> layer = chunkCache.get(chunk).layers.computeIfAbsent((short) location.getBlockY(), k -> new HashMap<>());

        if(!uuid.equals(layer.getOrDefault(chunkCoord, null))) {
            layer.put(chunkCoord, uuid);
            chunkCache.get(chunk).dump = true;
        }
    }

    public UUID getOwner(Location location)  throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {
            throw new IllegalStateException("Attempted to read data on an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkZ = location.getBlockZ() & 0xf;

        Map<Byte, UUID> layer = chunkCache.get(chunk).layers.get((short) location.getBlockY());
        if(layer == null) {
            return null;
        }
        return layer.get(Utils.compressChunkCoords(chunkX, chunkZ));
    }

    public void removeOwner(Location location)  throws IllegalStateException
    {
        Chunk chunk = location.getChunk();
        if(!chunkCache.containsKey(chunk)) {
            throw new IllegalStateException("Attempted to write data to an unloaded chunk");
        }

        int chunkX = location.getBlockX() & 0xf;
        int chunkZ = location.getBlockZ() & 0xf;

        byte chunkCoord = Utils.compressChunkCoords(chunkX, chunkZ);
        Map<Byte, UUID> layer = chunkCache.get(chunk).layers.get((short) location.getBlockY());
        if(layer != null && layer.containsKey(chunkCoord)) {
            layer.remove(chunkCoord);
            chunkCache.get(chunk).dump = true;
        }
    }
}
