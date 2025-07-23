package com.sectrend.buildscan.buildTools.scanner;


import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.util.zip.Checksum;


public class MyCrc32c implements Checksum {

    private static final int CRC32C_POLY = 517762881;
    private static final int REVERSED_CRC32C_POLY = Integer.reverse(517762881);
    private static final Unsafe UNSAFE;
    static {
        Field f = null;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int[] byteTable;
    private static final int[][] byteTables = new int[8][256];
    private static final int[] byteTable0;
    private static final int[] byteTable1;
    private static final int[] byteTable2;
    private static final int[] byteTable3;
    private static final int[] byteTable4;
    private static final int[] byteTable5;
    private static final int[] byteTable6;
    private static final int[] byteTable7;
    private int crc = -1;

    public MyCrc32c() {
    }

    public void update(int b) {
        this.crc = this.crc >>> 8 ^ byteTable[(this.crc ^ b & 255) & 255];
    }

    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off >= 0 && len >= 0 && off <= b.length - len) {
            this.crc = updateBytes(this.crc, b, off, off + len);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }


    public void reset() {
        this.crc = -1;
    }

    public long getValue() {
        return (long)(~this.crc) & 4294967295L;
    }

    private static int updateBytes(int crc, byte[] b, int off, int end) {
        if (end - off >= 8 && Unsafe.ARRAY_BYTE_INDEX_SCALE == 1) {
            int alignLength = 8 - (Unsafe.ARRAY_BYTE_BASE_OFFSET + off & 7) & 7;

            int firstHalf;
            for(firstHalf = off + alignLength; off < firstHalf; ++off) {
                crc = crc >>> 8 ^ byteTable[(crc ^ b[off]) & 255];
            }

            // 判断是否为 BIG_ENDIAN，并调整 CRC
            boolean isBigEndian = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
            if (isBigEndian) {
                crc = Integer.reverseBytes(crc);
            }

            for(; off < end - 8; off += 8) {
                int secondHalf;
                if (Unsafe.ADDRESS_SIZE == 4) {
                    firstHalf = UNSAFE.getInt(b, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + (long)off);
                    secondHalf = UNSAFE.getInt(b, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + (long)off + 4L);
                } else {
                    long value = UNSAFE.getLong(b, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + (long)off);
                    if (isBigEndian) {
                        firstHalf = (int)(value >>> 32);
                        secondHalf = (int)value;
                    } else {
                        firstHalf = (int)value;
                        secondHalf = (int)(value >>> 32);
                    }
                }

                crc ^= firstHalf;
                // 根据 Endian 设置 CRC
                if (isBigEndian) {
                    crc = processBigEndian(crc, secondHalf);
                } else {
                    crc = processLittleEndian(crc, secondHalf);
                }
            }

            if (isBigEndian) {
                crc = Integer.reverseBytes(crc);
            }
        }

        while(off < end) {
            crc = crc >>> 8 ^ byteTable[(crc ^ b[off]) & 255];
            ++off;
        }

        return crc;
    }

    // 处理 Big Endian 的 CRC 计算
    private static int processBigEndian(int crc, int secondHalf) {
        return byteTable0[secondHalf & 255] ^ byteTable1[secondHalf >>> 8 & 255] ^ byteTable2[secondHalf >>> 16 & 255]
                ^ byteTable3[secondHalf >>> 24] ^ byteTable4[crc & 255] ^ byteTable5[crc >>> 8 & 255]
                ^ byteTable6[crc >>> 16 & 255] ^ byteTable7[crc >>> 24];
    }

    // 处理 Little Endian 的 CRC 计算
    private static int processLittleEndian(int crc, int secondHalf) {
        return byteTable7[crc & 255] ^ byteTable6[crc >>> 8 & 255] ^ byteTable5[crc >>> 16 & 255]
                ^ byteTable4[crc >>> 24] ^ byteTable3[secondHalf & 255] ^ byteTable2[secondHalf >>> 8 & 255]
                ^ byteTable1[secondHalf >>> 16 & 255] ^ byteTable0[secondHalf >>> 24];
    }

    private static int updateDirectByteBuffer(int crc, long address, int off, int end) {
        if (end - off >= 8) {
            int alignLength = 8 - (int)(address + (long)off & 7L) & 7;

            int firstHalf;
            for(firstHalf = off + alignLength; off < firstHalf; ++off) {
                crc = crc >>> 8 ^ byteTable[(crc ^ UNSAFE.getByte(address + (long)off)) & 255];
            }

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                crc = Integer.reverseBytes(crc);
            }

            for(; off <= end - 8; off += 8) {
                firstHalf = UNSAFE.getInt(address + (long)off);
                int secondHalf = UNSAFE.getInt(address + (long)off + 4L);
                crc ^= firstHalf;
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    crc = byteTable7[crc & 255] ^ byteTable6[crc >>> 8 & 255] ^ byteTable5[crc >>> 16 & 255] ^ byteTable4[crc >>> 24] ^ byteTable3[secondHalf & 255] ^ byteTable2[secondHalf >>> 8 & 255] ^ byteTable1[secondHalf >>> 16 & 255] ^ byteTable0[secondHalf >>> 24];
                } else {
                    crc = byteTable0[secondHalf & 255] ^ byteTable1[secondHalf >>> 8 & 255] ^ byteTable2[secondHalf >>> 16 & 255] ^ byteTable3[secondHalf >>> 24] ^ byteTable4[crc & 255] ^ byteTable5[crc >>> 8 & 255] ^ byteTable6[crc >>> 16 & 255] ^ byteTable7[crc >>> 24];
                }
            }

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                crc = Integer.reverseBytes(crc);
            }
        }

        while(off < end) {
            crc = crc >>> 8 ^ byteTable[(crc ^ UNSAFE.getByte(address + (long)off)) & 255];
            ++off;
        }

        return crc;
    }

    static {
        byteTable0 = byteTables[0];
        byteTable1 = byteTables[1];
        byteTable2 = byteTables[2];
        byteTable3 = byteTables[3];
        byteTable4 = byteTables[4];
        byteTable5 = byteTables[5];
        byteTable6 = byteTables[6];
        byteTable7 = byteTables[7];

        int index;
        int reversedCrcValue;
        int byteIndex;
        for(index = 0; index < byteTables[0].length; ++index) {
            reversedCrcValue = index;

            for(byteIndex = 0; byteIndex < 8; ++byteIndex) {
                if ((reversedCrcValue & 1) != 0) {
                    reversedCrcValue = reversedCrcValue >>> 1 ^ REVERSED_CRC32C_POLY;
                } else {
                    reversedCrcValue >>>= 1;
                }
            }

            byteTables[0][index] = reversedCrcValue;
        }

        for(index = 0; index < byteTables[0].length; ++index) {
            reversedCrcValue = byteTables[0][index];

            for(byteIndex = 1; byteIndex < byteTables.length; ++byteIndex) {
                reversedCrcValue = byteTables[0][reversedCrcValue & 255] ^ reversedCrcValue >>> 8;
                byteTables[byteIndex][index] = reversedCrcValue;
            }
        }

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            byteTable = byteTables[0];
        } else {
            byteTable = new int[byteTable0.length];
            System.arraycopy(byteTable0, 0, byteTable, 0, byteTable0.length);
            int[][] var5 = byteTables;
            reversedCrcValue = var5.length;

            for(byteIndex = 0; byteIndex < reversedCrcValue; ++byteIndex) {
                int[] table = var5[byteIndex];

                for(int i = 0; i < table.length; ++i) {
                    table[i] = Integer.reverseBytes(table[i]);
                }
            }
        }

    }
}
