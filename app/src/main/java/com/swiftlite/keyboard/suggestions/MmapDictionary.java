package com.swiftlite.keyboard.suggestions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MmapDictionary implements Closeable {

    private static final int MAGIC        = 0x53574454;
    private static final int HEADER_BYTES = 8;

    private final MappedByteBuffer mBuf;
    private final int mCount;
    private final int mOffsetTableStart;
    private final int mDataStart;
    private RandomAccessFile mRaf;

    private String[] mUserWords;

    public MmapDictionary(File file) throws IOException {
        mRaf = new RandomAccessFile(file, "r");
        FileChannel ch = mRaf.getChannel();
        mBuf = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
        mBuf.order(ByteOrder.BIG_ENDIAN);
        ch.close();
        int magic = mBuf.getInt(0);
        if (magic != MAGIC) throw new IOException("Bad dict magic: 0x" + Integer.toHexString(magic));
        mCount = mBuf.getInt(4);
        mOffsetTableStart = HEADER_BYTES;
        mDataStart = HEADER_BYTES + mCount * 4;
    }

    public void setUserWords(String[] words) { mUserWords = words; }
    public String[] getUserWords()           { return mUserWords; }

    public int size() { return mCount; }

    public byte getCategory(int index) {
        int entryOff = mDataStart + mBuf.getInt(mOffsetTableStart + index * 4);
        return mBuf.get(entryOff);
    }

    public int getWordLength(int index) {
        int entryOff = mDataStart + mBuf.getInt(mOffsetTableStart + index * 4);
        return mBuf.get(entryOff + 1) & 0xFF;
    }

    public char getWordFirstCharLower(int index) {
        int entryOff = mDataStart + mBuf.getInt(mOffsetTableStart + index * 4);
        int b = mBuf.get(entryOff + 2) & 0xFF;
        return Character.toLowerCase((char) b);
    }

    public String getWord(int index) {
        int entryOff = mDataStart + mBuf.getInt(mOffsetTableStart + index * 4);
        int len = mBuf.get(entryOff + 1) & 0xFF;
        byte[] bytes = new byte[len];
        ByteBuffer slice = mBuf.duplicate();
        slice.position(entryOff + 2);
        slice.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String getLowerWord(int index) {
        return getWord(index).toLowerCase(Locale.ROOT);
    }

    public int binarySearch(String lowerTarget) {
        byte[] target = lowerTarget.getBytes(StandardCharsets.UTF_8);
        int low = 0, high = mCount - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = compareEntryTo(mid, target);
            if      (cmp < 0) low  = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else              return mid;
        }
        return -(low + 1);
    }

    public int prefixSearchStart(byte[] prefixUtf8) {
        int low = 0, high = mCount - 1, result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = entryStartsWith(mid, prefixUtf8);
            if (cmp < 0) {
                low = mid + 1;
            } else {
                if (cmp == 0) result = mid;
                high = mid - 1;
            }
        }
        return result;
    }

    private int compareEntryTo(int index, byte[] target) {
        int entryOff = mDataStart + mBuf.getInt(mOffsetTableStart + index * 4);
        int len      = mBuf.get(entryOff + 1) & 0xFF;
        int dataOff  = entryOff + 2;
        int minLen   = Math.min(len, target.length);
        for (int i = 0; i < minLen; i++) {
            int a = mBuf.get(dataOff + i) & 0xFF;
            int b = target[i] & 0xFF;
            if (a != b) return a - b;
        }
        return len - target.length;
    }

    private int entryStartsWith(int index, byte[] prefix) {
        int entryOff = mDataStart + mBuf.getInt(mOffsetTableStart + index * 4);
        int len      = mBuf.get(entryOff + 1) & 0xFF;
        if (len < prefix.length)
            return compareEntryTo(index, prefix) < 0 ? -1 : 1;
        int dataOff = entryOff + 2;
        for (int i = 0; i < prefix.length; i++) {
            int a = mBuf.get(dataOff + i) & 0xFF;
            int b = prefix[i] & 0xFF;
            if (a != b) return a - b;
        }
        return 0;
    }

    @Override
    public void close() {
        if (mRaf != null) {
            try { mRaf.close(); } catch (IOException ignored) {}
            mRaf = null;
        }
    }
}
