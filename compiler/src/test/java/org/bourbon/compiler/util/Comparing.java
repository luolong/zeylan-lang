package org.bourbon.compiler.util;

import java.util.Comparator;

public class Comparing {

    public static Comparator<CharSequence> charSequence() {
        return CharSeqComparator.INSTANCE;
    }

    enum CharSeqComparator implements Comparator<CharSequence> {
        INSTANCE;

        @Override
        public int compare(CharSequence s1, CharSequence s2) {
            int len = Math.min(s1.length(), s2.length());

            // find the first difference and return
            for (int i = 0; i < len; i += 1) {
                int cmp = Character.compare(s1.charAt(i), s2.charAt(i));
                if (cmp != 0) {
                    return cmp;
                }
            }

            // if there are no differences, then the shorter seq is first
            return Integer.compare(s1.length(), s2.length());
        }
    }
}
