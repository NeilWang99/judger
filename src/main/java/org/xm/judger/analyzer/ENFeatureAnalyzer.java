package org.xm.judger.analyzer;

import org.xm.judger.domain.Config;
import org.xm.judger.domain.ENEssayInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Analyze feature
 *
 * @author xuming
 */
public class ENFeatureAnalyzer {
    public static void correlationTest(ArrayList<ENEssayInstance> instances, int id) {
        ArrayList<ENEssayInstance> list = filter(instances, id);
        assert (list.size() > 0);
        if (Config.DEBUG) {
            System.out.println("Pearson correlation coefficients with domain1_score for essay " + id + ", " + list.size() + " instances");

            for (String feature : list.get(0).listFeatures())
                System.out.println("\tr for" + feature + ": " + pearson(list, feature));
        }
    }

    /**
     * Compute Pearson correlation coefficient between the domain_score1 and the specified feature.
     * Note:  You shouldn't run this on the full set of instances, but filter by instance type.
     * <p>
     * I originally tried Spearman correlation, but when a feature is undefined that makes it sort
     * according to essay_id, which (ironically) is highly correlated for some reason.  The code is
     * ugly as a result of coding Spearman first (but you can swap back and forth with 2 line changes).
     * <p>
     * Note that because we're passing in a single task's instances, any of the normalizations are
     * basically divide-by-invariant.  So they have minimal effect.
     *
     * @author Keith Trnka
     */
    public static double pearson(ArrayList<ENEssayInstance> filteredInstances, final String feature) {
        // sort with domain_score
        Collections.sort(filteredInstances, (a, b) -> a.domain1_score - b.domain1_score);
        // store ranks according domain_score
        HashMap<Integer, Integer> scoreRanks = new HashMap<>();
        for (int i = 0; i < filteredInstances.size(); i++)
            scoreRanks.put(filteredInstances.get(i).id, filteredInstances.get(i).domain1_score);

        // store them according the feature
        Collections.sort(filteredInstances, (a, b) -> (int) (10000 * (a.getFeature(feature) - b.getFeature(feature))));
        HashMap<Integer, Integer> featureRanks = new HashMap<>();
        for (int i = 0; i < filteredInstances.size(); i++)
            featureRanks.put(filteredInstances.get(i).id, (int) (10000 * filteredInstances.get(i).getFeature(feature)));

        //compute mean domain and mean feature
        double meanScoreRank = 0;
        for (int i : scoreRanks.values()) meanScoreRank += i;
        meanScoreRank /= filteredInstances.size();

        double meanFeatureRank = 0;
        for (int i : featureRanks.values())
            meanFeatureRank += i;
        meanFeatureRank /= filteredInstances.size();

        //compute the components
        double prod = 0;
        double scoreSquare = 0;
        double featureSquare = 0;
        for (ENEssayInstance instance : filteredInstances) {
            double scoreRank = scoreRanks.get(instance.id);
            double featureRank = featureRanks.get(instance.id);
            double scoreDiff = scoreRank - meanScoreRank;
            double featureDiff = featureRank - meanFeatureRank;
            prod += scoreDiff * featureDiff;
            scoreSquare += scoreDiff * scoreDiff;
            featureSquare += featureDiff * featureDiff;
        }
        double result = prod / Math.sqrt(scoreSquare * featureSquare);
        return result;
    }

    public static ArrayList<ENEssayInstance> filter(ArrayList<ENEssayInstance> instances, int set) {
        ArrayList<ENEssayInstance> filterList = instances
                .stream()
                .filter(i -> i.set == set)
                .collect(Collectors.toCollection(ArrayList::new));
        return filterList;
    }

    public static HashMap<Double, int[]> buildHistogram(ArrayList<ENEssayInstance> instances, String feature) {
        HashMap<Double, int[]> histogram = new HashMap<>();
        for (ENEssayInstance instance : instances) {
            Double value = Double.valueOf(instance.getFeature(feature));
            if (histogram.containsKey(value))
                histogram.get(value)[0]++;
            else
                histogram.put(value, new int[]{1});
        }
        return histogram;
    }

    public static void analyseFeature(ArrayList<ENEssayInstance> instances, String feature) {
        System.out.println("Analysis of feature " + feature);
        // count the number of unique values and/or decide if it's discrete
        HashMap<Double, int[]> histogram = buildHistogram(instances, feature);

        if (histogram.size() <= 20) {
            System.out.println("\tdiscrete, " + histogram.size() + " values");

            ArrayList<Double> values = new ArrayList<>(histogram.keySet());
            Collections.sort(values);
            for (Double value : values)
                System.out.println("\t\t" + value + ": " + histogram.get(value)[0]);
        } else {
            System.out.println("\tcontinuous");
        }

        System.out.println("\tmean: " + getMean(instances, feature));
    }

    public static double getMean(ArrayList<ENEssayInstance> instances, String feature) {
        double sum = 0;
        for (ENEssayInstance instance : instances)
            sum += instance.getFeature(feature);

        return sum / instances.size();
    }

    public static void analysis(ArrayList<ENEssayInstance> instances) {
        // analysis features are good or bad
        correlationTest(instances, 1);
//        correlationTest(instances, 2);
//        correlationTest(instances, 3);
        // show the average score, etc
        analyseFeature(filter(instances, 1), "grade");
    }

}
