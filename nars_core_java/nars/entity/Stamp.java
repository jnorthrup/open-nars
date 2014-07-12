/*
 * Stamp.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.entity;

import java.util.*;

import nars.io.Symbols;
import nars.main_nogui.Parameters;
import nars.main_nogui.ReasonerBatch;
import nars.language.Term;

/**
 * Each Sentence has a time stamp, consisting the following components: (1) The
 * creation time of the sentence, (2) A evidentialBase of serial numbers of
 * sentence, from which the sentence is derived. Each input sentence gets a
 * unique serial number, though the creation time may be not unique. The derived
 * sentences inherits serial numbers from its parents, cut at the baseLength
 * limit.
 */
public class Stamp implements Cloneable {

    /**
     * serial number, for the whole system TODO : should it really be static? or
     * a Stamp be a field in {@link ReasonerBatch} ?
     */
    private static long currentSerial = 0;
    /**
     * serial numbers
     */
    private long[] evidentialBase;
    /**
     * evidentialBase baseLength
     */
    private int baseLength;
    /**
     * creation time of the stamp
     */
    private long creationTime;
    /**
     * derivation chain containing the used premises and conclusions which made
     * deriving the conclusion c possible *
     */
    private ArrayList<Term> derivationChain;

    /**
     * Generate a new stamp, with a new serial number, for a new Task
     *
     * @param time Creation time of the stamp
     */
    public Stamp(long time) {
        currentSerial++;
        baseLength = 1;
        evidentialBase = new long[baseLength];
        evidentialBase[0] = currentSerial;
        creationTime = time;
        derivationChain = new ArrayList<Term>();
    }

    /**
     * Generate a new stamp identical with a given one
     *
     * @param old The stamp to be cloned
     */
    private Stamp(Stamp old) {
        baseLength = old.baseLength();
        evidentialBase = old.getBase();
        creationTime = old.getCreationTime();
        derivationChain = old.getChain();
    }

    /**
     * Generate a new stamp from an existing one, with the same evidentialBase
     * but different creation time
     * <p>
     * For single-premise rules
     *
     * @param old The stamp of the single premise
     * @param time The current time
     */
    public Stamp(Stamp old, long time) {
        baseLength = old.baseLength();
        evidentialBase = old.getBase();
        creationTime = time;
        derivationChain = old.getChain();
    }

    /**
     * Generate a new stamp for derived sentence by merging the two from parents
     * the first one is no shorter than the second
     *
     * @param first The first Stamp
     * @param second The second Stamp
     */
    private Stamp(Stamp first, Stamp second, long time) {
        int i1, i2, j;
        i1 = i2 = j = 0;
        baseLength = Math.min(first.baseLength() + second.baseLength(), Parameters.MAXIMUM_EVIDENTAL_BASE_LENGTH);
        evidentialBase = new long[baseLength];
        while (i2 < second.baseLength() && j < baseLength) {
            evidentialBase[j] = first.get(i1);
            i1++;
            j++;
            evidentialBase[j] = second.get(i2);
            i2++;
            j++;
        }
        while (i1 < first.baseLength() && j < baseLength) {
            evidentialBase[j] = first.get(i1);
            i1++;
            j++;
        }
        List<Term> chain1 = first.getChain();
        List<Term> chain2 = second.getChain();
        i1 = chain1.size() - 1;
        i2 = chain2.size() - 1;
        j = 0;
        derivationChain = new ArrayList<Term>(); //take as long till the chain is full or all elements were taken out of chain1 and chain2:
        while (j < Parameters.MAXIMUM_DERIVATION_CHAIN_LENGTH && (i1 >= 0 || i2 >= 0)) {
            if (j % 2 == 0) {//one time take from first, then from second, last ones are more important
                if (i1 >= 0) {
                    derivationChain.add(chain1.get(i1));
                    i1--;
                }
            } else {
                if (i2 >= 0) {
                    derivationChain.add(chain2.get(i2));
                    i2--;
                }
            }
            j++;
        } //ok but now the most important elements are at the beginning so let's change that:
        Collections.reverse(derivationChain); //if jvm implements that correctly this is O(1)
        creationTime = time;
    }

    /**
     * Try to merge two Stamps, return null if have overlap
     * <p>
     * By default, the event time of the first stamp is used in the result
     *
     * @param first The first Stamp
     * @param second The second Stamp
     * @param time The new creation time
     * @return The merged Stamp, or null
     */
    public static Stamp make(Stamp first, Stamp second, long time) {
// temporarily comment out
//        if (equalBases(first.getBase(), second.getBase())) {
//            return null;  // do not merge identical bases
//        }
        if (first.baseLength() > second.baseLength()) {
            return new Stamp(first, second, time);
        } else {
            return new Stamp(second, first, time);
        }
    }

//    private static boolean equalBases(long[] base1, long[] base2) {
//        if (base1.length != base2.length) {
//            return false;
//        }
//        for (long n1 : base1) {
//            boolean found = false;
//            for (long n2 : base2) {
//                if (n1 == n2) {
//                    found = true;
//                }
//            }
//            if (!found) {
//                return false;
//            }
//        }
//        return true;
//    }

    /**
     * Clone a stamp
     *
     * @return The cloned stamp
     */
    @Override
    public Object clone() {
        return new Stamp(this);
    }

    /**
     * Initialize the stamp mechanism of the system, called in Reasoner
     */
    public static void init() {
        currentSerial = 0;
    }

    /**
     * Return the baseLength of the evidentialBase
     *
     * @return Length of the Stamp
     */
    public int baseLength() {
        return baseLength;
    }

    /**
     * Return the chainLength of the derivationChain
     *
     * @return Length of the Stamp
     */
    public int derivationLength() {
        return derivationChain.size();
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     *
     * @param i The index
     * @return The number at the index
     */
    long get(int i) {
        return evidentialBase[i];
    }

    /**
     * Get the evidentialBase, called from derivedTask in Memory
     *
     * @return The evidentialBase of numbers
     */
    public long[] getBase() {
        return evidentialBase;
    }

    /**
     * Get the derivationChain, called from derivedTask in Memory
     *
     * @return The evidentialBase of numbers
     */
    public ArrayList<Term> getChain() {
        return derivationChain;
    }

    /**
     * Add element to the chain
     *
     * @return The evidentialBase of numbers
     */
    public void addToChain(Term T) {
        derivationChain.add(T);
        if (derivationChain.size() > Parameters.MAXIMUM_DERIVATION_CHAIN_LENGTH) {
            derivationChain.remove(0);
        }
    }

    /**
     * Convert the evidentialBase into a set
     *
     * @return The TreeSet representation of the evidential base
     */
    private TreeSet<Long> toSet() {
        TreeSet<Long> set = new TreeSet<>();
        for (int i = 0; i < baseLength; i++) {
            set.add(evidentialBase[i]);
        }
        return set;
    }

    /**
     * Check if two stamps contains the same content
     *
     * @param that The Stamp to be compared
     * @return Whether the two have contain the same elements
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Stamp)) {
            return false;
        }
        TreeSet<Long> set1 = toSet();
        TreeSet<Long> set2 = ((Stamp) that).toSet();
        return (set1.containsAll(set2) && set2.containsAll(set1));
    }

    /**
     * The hash code of Stamp
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Get the creationTime of the truth-value
     *
     * @return The creation time
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Get a String form of the Stamp for display Format: {creationTime [:
     * eventTime] : evidentialBase}
     *
     * @return The Stamp as a String
     */
    @Override
    public String toString() {
        boolean show_derivation_chain=true;
        StringBuilder buffer = new StringBuilder(" " + Symbols.STAMP_OPENER + creationTime);
        buffer.append(" ").append(Symbols.STAMP_STARTER).append(" ");
        for (int i = 0; i < baseLength; i++) {
            buffer.append(Long.toString(evidentialBase[i]));
            if (i < (baseLength - 1)) {
                buffer.append(Symbols.STAMP_SEPARATOR);
            } else {
                if (show_derivation_chain && derivationChain.size() > 0) {
                    buffer.append(" ").append(Symbols.STAMP_STARTER).append(" ");
                }
            }
        }
        if(show_derivation_chain) {
            for (int i = 0; i < derivationChain.size(); i++) {
                buffer.append(derivationChain.get(i));
                if (i < (derivationChain.size() - 1)) {
                    buffer.append(Symbols.STAMP_SEPARATOR);
                }
            }
        }
        buffer.append(Symbols.STAMP_CLOSER).append(" ");
        return buffer.toString();
    }
}
