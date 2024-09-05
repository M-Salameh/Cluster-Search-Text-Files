package org.AutoHealerAndClusterSearch.generalTesting;

import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.FileWordPair;
import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.ValueComparatorForFileScore;
import org.AutoHealerAndClusterSearch.WordsCountingInFiles.WordsCountingInFiles;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class test
{

    public static void listFilesForFolder(String filesLocation)
    {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(Paths.get(filesLocation)))
        {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    fileNames.add(file.getFileName().toString());
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        for (String file : fileNames) System.out.println(file);
    }
    
    public static void countWordsInFiles()
    {
        String path = System.getProperty("user.dir") + "/SearchFiles/";
        List<String> fs = new ArrayList<>();
        fs.add(path+"f (1).txt");
        fs.add(path+"f (3).txt");
        fs.add(path+"f (5).txt");
        fs.add(path+"f (6).txt");
        String query ;
        Scanner scanner = new Scanner(System.in);
        query= scanner.nextLine();
        String[] ws = query.split(" ");
        List<String> words = new ArrayList<>();
        for (String w : ws)
        {
            words.add(w);
        }
        Map<String, List<FileWordPair>> wordOccurrences = countWordOccurrences(words, fs);

        // Print the word occurrences
        for (Map.Entry<String, List<FileWordPair>> entry : wordOccurrences.entrySet()) {
            String word = entry.getKey();
            List<FileWordPair> occurrences = entry.getValue();
            System.out.println("Word: " + word);
            for (FileWordPair fileOccurrence : occurrences) {
                System.out.println("File: " + fileOccurrence.fileName + ", Count: " + fileOccurrence.freq);
            }
        }
    }

    private static int countFilesInDirectory()
    {
        try (Stream<Path> files = Files.list(Paths.get(System.getProperty("user.dir") + "/SearchFiles/")))
        {
            return (int) files.count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Map<String, List<FileWordPair>> countWordOccurrences(List<String> words, List<String> filePaths)
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

                // Count occurrences of words in the text
                for (String word : words)
                {
                    int count = countWordOccurrencesInText(word, text);
                    List<FileWordPair> FileWordPair = wordOccurrences.getOrDefault(word, new ArrayList<>());
                    FileWordPair.add(new FileWordPair(filePath, count));
                    wordOccurrences.put(word, FileWordPair);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wordOccurrences;
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
        
    public static void rubbish(String path)
    {
        String query = "The File and Kindness are superstitious for arranging Pcs";

        List<String> f1 = new ArrayList<>();
        List<String> f2 = new ArrayList<>();
        f1.add(path+"f (1).txt");
        f1.add(path+"f (3).txt");
        f1.add(path+"f (5).txt");
        f1.add(path+"f (6).txt");

        f2.add(path+"f (2).txt");
        f2.add(path+"f (4).txt");
        f2.add(path+"f (7).txt");
        f2.add(path+"f (8).txt");


        Map<String,List<FileWordPair>> m1 = WordsCountingInFiles.countWordsInFiles(query , f1);
        Map<String,List<FileWordPair>> m2 = WordsCountingInFiles.countWordsInFiles(query , f2);


    }


    public static void testTreeMap()
    {
        TreeMap<String , Double> treeMap  = new TreeMap<>();
        treeMap.put("Key1", 10.5);
        treeMap.put("Key2", 5.2);
        treeMap.put("Key3", 15.7);
        treeMap.put("Key4", 7.8);
        for (Map.Entry<String,Double> entry : treeMap.entrySet())
        {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
    public static void doo()
    {
        String path = System.getProperty("user.dir") + "/SearchFiles/";
        ///listFilesForFolder(System.getProperty("user.dir") + "/SearchFiles");
        ///countWordsInFiles();
        //System.out.println(countFilesInDirectory());
        testTreeMap();

    }


}
