package com.swiftlite.keyboard.suggestions;

import java.util.List;
import java.util.Locale;

public class CompactDictionary {
    private final String mAllWords;
    private final int[] mOffsets;
    private final byte[] mCategories;
    private final int mCount;

    public CompactDictionary(List<DictWord> words) {
        mCount = words.size();
        StringBuilder sb = new StringBuilder();
        mOffsets = new int[mCount + 1];
        mCategories = new byte[mCount];
        
        for (int i = 0; i < mCount; i++) {
            DictWord dw = words.get(i);
            mOffsets[i] = sb.length();
            mCategories[i] = dw.category;
            sb.append(dw.word);
        }
        mOffsets[mCount] = sb.length();
        mAllWords = sb.toString();
    }

    public int size() { return mCount; }

    public String getWord(int index) {
        if (index < 0 || index >= mCount) return null;
        return mAllWords.substring(mOffsets[index], mOffsets[index + 1]);
    }

    public byte getCategory(int index) {
        if (index < 0 || index >= mCount) return -1;
        return mCategories[index];
    }

    public String getLowerWord(int index) {
        String w = getWord(index);
        return w != null ? w.toLowerCase(Locale.getDefault()) : null;
    }

    public int binarySearch(String lowerTarget) {
        int low = 0, high = mCount - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = getLowerWord(mid).compareTo(lowerTarget);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }
}
