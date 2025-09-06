# BlockCreator
A Minecraft plugin that compactly and efficiently stores the creator of each block in a chunk.

##  How It Works

Each chunk stores block ownership using two NBT byte arrays: `users` and `data`.

###  `users` Array
This maps byte values to UUIDs. It is always a multiple of 17 bytes long:
- **1 byte**: Key
- **16 bytes**: UUID

A key of `0x00` means the block has no owner.

###  `data` Array
This array holds the actual block ownership data, organized layer-by-layer. Only non-empty layers are stored.

#### Layers & Hexadecants

Each layer is divided into 16 parts, called **hexadecants**. This makes storage more efficient—if only one block is placed, we only need 15 bytes of overhead instead of 255.

---

##  Layer Header

Each layer begins with **2 bytes** (a short) describing it:

- **Bits 0–8** (9 bits): Y-coordinate of the layer (0–384).  
  (Minecraft chunk Y-levels go from -64 to 320, so offset is +64)
- **Bits 9–12** (4 bits): Number of hexadecants stored **minus one** (values 0–15 → 1–16 hexadecants)
- **Bits 13–15** (3 bits): Reserved for future use (e.g., versioning)

>  If a layer has 1 hexadecant, this value is `0x00`;  
> If 2 hexadecants: `0x01`, and so on.

---

##  Hexadecant Indexing

Next is a byte array telling the program **which hexadecants** are present.

Each hexadecant index is 4 bits long, so 2 hexadecants fit in 1 byte.  
The length of this array is `ceil(hexadecantCount / 2)`.

Example:
If there are **5 hexadecants** at indices `3, 4, 6, 7, 10`, the array looks like:

```
An extra 0 is appended since the count is odd
               \/
0x34 0x67 0xA0
```

---

##  Hexadecant Data

Each hexadecant contains exactly **16 bytes**, where each byte indicates the owner (by user ID) of one block.

So, total data size for one layer is:
```
hexadecantCount * 16 bytes
```

---

##  Full Example

Here’s what a `data` array might look like when there's one layer in the chunk with blocks placed by users:

```
Layer Header:
0x04 0x64 → binary 00000100 01100100

- Y-coordinate = 100 (→ actual Y = 34, since 100 - 64)
- Hexadecants stored = 2

Hexadecant Mapping:
0xAB
- First hexadecant is at index 10 (A)
- Second hexadecant is at index 11 (B)

Hexadecant Data (32 bytes total):

0x00 0x00 0x00 0x00 0x01 0x00 0x00 0x00 0x00 0x01 0x00 0x00 0x00 0x00 0x00 0x00  
0x00 0x00 0x00 0x00 0x00 0x02 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00

- First hexadecant: Two blocks placed by user with ID `1`
- Second hexadecant: One block placed by user with ID `2`
```

---

also minecraft compresses its chunks with the deflate algorithm, that's why this data isn't compressed. Double compression would be counter productive



## API
To get the API, use BlockCreator.getAPI(), the methods are: (they are documented with javadoc comments)
```hasOwner, setOwner, getOwner, removeOwner```
