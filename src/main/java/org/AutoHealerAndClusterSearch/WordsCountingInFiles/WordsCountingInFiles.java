package org.AutoHealerAndClusterSearch.WordsCountingInFiles;

import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.FileWordPair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class WordsCountingInFiles
{


    public static Map<String, List<FileWordPair>> countWordsInFiles(List<String> words, List<String> filePaths)
    {
        Map<String, List<FileWordPair>> wordOccurrences = new HashMap<>();

        for (String filePath : filePaths)
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
            {
                StringBuilder textBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    textBuilder.append(line).append("\n");
                }
                String text = textBuilder.toString();

                int size = text.length();
                size = Math.max(size,1);
                // Count occurrences of words in the text
                for (String word : words)
                {
                    int count = countWordOccurrencesInText(word, text);

                    if (count == 0) continue;

                    double freq = 1.0*(count)/size;
                    List<FileWordPair> FileWordPair = wordOccurrences.getOrDefault(word, new ArrayList<>());

                    FileWordPair.add(new FileWordPair(filePath, freq));
                    wordOccurrences.put(word, FileWordPair);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wordOccurrences;
    }


    public static Map<String, List<FileWordPair>> countWordsInFiles(String query, List<String> filePaths)
    {
        String[] temp = query.split(" ");
        List<String> words = new ArrayList<>();
        for (String s : temp)
        {
            words.add(s);
        }
        return countWordsInFiles(words , filePaths);
    }

    public static int countWordOccurrencesInText(String word, String text)
    {
        int count = 0;
        int index = text.toLowerCase().indexOf(word.toLowerCase());
        while (index != -1)
        {
            count++;
            index = text.toLowerCase().indexOf(word.toLowerCase(), index + 1);
        }
        return count;
    }
}
