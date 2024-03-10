package org.example;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
//RMI service implementation
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class InvertedIndexServiceImpl extends UnicastRemoteObject implements InvertedIndexService {
 // A ForkJoinPool to handle concurrent tasks
 private ForkJoinPool pool;

 public InvertedIndexServiceImpl() throws RemoteException {
     super();
     // Initialize the pool with the number of available processors
     pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
 }

 @Override
 public Map<String, List<Integer>> getInvertedIndex(String fileName) throws RemoteException {
    
    //call the find file function to return the file contents as a string.
    String text = new getFileContents().getFileContent(fileName);
    
    // Split the text into lines
     String[] lines = text.split("\n");
     // Create a map to store the results
     Map<String, List<Integer>> index = new HashMap<>();
     // For each line, submit a task to the pool that computes the inverted index for that line
     for (int i=0;i<lines.length;i++) {
    	 String line = lines[i];
    	 int lineNumber=i;
    	 pool.submit(() -> {
        	 // Split the line into words and count their occurrences
             String[] words = line.split("\\s+");
             
             Map<String, List<Integer>> lineIndex = new HashMap<>();
             
             for (int j=0; j< words.length;j++) {
                 //extract the current word
            	 String word = words[j];
            	 //get its index list from the hash map.
            	 List <Integer> l1 = lineIndex.get(word);
                 //if new word, create the new list value.
            	 if (l1==null) {
                	 l1 = new ArrayList<>();
                 }
            	 //add the current index.
            	 l1.add(lineNumber+1);
            	 //update the hash map list value.
                 lineIndex.put(word, l1);
             }
             
             // Merge the line index with the global index
             synchronized (index) {
                 for (Map.Entry<String, List<Integer>> entry : lineIndex.entrySet()) {
                	 List<Integer> l_lineIndex = entry.getValue();
                	 List<Integer> l_index = index.get(entry.getKey());
                	 if (l_index == null) {
                		 l_index = new ArrayList<>();
                	 }
                	 //add to the list
                	 l_index.addAll(l_lineIndex);
                	 //sort for easy testing, as the threads run asynchronously
                     Collections.sort(l_index);
                	 index.put(entry.getKey(), l_index);
                 }
             }
         });
     }
     // Wait for all tasks to finish
     pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
     // Return the inverted index
     return index;
 }
 
 public static void main(String args[]) {
     try {

         //creating an instance of the interface implementation
         InvertedIndexServiceImpl server = new InvertedIndexServiceImpl();

         // this fixed: java.rmi.ConnectException: Connection refused to host: 127.0.0.1;
         LocateRegistry.createRegistry(8099);

         Naming.rebind("//155.248.230.141:8099/InvertedIndexService", server);
         System.out.println("InvertedIndexService ready...");
     } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
     }
 }
 
 
}
