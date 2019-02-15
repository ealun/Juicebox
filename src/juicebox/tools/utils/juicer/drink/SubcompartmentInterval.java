/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2018 Broad Institute, Aiden Lab
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.tools.utils.juicer.drink;

import com.google.common.primitives.Ints;
import juicebox.data.feature.Feature;
import juicebox.data.feature.FeatureFilter;
import juicebox.data.feature.FeatureFunction;
import juicebox.data.feature.GenomeWideList;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class SubcompartmentInterval extends SimpleInterval {

    private Integer clusterID;

    private static Color[] colors = new Color[]{
            new Color(255, 0, 0),
            new Color(255, 255, 0),
            new Color(0, 234, 255),
            new Color(170, 0, 255),
            new Color(255, 127, 0),
            new Color(191, 255, 0),
            new Color(0, 149, 255),
            new Color(255, 0, 170),
            new Color(255, 212, 0),
            new Color(106, 255, 0),
            new Color(0, 64, 255),
            new Color(237, 185, 185),
            new Color(185, 215, 237),
            new Color(231, 233, 185),
            new Color(220, 185, 237),
            new Color(185, 237, 224),
            new Color(143, 35, 35),
            new Color(35, 98, 143),
            new Color(143, 106, 35),
            new Color(107, 35, 143),
            new Color(79, 143, 35),
            new Color(0, 0, 0),
            new Color(115, 115, 115),
            new Color(204, 204, 204)
    };


    public static void collapseGWList(GenomeWideList<SubcompartmentInterval> intraSubcompartments) {
        intraSubcompartments.filterLists(new FeatureFilter<SubcompartmentInterval>() {
            @Override
            public List<SubcompartmentInterval> filter(String chr, List<SubcompartmentInterval> featureList) {
                return collapseSubcompartmentIntervals(featureList);
            }
        });
    }

    public static void extractDiffVectors(List<GenomeWideList<SubcompartmentInterval>> comparativeSubcompartments,
                                          Map<Integer, double[]> idToCentroidMap, File outputDirectory) {
        // extract differences relative to control
        // calculate consensus

        GenomeWideList<SubcompartmentInterval> consensus = calculateConsensus(comparativeSubcompartments);


        // extract differences relative to consensus


    }

    private static GenomeWideList<SubcompartmentInterval> calculateConsensus(
            final List<GenomeWideList<SubcompartmentInterval>> comparativeSubcompartments) {

        GenomeWideList<SubcompartmentInterval> control = comparativeSubcompartments.get(0);
        GenomeWideList<SubcompartmentInterval> consensus = control.deepClone();

        final Map<SimpleInterval, Integer> modeOfClusterIdsInInterval = new HashMap<>();

        control.processLists(new FeatureFunction<SubcompartmentInterval>() {
            @Override
            public void process(String chr, List<SubcompartmentInterval> controlList) {

                Map<SimpleInterval, List<Integer>> clusterIdsInInterval = new HashMap<>();
                for (SubcompartmentInterval sInterval : controlList) {
                    List<Integer> ids = new ArrayList<>();
                    ids.add(sInterval.getClusterID());
                    clusterIdsInInterval.put(sInterval.getSimpleIntervalKey(), ids);
                }

                if (comparativeSubcompartments.size() > 1) {
                    for (int i = 1; i < comparativeSubcompartments.size(); i++) {
                        for (SubcompartmentInterval sInterval : comparativeSubcompartments.get(i).getFeatures(chr)) {
                            if (clusterIdsInInterval.containsKey(sInterval.getSimpleIntervalKey())) {
                                clusterIdsInInterval.get(sInterval.getSimpleIntervalKey()).add(sInterval.getClusterID());
                            } else {
                                List<Integer> ids = new ArrayList<>();
                                ids.add(sInterval.getClusterID());
                                clusterIdsInInterval.put(sInterval.getSimpleIntervalKey(), ids);
                            }
                        }
                    }
                }


                for (SimpleInterval sInterval : clusterIdsInInterval.keySet()) {
                    int mode = mode(clusterIdsInInterval.get(sInterval));
                    if (mode >= 0) {
                        modeOfClusterIdsInInterval.put(sInterval, mode);
                    }
                }
            }
        });

        consensus.processLists(new FeatureFunction<SubcompartmentInterval>() {
            @Override
            public void process(String chr, List<SubcompartmentInterval> featureList) {
                for (SubcompartmentInterval sInterval : featureList) {
                    SimpleInterval key = sInterval.getSimpleIntervalKey();
                    if (modeOfClusterIdsInInterval.containsKey(key)) {
                        sInterval.setClusterID(modeOfClusterIdsInInterval.get(key));
                    }
                }
            }
        });

        return null;
    }

    /**
     * @param integers
     * @return mode only if there is exactly one value repeated the max number of times
     */
    private static Integer mode(List<Integer> integers) {

        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer i : integers) {
            if (counts.containsKey(i)) {
                counts.put(i, counts.get(i) + 1);
            } else {
                counts.put(i, 1);
            }
        }

        int maxNumTimesCount = Ints.max(Ints.toArray(counts.values()));
        int howManyTimesDoesMaxAppear = 0;
        int potentialModeVal = -1;
        for (Integer val : counts.keySet()) {
            if (counts.get(val).equals(maxNumTimesCount)) {
                potentialModeVal = val;
                howManyTimesDoesMaxAppear++;
            }
        }

        if (howManyTimesDoesMaxAppear == 1) {
            return potentialModeVal;
        }

        return -1;
    }

    private void setClusterID(Integer clusterID) {
        this.clusterID = clusterID;
    }

    public static void reSort(GenomeWideList<SubcompartmentInterval> subcompartments) {
        subcompartments.filterLists(new FeatureFilter<SubcompartmentInterval>() {
            @Override
            public List<SubcompartmentInterval> filter(String chr, List<SubcompartmentInterval> featureList) {
                Collections.sort(featureList);
                return featureList;
            }
        });
    }

    public SubcompartmentInterval(int chrIndex, String chrName, int x1, int x2, Integer clusterID) {
        super(chrIndex, chrName, x1, x2);
        this.clusterID = clusterID;
    }

    private static List<SubcompartmentInterval> collapseSubcompartmentIntervals(List<SubcompartmentInterval> intervals) {
        if (intervals.size() > 0) {

            Collections.sort(intervals);
            SubcompartmentInterval collapsedInterval = (SubcompartmentInterval) intervals.get(0).deepClone();

            Set<SubcompartmentInterval> newIntervals = new HashSet<>();
            for (SubcompartmentInterval nextInterval : intervals) {
                if (collapsedInterval.overlapsWith(nextInterval)) {
                    collapsedInterval = collapsedInterval.absorbAndReturnNewInterval(nextInterval);
                } else {
                    newIntervals.add(collapsedInterval);
                    collapsedInterval = (SubcompartmentInterval) nextInterval.deepClone();
                }
            }
            newIntervals.add(collapsedInterval);

            List<SubcompartmentInterval> newIntervalsSorted = new ArrayList<>(newIntervals);
            Collections.sort(newIntervalsSorted);

            return newIntervalsSorted;
        }
        return intervals;
    }

    private boolean overlapsWith(SubcompartmentInterval o) {
        return getChrIndex().equals(o.getChrIndex()) && clusterID.equals(o.clusterID) && getX2().equals(o.getX1());
    }


    public Integer getClusterID() {
        return clusterID;
    }

    private SubcompartmentInterval absorbAndReturnNewInterval(SubcompartmentInterval interval) {
        return new SubcompartmentInterval(getChrIndex(), getChrName(), getX1(), interval.getX2(), clusterID);
    }

    @Override
    public Feature deepClone() {
        return new SubcompartmentInterval(getChrIndex(), getChrName(), getX1(), getX2(), clusterID);
    }

    @Override
    public String toString() {
        Color color = colors[clusterID % colors.length];
        String colorString = color.getRed() + "," + color.getGreen() + "," + color.getBlue();
        return "chr" + getChrName() + "\t" + getX1() + "\t" + getX2() + "\t" + clusterID + "\t" + clusterID
                + "\t.\t" + getX1() + "\t" + getX2() + "\t" + colorString;
    }
}