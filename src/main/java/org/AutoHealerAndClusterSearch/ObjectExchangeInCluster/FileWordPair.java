package org.AutoHealerAndClusterSearch.ObjectExchangeInCluster;

import java.io.Serializable;

public class FileWordPair implements Serializable
{
    public String fileName;
    public double freq;

    public FileWordPair (String fileName , double freq)
    {
        this.fileName = fileName;
        this.freq = freq;
    }
    @Override
    public String toString()
    {
        return "appeared in : [ "+ fileName+" ] " + freq;
    }
}
