package com.acertainbookstore.client.workloads;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.acertainbookstore.business.ImmutableBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	private Random rnd;
	private final float spaceProb = 0.05f;
	private final float numProb = 0.10f;
	private final float upperAlpha = 0.35f;
	private final float lowerAlpha = 1.00f;
	private final int numOffset = 48;
	private final int upperOffset = 65;
	private final int lowerOffset = 97;

	private RangeConfig stringConfig, copiesConfig;
	private float editorBias;

	public static class RangeConfig {
		private int low, high;	
		public RangeConfig(int low, int high) {
			this.low = low;
			this.high = high;
		}

		public int GetLow() { return low; }
		public int GetHigh() { return high; }
		public int GetDiff() { return high-low; }
	}

	public BookSetGenerator(RangeConfig stringGen, RangeConfig numCopiesGen, float editorBias) {
		this.rnd = new Random();
		this.stringConfig = stringGen;
		this.copiesConfig = numCopiesGen;
		this.editorBias = editorBias;
	}

	//Utility to prevent making 100 temporary Random instances in other parts
	public int GetRandomInt(RangeConfig range) {
		return rnd.nextInt(range.GetDiff() + 1) + range.GetLow();
	}

	/**
	 * Generates title between the length of low and high
	 * @param low
	 * @param high
	 * @return
	 */
	private String GenerateString(RangeConfig range) {
		int low = range.GetLow();
		int diff = range.GetDiff();
		if (low < 0 && diff < 1)
			throw new InvalidParameterException("Expected positive values where high > low");
		
		int length = this.rnd.nextInt(diff + 1) + low;
		char[] outstr = new char[length];
		for (int i = 0; i < length; i++) {
			float decision = rnd.nextFloat();
			if (decision < spaceProb) {
				outstr[i] = ' ';
			} else if (decision < numProb) {
				int num = rnd.nextInt(10);
				outstr[i] = (char)(numOffset + num);
			} else if (decision < upperAlpha) {
				int alpha = rnd.nextInt(26);
				outstr[i] = (char)(upperOffset + alpha);
			} else if (decision < lowerAlpha) {
				int alpha = rnd.nextInt(26);
				outstr[i] = (char)(lowerOffset + alpha);
			}
		}
		return new String(outstr);
	}

	private int GenerateISBN() {
		return rnd.nextInt(Integer.MAX_VALUE);
	}

	private float GenerateReasonablePrice() {
		return rnd.nextFloat() * 50.0f;
	}

	private int GenerateCopies(RangeConfig range) {
		return GetRandomInt(range);
	}

	private boolean GenerateEditor(float trueBias) {
		return rnd.nextFloat() < editorBias;
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		if (isbns.size() <= num)
			return isbns;
		
		Set<Integer> sample = new HashSet<Integer>();
		//Ensure ordering and indexability by making a list
		List<Integer> subtractSet = new ArrayList<Integer>(isbns);

		while (sample.size() < num) {
			int index = rnd.nextInt(subtractSet.size());
			Integer isbn = subtractSet.get(index);
			sample.add(isbn);
			subtractSet.remove(index);
		}

		return sample;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		if (num < 1) {
			throw new InvalidParameterException("Expected a positive number as input");
		}

		Set<StockBook> books = new HashSet<StockBook>();
		while (books.size() < num) {
			StockBook book = new ImmutableStockBook(
				GenerateISBN(), 
				GenerateString(stringConfig), 
				GenerateString(stringConfig),
				GenerateReasonablePrice(),
				GenerateCopies(copiesConfig),
				0,
				0,
				0,
				GenerateEditor(editorBias)
			);
			//Ensure uniqueness from generator
			if (books.stream().allMatch(b -> b.getISBN() != book.getISBN())) {
				books.add(book);
			}
		}
		return books;
	}
}
