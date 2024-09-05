package org.AutoHealerAndClusterSearch.ObjectExchangeInCluster;

import java.io.Serializable;
import java.util.*;

public class SearchQueryResponse implements Serializable {

    private Map<String, List<FileWordPair>> wordFrequencies;

    public SearchQueryResponse()
    {
        wordFrequencies = new HashMap<>();
    }

    public void setWordFrequencies(Map<String , List<FileWordPair>> preCalced)
    {
        wordFrequencies = preCalced;
    }

    public Map<String, List<FileWordPair>> getWordFrequencies()
    {
        return wordFrequencies;
    }

    public Map<String , Double> calcScoreForFilesContainingWord(String word , Double idf)
    {
        List<FileWordPair> temp = wordFrequencies.getOrDefault(word , new ArrayList<>());
        Map<String , Double> ans = new HashMap<>();
        if (temp.size() == 0) return ans;
        for (FileWordPair fileWordPair : temp)
        {
            ans.put(fileWordPair.fileName , fileWordPair.freq*idf);
        }
        return ans;

    }
}


