package com.acertainbookstore.business;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Random;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/** {@link TwoLevelLockingConcurrentCertainBookStore} implements the {@link BookStore} and
 * {@link StockManager} functionalities.
 * 
 * @see BookStore
 * @see StockManager
 */
public class TwoLevelLockingConcurrentCertainBookStore implements BookStore, StockManager {

	private ReadWriteLock globalLock;
	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private Map<Integer, SimpleEntry<ReadWriteLock, BookStoreBook>> bookMap = null;

	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public TwoLevelLockingConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<>();
		globalLock = new ReentrantReadWriteLock();
	}

	private SimpleEntry<ReadWriteLock, BookStoreBook> BundleBookAndLock(BookStoreBook book) {
		return new SimpleEntry<>(new ReentrantReadWriteLock(), book);
	}

	private void ReadLockByISBN(Integer isbn) {
		if (bookMap.containsKey(isbn))
			bookMap.get(isbn).getKey().readLock().lock();
	}

	private void ReadUnlockByISBN(Integer isbn) {
		if (bookMap.containsKey(isbn))
		{
			//Unlocking should be inconsequential, therefore report if unlocking non-existing locks, but otherwise continue
			try {
				bookMap.get(isbn).getKey().readLock().unlock();
			} catch (IllegalMonitorStateException ex) {
				System.out.println("Warning: " + Thread.currentThread().getName() + " attempted to unlock a lock it did not hold.");
			}
		}
	}

	private void ReadUnlockByISBN(Iterable<Integer> isbns) {
		isbns.forEach(isbn -> ReadUnlockByISBN(isbn));
	}

	private void ReadLockAll() {
		bookMap.values().forEach(entry -> entry.getKey().readLock().lock());
	}

	private void ReadUnlockAll() {
		bookMap.values().forEach(entry -> {
			//Unlocking should be inconsequential, therefore report if unlocking non-existing locks, but otherwise continue
			try {
				entry.getKey().readLock().unlock();
			} catch (IllegalMonitorStateException ex) {
				System.out.println("Warning: " + Thread.currentThread().getName() + " attempted to unlock a lock it did not hold.");
			}	
		});
	}

	private void WriteLockByISBN(Integer isbn) {
		if (bookMap.containsKey(isbn))
			bookMap.get(isbn).getKey().writeLock().lock();
	}

	private void WriteUnlockByISBN(Integer isbn) {
		if (bookMap.containsKey(isbn)) {
			//Unlocking should be inconsequential, therefore report if unlocking non-existing locks, but otherwise continue
			try {
				bookMap.get(isbn).getKey().writeLock().unlock();
			} catch (IllegalMonitorStateException ex) {
				System.out.println("Warning: " + Thread.currentThread().getName() + " attempted to unlock a lock it did not hold.");
			}
		}
	}

	private void WriteUnlockByISBN(Iterable<Integer> isbns) {
		isbns.forEach(isbn -> WriteUnlockByISBN(isbn));
	}

	@SuppressWarnings("unused")
	private void WriteLockAll() {
		bookMap.values().forEach(entry -> entry.getKey().writeLock().lock());
	}

	@SuppressWarnings("unused")
	private void WriteUnlockAll() {
		bookMap.values().forEach(entry -> {
			try {
				entry.getKey().writeLock().unlock();
			} catch (IllegalMonitorStateException ex) {
				System.out.println("Warning: " + Thread.currentThread().getName() + " attempted to unlock a lock it did not hold.");
			}	
		});
	}

	private void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();

		if (BookStoreUtility.isInvalidISBN(isbn)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookTitle)) { // Check if the book has valid title
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookAuthor)) { // Check if the book has valid author
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isInvalidNoCopies(noCopies)) { // Check if the book has at least one copy
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookPrice < 0.0) { // Check if the price of the book is valid
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookMap.containsKey(isbn)) {// Check if the book is not in stock
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.DUPLICATED);
		}
	}	
	
	private void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

		if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
			throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
		}
	}
	
	private void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
	}
	
	private void validateISBNInStock(Integer ISBN) throws BookStoreException {
		if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
		}
		if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addBooks(java.util.Set)
	 */
	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		BookStoreException exception = null;

		//Already correct from singlelock, as it inserts elements
		globalLock.writeLock().lock();
		try {
			// Check if all are there
			for (StockBook book : bookSet) {
				validate(book);
			}

			for (StockBook book : bookSet) {
				int isbn = book.getISBN();
				bookMap.put(isbn, BundleBookAndLock(new BookStoreBook(book)));
			}
		} catch (BookStoreException ex) {
			exception = ex;
		// Finally is always called after either try has finished or the catch
		} finally {
			globalLock.writeLock().unlock();
		}
		if (exception != null)
			throw exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addCopies(java.util.Set)
	 */
	public void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		int isbn;
		int numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		BookStoreException exception = null;

		//Works as intension lock, this mutates entries, therefore do not exclusive lock
		globalLock.readLock().lock();
		try {
			for (BookCopy bookCopy : bookCopiesSet) {
				//As it is a set, the same thread cannot lock the same isbn.
				WriteLockByISBN(bookCopy.getISBN());
				validate(bookCopy);
			}
			
			// Update the number of copies
			for (BookCopy bookCopy : bookCopiesSet) {
				isbn = bookCopy.getISBN();
				numCopies = bookCopy.getNumCopies();
				bookMap.get(isbn).getValue().addCopies(numCopies);
			}
		} catch (BookStoreException ex) {
			exception = ex;
		// Finally is always called after either try has finished or the catch
		} finally {
			WriteUnlockByISBN(bookCopiesSet.stream().map(bookCopy -> bookCopy.getISBN()).collect(Collectors.toList()));
			globalLock.readLock().unlock();
		}
		if (exception != null)
			throw exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public List<StockBook> getBooks() {

		List<StockBook> returnVal;

		//Safe for reading
		globalLock.readLock().lock();
		try {
			ReadLockAll();
			returnVal = 
				bookMap.values().stream()
					.map(book -> book.getValue().immutableStockBook())
					.collect(Collectors.toList());
		} finally {
			ReadUnlockAll();
			globalLock.readLock().unlock();
		}
		return returnVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#updateEditorPicks(java.util
	 * .Set)
	 */
	public void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		BookStoreException exception = null;

		//Requires write lock as it mutates a value, validation step needs to be consistant
		//when we get to the write step
		globalLock.readLock().lock();
		try {
			for (BookEditorPick editorPickArg : editorPicks) {
				WriteLockByISBN(editorPickArg.getISBN());
				validate(editorPickArg);
			}
			
			for (BookEditorPick editorPickArg : editorPicks) {
				bookMap.get(editorPickArg.getISBN()).getValue().setEditorPick(editorPickArg.isEditorPick());
			}
		} catch (BookStoreException ex) {
			exception = ex;
		} finally {
			WriteUnlockByISBN(editorPicks.stream().map(bookEditorPick -> bookEditorPick.getISBN()).collect(Collectors.toList()));
			globalLock.readLock().unlock();
		}
		if (exception != null)
			throw exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#buyBooks(java.util.Set)
	 */
	public void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all ISBNs that we buy are there first.
		int isbn;
		BookStoreBook book;
		Boolean saleMiss = false;

		Map<Integer, Integer> salesMisses = new HashMap<>();

		BookStoreException exception = null;

		//Intension lock as it only mutates existing books
		globalLock.readLock().lock();
		try {
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				isbn = bookCopyToBuy.getISBN();
				
				WriteLockByISBN(isbn);
				validate(bookCopyToBuy);
				
				book = bookMap.get(isbn).getValue();
				
				if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
					// If we cannot sell the copies of the book, it is a miss.
					salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
					saleMiss = true;
				}
			}
			
			// We throw exception now since we want to see how many books in the
			// order incurred misses which is used by books in demand
			if (saleMiss) {
				for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
					book = bookMap.get(saleMissEntry.getKey()).getValue();
					book.addSaleMiss(saleMissEntry.getValue());
				}
				throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
			}
			
			// Then make the purchase.
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				book = bookMap.get(bookCopyToBuy.getISBN()).getValue();
				book.buyCopies(bookCopyToBuy.getNumCopies());
			}
		} catch (BookStoreException ex) {
			exception = ex;
		} finally {
			WriteUnlockByISBN(bookCopiesToBuy.stream().map(bookCopy -> bookCopy.getISBN()).collect(Collectors.toList()));
			globalLock.readLock().unlock();
		}
		if (exception != null) 
			throw exception;
	}
		
		/*
		* (non-Javadoc)
		* 
		* @see
		* com.acertainbookstore.interfaces.StockManager#getBooksByISBN(java.util.
		* Set)
	 */
	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		List<StockBook> returnVal = null;
		BookStoreException exception = null;

		//Safe for reading
		globalLock.readLock().lock();
		try {
			for (Integer ISBN : isbnSet) {
				ReadLockByISBN(ISBN);
				validateISBNInStock(ISBN);
			}

			returnVal = 
				isbnSet.stream()
					.map(isbn -> bookMap.get(isbn).getValue().immutableStockBook())
					.collect(Collectors.toList());
		} catch (BookStoreException ex) {
			exception = ex;
		} finally {
			ReadUnlockByISBN(isbnSet);
			globalLock.readLock().unlock();
		}
		if (exception != null)
			throw exception;
		else
			return returnVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getBooks(java.util.Set)
	 */
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		List<Book> returnVal = null;
		BookStoreException exception = null;

		//Safe for reading
		globalLock.readLock().lock();
		try {
			// Check that all ISBNs that we rate are there to start with.
			for (Integer ISBN : isbnSet) {
				ReadLockByISBN(ISBN);
				validateISBNInStock(ISBN);
			}
			
			returnVal = 
				isbnSet.stream()
					.map(isbn -> bookMap.get(isbn).getValue().immutableBook())
					.collect(Collectors.toList());
		} catch (BookStoreException ex) {
			exception = ex;
		} finally {
			ReadUnlockByISBN(isbnSet);
			globalLock.readLock().unlock();
		}
		if (exception != null)
			throw exception;
		else
			return returnVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getEditorPicks(int)
	 */
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		List<BookStoreBook> listAllEditorPicks;

		//Only need readlock when initially fetching
		//Unconcerned for the data to have changed upon return
		globalLock.readLock().lock();
		ReadLockAll();
		try {
			listAllEditorPicks = 
				bookMap.entrySet().stream()
					.map(pair -> pair.getValue().getValue()) //Lol
					.filter(book -> book.isEditorPick())
					.collect(Collectors.toList());
		} finally {
			ReadUnlockAll();
			globalLock.readLock().unlock();
		}

		// Find numBooks random indices of books that will be picked.
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<>();
		int rangePicks = listAllEditorPicks.size();

		if (rangePicks <= numBooks) {

			// We need to add all books.
			for (int i = 0; i < listAllEditorPicks.size(); i++) {
				tobePicked.add(i);
			}
		} else {

			// We need to pick randomly the books that need to be returned.
			int randNum;

			while (tobePicked.size() < numBooks) {
				randNum = rand.nextInt(rangePicks);
				tobePicked.add(randNum);
			}
		}

        // Return all the books by the randomly chosen indices.
        return tobePicked.stream()
                .map(index -> listAllEditorPicks.get(index).immutableBook())
                .collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getTopRatedBooks(int)
	 */
	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		if(numBooks <= 0) throw new BookStoreException(BookStoreConstants.NULL_INPUT);

		List<Book> returnVal = null;

		globalLock.readLock().lock();
		ReadLockAll();
		try {
			returnVal = 
				bookMap.values().stream()
					.map(entry -> entry.getValue())
					.sequential()
					.sorted((book1, book2) -> Float.compare(book2.getAverageRating(), book1.getAverageRating()))
					.map(book -> book.immutableStockBook())
					.limit(numBooks)
					.collect(Collectors.toList());
		} finally {
			ReadUnlockAll();
			globalLock.readLock().unlock();
		}
		return returnVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksInDemand()
	 */
	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		List<StockBook> returnVal = null;

		globalLock.readLock().lock();
		ReadLockAll();
		try {
			//Filter any non-zero sale miss books and return the ones which had missed sales
			returnVal = 
				bookMap.values().stream()
					.map(entry -> entry.getValue())
					.filter(book -> book.getNumSaleMisses() > 0)
					.map(book -> book.immutableStockBook())
					.collect(Collectors.toList());
		} finally {
			ReadUnlockAll();
			globalLock.readLock().unlock();
		}
		return returnVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#rateBooks(java.util.Set)
	 */
	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		if(bookRating == null) {
            throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		
		BookStoreException exception = null;

		globalLock.readLock().lock();
		try {
			// Check whether all books are in the collection.
			for (BookRating bookRate : bookRating) {
				WriteLockByISBN(bookRate.getISBN());
				validateISBNInStock(bookRate.getISBN());
				if (bookRate.getRating() < 0 || bookRate.getRating() > 5) {
					throw new BookStoreException("Invalid rating provided, expected between 0 and 5");
				}
			}

			// If all books validated, then perform the ratings (all-or-nothing)
			for (BookRating bookRate : bookRating) {
				BookStoreBook book = bookMap.get(bookRate.getISBN()).getValue();
				book.addRating(bookRate.getRating());
			}
		} catch (BookStoreException ex) {
			exception = ex;
		} finally {
			WriteUnlockByISBN(bookRating.stream().map(bookRate -> bookRate.getISBN()).collect(Collectors.toList()));
			globalLock.readLock().unlock();
		}
		if (exception != null)
			throw exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeAllBooks()
	 */
	public void removeAllBooks() throws BookStoreException {
		//Already fine as all intention locks has to be freed
		globalLock.writeLock().lock();
		try {
			bookMap.clear();
		} finally {
			globalLock.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#removeBooks(java.util.Set)
	 */
	public void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		BookStoreException exception = null;

		//Already fine as it requires all intention to locks be gone
		globalLock.writeLock().lock();
		try {
			for (Integer ISBN : isbnSet) {
				if (BookStoreUtility.isInvalidISBN(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
				}
				
				if (!bookMap.containsKey(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
				}
			}
			
			for (int isbn : isbnSet) {
				bookMap.remove(isbn);
			}
		} catch (BookStoreException ex) {
			exception = ex;
		} finally {
			globalLock.writeLock().unlock();
		}
		if (exception != null)
			throw exception;
	}
}
