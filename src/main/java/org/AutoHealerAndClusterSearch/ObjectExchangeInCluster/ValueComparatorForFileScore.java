package org.AutoHealerAndClusterSearch.ObjectExchangeInCluster;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ValueComparatorForFileScore implements Comparator<Map.Entry<String,Double>>
{
    /*Map<String, Double> base;

    public ValueComparatorForFileScore() {
        this.base = new TreeMap<>();
    }

    public ValueComparatorForFileScore(TreeMap<String, Double> base) {
        this.base = base;
    }*/

    @Override
    public int compare(Map.Entry<String,Double> a, Map.Entry<String,Double> b) {
        return Double.compare(a.getValue() , b.getValue());
    }

}
