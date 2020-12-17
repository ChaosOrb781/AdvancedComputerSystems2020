/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	private static int UniqueBooks = 1000;
	private static String serverAddress = "http://localhost:8081";
	private static int[] threadsToTest = new int[] {1,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32};
	private static boolean localTest = true;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		for (int num : threadsToTest) {
			System.out.printf("Doing %d threads...\n", num);
			runTest(num);
			System.out.printf("Done with %d threads\n", num);
		}
	}

	public static void runTest(int numConcurrentWorkloadThreads) throws Exception {
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults, numConcurrentWorkloadThreads);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults, int numberOfThreads) throws IOException {
		// TODO: You should aggregate metrics and output them for plotting here
		Path currentPath = Paths.get("").toAbsolutePath();
		String batchName = "BatchResults" + (localTest ? "Local" : "Server") + ".csv";
		File batchFile = Paths.get(currentPath.toString(), batchName).toFile();
		FileWriter fw = null;
		if (!batchFile.exists()) {
			batchFile.createNewFile();
			fw = new FileWriter(batchFile, true);
			fw.append("batchid,date,successfulInteractions,elapsedTimeInNanoSecs,totalRuns,successfulFequentBookStoreInterationRuns,totalFrequentBookStoreInteractionRuns\n");
		} else {
			fw = new FileWriter(batchFile, true);
		}
		Random rnd = new Random();
		int batch = rnd.nextInt();
		for (WorkerRunResult wrr : workerRunResults) {
			fw.append(batch + "," + wrr.toCSV());
		}
		fw.close();

		String metricName = "MetricResults" + (localTest ? "Local" : "Server") + ".csv";
		File metricFile = Paths.get(currentPath.toString(), metricName).toFile();
		if (!metricFile.exists()) {
			metricFile.createNewFile();
			fw = new FileWriter(metricFile, true);
			fw.append("batchid,date,initialBookCount,numberOfThreads,frequentPercent,totalTime,avgSuccessRate,aggThroughput,avgThroughput,stdThroughput,aggGoodput,avgGoodput,stdGoodput,aggDiffput,avgDiffput,stdDiffput,aggLatency,avgLatency,stdLatency\n");
		} else {
			fw = new FileWriter(metricFile, true);
		}

		double frequentPercent = 0.0;
		double totalTime = 0.0;
		double avgSuccessRate = 0.0;

		double aggThroughput = 0.0;
		double avgThroughput = 0.0;
		double stdThroughput = 0.0;
		double aggGoodput = 0.0;
		double avgGoodput = 0.0;
		double stdGoodput = 0.0;
		double aggLatency = 0.0;
		double avgLatency = 0.0;
		double stdLatency = 0.0;


		for (WorkerRunResult wrr : workerRunResults) {
			frequentPercent += (double)wrr.getTotalFrequentBookStoreInteractionRuns() / wrr.getTotalRuns();
			double time = (double)wrr.getElapsedTimeInNanoSecs() / 1000000;
			totalTime += time;
			avgSuccessRate += (double)wrr.getSuccessfulFrequentBookStoreInteractionRuns() / wrr.getTotalFrequentBookStoreInteractionRuns();

			aggThroughput += (double)wrr.getTotalFrequentBookStoreInteractionRuns() / time;
			aggGoodput += (double)wrr.getSuccessfulFrequentBookStoreInteractionRuns() / time;
			aggLatency += (double)time / wrr.getTotalFrequentBookStoreInteractionRuns();
		}

		frequentPercent /= workerRunResults.size();
		avgSuccessRate /= workerRunResults.size();
		avgThroughput = aggThroughput / workerRunResults.size();
		avgGoodput = aggGoodput / workerRunResults.size();
		avgLatency = aggLatency / workerRunResults.size();

		for (WorkerRunResult wrr : workerRunResults) {
			double time = (double)wrr.getElapsedTimeInNanoSecs() / 1000000;
			
			double diffThroughput = (double)wrr.getTotalFrequentBookStoreInteractionRuns() / time - avgThroughput;
			stdThroughput += diffThroughput * diffThroughput;
			double diffGoodput = (double)wrr.getSuccessfulFrequentBookStoreInteractionRuns() / time - avgGoodput;
			stdGoodput += diffGoodput * diffGoodput;
			double diffLatency = (double)time / wrr.getTotalFrequentBookStoreInteractionRuns() - avgLatency;
			stdLatency += diffLatency * diffLatency;
		}

		//Note: it is the sample variance
		stdThroughput /= workerRunResults.size() > 1 ? (workerRunResults.size() - 1) : 1;
		stdThroughput = Math.sqrt(stdThroughput);
		stdGoodput /= workerRunResults.size() > 1 ? (workerRunResults.size() - 1) : 1;
		stdGoodput = Math.sqrt(stdGoodput);
		stdLatency /= workerRunResults.size() > 1 ? (workerRunResults.size() - 1) : 1;
		stdLatency = Math.sqrt(stdLatency);

		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-DD-HH-MM-SS"));
		StringBuilder sb = new StringBuilder();
		sb.append(batch).append(',');
		sb.append(date).append(',');
		sb.append(UniqueBooks).append(',');
		sb.append(numberOfThreads).append(',');
		sb.append(frequentPercent * 100).append(',');
		sb.append(totalTime).append(',');
		sb.append(avgSuccessRate * 100).append(',');
		sb.append(aggThroughput).append(',');
		sb.append(avgThroughput).append(',');
		sb.append(stdThroughput).append(',');
		sb.append(aggGoodput).append(',');
		sb.append(avgGoodput).append(',');
		sb.append(stdGoodput).append(',');
		sb.append(aggThroughput - aggGoodput).append(',');
		sb.append(avgThroughput - avgGoodput).append(',');
		sb.append(stdThroughput - stdGoodput).append(',');
		sb.append(aggLatency).append(',');
		sb.append(avgLatency).append(',');
		sb.append(stdLatency).append('\n');

		fw.append(sb.toString());
		fw.close();
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager) throws BookStoreException {
		//Create temporary bookset generator
		BookSetGenerator bsg = new BookSetGenerator(WorkloadConfiguration.STRINGRANGE, WorkloadConfiguration.COPIESRANGE, WorkloadConfiguration.EDITORBIAS);
		Set<com.acertainbookstore.business.StockBook> books = bsg.nextSetOfStockBooks(UniqueBooks);
		stockManager.addBooks(books);
		//Run query on the bookstore to see if the books were added to the stock correctly
		bookStore.getBooks(books.stream().map(book -> book.getISBN()).collect(Collectors.toSet()));
	}
}
