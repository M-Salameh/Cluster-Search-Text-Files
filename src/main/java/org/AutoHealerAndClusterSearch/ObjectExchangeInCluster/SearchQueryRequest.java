package org.AutoHealerAndClusterSearch.ObjectExchangeInCluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchQueryRequest implements Serializable
{
    private String query;
    private int numberOfFilesToScan;
    private int filesOffset;
    public SearchQueryRequest(String query , int numberOfFilesToScan , int filesOffset)
    {
        this.numberOfFilesToScan = numberOfFilesToScan;
        this.filesOffset = filesOffset;
        this.query = query;
    }
    public List<String> getQueryWords()
    {
        String[] qs = query.split(" ");
        List<String> words = new ArrayList<>();
        for (String w : qs)
        {
            words.add(w);
        }
        return words;
    }
    public int getNumberOfFilesToScan()
    {
        return numberOfFilesToScan;
    }
    public int getFilesOffset()
    {
        return filesOffset;
    }
}
