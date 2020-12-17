package com.acertainbookstore.client.workloads;

import com.acertainbookstore.client.workloads.BookSetGenerator.RangeConfig;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;

/**
 * 
 * WorkloadConfiguration represents the configuration parameters to be used by
 * Workers class for running the workloads
 * 
 */
public class WorkloadConfiguration {
	private int numBooksToBuy = 5; //Used
	private int numBookCopiesToBuy = 1; //Used
	private int numEditorPicksToGet = 10; //Used
	private int numAddCopies = 10; //Used
	private int numBooksToAdd = 5; //Used
	private int numBooksWithLeastCopies = 5; //Used
	private int warmUpRuns = 100;
	private int numActualRuns = 500;
	private float percentRareStockManagerInteraction = 10f;
	private float percentFrequentStockManagerInteraction = 30f;
	private BookSetGenerator bookSetGenerator = null;
	private StockManager stockManager = null;
	private BookStore bookStore = null;

	public static final RangeConfig STRINGRANGE = new RangeConfig(25, 50);
	public static final RangeConfig COPIESRANGE = new RangeConfig(1, 25);
	public static final float EDITORBIAS = 0.1f; //~10% are true

	public WorkloadConfiguration(BookStore bookStore, StockManager stockManager) throws Exception {
		// Create a new one so that it is not shared
		bookSetGenerator = new BookSetGenerator(STRINGRANGE, COPIESRANGE, EDITORBIAS);
		this.bookStore = bookStore;
		this.stockManager = stockManager;
	}

	public int getNumBooksToBuy() {
		return numBooksToBuy;
	}

	public void setNumBooksToBuy(int numBooksToBuy) {
		this.numBooksToBuy = numBooksToBuy;
	}

	public int getNumBookCopiesToBuy() {
		return numBookCopiesToBuy;
	}

	public void setNumBookCopiesToBuy(int numBookCopiesToBuy) {
		this.numBookCopiesToBuy = numBookCopiesToBuy;
	}

	public int getNumBooksToAdd() {
		return numBooksToAdd;
	}

	public void setNumBooksToAdd(int numBooksToAdd) {
		this.numBooksToAdd = numBooksToAdd;
	}

	public int getNumBooksWithLeastCopies() {
		return numBooksWithLeastCopies;
	}

	public void setNumBooksWithLeastCopies(int numBooksWithLeastCopies) {
		this.numBooksWithLeastCopies = numBooksWithLeastCopies;
	}
	
	public StockManager getStockManager() {
		return stockManager;
	}

	public BookStore getBookStore() {
		return bookStore;
	}

	public void setStockManager(StockManager stockManager) {
		this.stockManager = stockManager;
	}

	public void setBookStore(BookStore bookStore) {
		this.bookStore = bookStore;
	}

	public float getPercentRareStockManagerInteraction() {
		return percentRareStockManagerInteraction;
	}

	public void setPercentRareStockManagerInteraction(
			float percentRareStockManagerInteraction) {
		this.percentRareStockManagerInteraction = percentRareStockManagerInteraction;
	}

	public float getPercentFrequentStockManagerInteraction() {
		return percentFrequentStockManagerInteraction;
	}

	public void setPercentFrequentStockManagerInteraction(
			float percentFrequentStockManagerInteraction) {
		this.percentFrequentStockManagerInteraction = percentFrequentStockManagerInteraction;
	}

	public int getWarmUpRuns() {
		return warmUpRuns;
	}

	public void setWarmUpRuns(int warmUpRuns) {
		this.warmUpRuns = warmUpRuns;
	}

	public int getNumActualRuns() {
		return numActualRuns;
	}

	public void setNumActualRuns(int numActualRuns) {
		this.numActualRuns = numActualRuns;
	}

	public int getNumEditorPicksToGet() {
		return numEditorPicksToGet;
	}

	public void setNumEditorPicksToGet(int numEditorPicksToGet) {
		this.numEditorPicksToGet = numEditorPicksToGet;
	}

	public int getNumAddCopies() {
		return numAddCopies;
	}

	public void setNumAddCopies(int numAddCopies) {
		this.numAddCopies = numAddCopies;
	}

	public BookSetGenerator getBookSetGenerator() {
		return bookSetGenerator;
	}

	public void setBookSetGenerator(BookSetGenerator bookSetGenerator) {
		this.bookSetGenerator = bookSetGenerator;
	}

}
