package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.AbstractMap.SimpleEntry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = false;

	/** Single lock test */
	private static boolean singleLock = false;

	/** Concurrency test variables */
	private static Integer numberOfOperations = 1000;
	
	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;
			
			String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
			singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

			if (localTest) {
				if (singleLock) {
					SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				} else {
					TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				}
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	private List<Integer> knownISBNs = new ArrayList<Integer>();
	private StockBook instanciateNewBook(String title, String author, float price, int numCopies, boolean editorsPick) {
		Random rnd = new Random();
		int randomISBN = rnd.nextInt(10000000);
		while (knownISBNs.contains(randomISBN)) {
			randomISBN = rnd.nextInt(10000000);
		}
		return new ImmutableStockBook(randomISBN, title, author, price, numCopies, 0, 0, 0, editorsPick);
	}

	//Add 11 books, 4 of which are editors choice
	public List<StockBook> getDefaultBooks() {
		List<StockBook> booklist = new ArrayList<StockBook>();
		booklist.add(getDefaultBook());
		
		booklist.add(instanciateNewBook("House of Leaves", "Mark Danielewski", 15, 5, false));
		booklist.add(instanciateNewBook("The Great Gatsby", "F.S.Fitzgerald", 5, 4, true));
		booklist.add(instanciateNewBook("Invisible Man", "Some One", 20, 5, true));
		booklist.add(instanciateNewBook("Alice in Wonderland", "Jacob Smith", 25, 8, false));
		booklist.add(instanciateNewBook("The Color Purple", "Mark Daniel.", 15, 5, true));

		booklist.add(instanciateNewBook("Ulysses", "James Joyce", 17, 3, false));
		booklist.add(instanciateNewBook("1984", "George Orwell", 8, 4, false));
		booklist.add(instanciateNewBook("The Stranger", "Some One", 22, 8, true));
		booklist.add(instanciateNewBook("Among Us", "Jacob the Great", 40, 1, false));
		booklist.add(instanciateNewBook("It's raining men", "The Weather Girls", 3, 9, false));
		
		return booklist;
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.addAll(getDefaultBooks());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		//Reset to only having default book, due to our modified initial state
		storeManager.removeAllBooks();
		Set<StockBook> tmp = new HashSet<StockBook>();
		tmp.add(getDefaultBook());
		storeManager.addBooks(tmp);

		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		//Reset to only having default book, due to our modified initial state
		storeManager.removeAllBooks();
		Set<StockBook> tmp = new HashSet<StockBook>();
		tmp.add(getDefaultBook());
		storeManager.addBooks(tmp);

		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/** ADDITIONAL TESTING CONDUCTED */

	/**
	 * Test the rate books functionality
	 * given an invalid ISBN, test if a book cannot be rated 
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */

	
	@Test
	public void testRateBooksInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 5));
		booksToRate.add(new BookRating(TEST_ISBN - 1, 4));


		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		for (int i = 0; i < booksInStorePostTest.size(); i++) {
			StockBook prebook = booksInStorePreTest.get(i);
			StockBook postbook = booksInStorePostTest.get(i);
			assertTrue(prebook.getISBN() == postbook.getISBN() 
			        && prebook.getTitle().equals(postbook.getTitle())
					&& prebook.getAuthor().equals(postbook.getAuthor()) 
					&& prebook.getPrice() == postbook.getPrice()
					&& prebook.getNumSaleMisses() == postbook.getNumSaleMisses()
					&& prebook.getAverageRating() == postbook.getAverageRating()
					&& prebook.getNumTimesRated() == postbook.getNumTimesRated()
					&& prebook.getTotalRating() == postbook.getTotalRating()
					&& prebook.isEditorPick() == postbook.isEditorPick());
		}
	}



	/**
	 * Test the rate books functionality
	 * given a negative rating for a book that is in the collection, tests if a book cannot be rated 
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */

	
	@Test
	public void testRateBooksInvalidRating() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		Random rnd = new Random();
		for (int i = 0; i < booksInStorePreTest.size() - 1; i++) {
			booksToRate.add(new BookRating(booksInStorePreTest.get(i).getISBN(), rnd.nextInt(6)));
		}
		int lastIndex = booksInStorePreTest.size() - 1;
		booksToRate.add(new BookRating(booksInStorePreTest.get(lastIndex).getISBN(), -1));


		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		for (int i = 0; i < booksInStorePostTest.size(); i++) {
			StockBook prebook = booksInStorePreTest.get(i);
			StockBook postbook = booksInStorePostTest.get(i);
			assertTrue(prebook.getISBN() == postbook.getISBN() 
			        && prebook.getTitle().equals(postbook.getTitle())
					&& prebook.getAuthor().equals(postbook.getAuthor()) 
					&& prebook.getPrice() == postbook.getPrice()
					&& prebook.getNumSaleMisses() == postbook.getNumSaleMisses()
					&& prebook.getAverageRating() == postbook.getAverageRating()
					&& prebook.getNumTimesRated() == postbook.getNumTimesRated()
					&& prebook.getTotalRating() == postbook.getTotalRating()
					&& prebook.isEditorPick() == postbook.isEditorPick());
		}
	}

	/**
	 * Test the rate books functionality
	 * given valid list of ratings for a book that is in the collection, 
	 * tests if a book can be rated, as well as the correctness of the avg. rating.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */

	@Test
	public void testRateBooksThoroughly() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		Random rnd = new Random();
		for (int i = 0; i < booksInStorePreTest.size() - 1; i++) {
			booksToRate.add(new BookRating(booksInStorePreTest.get(i).getISBN(), rnd.nextInt(5)+1));
		}
		int lastIndex = booksInStorePreTest.size() - 1;

		int avg1 = rnd.nextInt(5)+1;
		BookRating last = new BookRating(booksInStorePreTest.get(lastIndex).getISBN(), avg1);
		booksToRate.add(last); //rate the last book twice for the avg. calc.
		client.rateBooks(booksToRate);

		int avg2 = rnd.nextInt(5)+1;
		booksToRate.remove(last);
		booksToRate.add(new BookRating(booksInStorePreTest.get(lastIndex).getISBN(), avg2));
		client.rateBooks(booksToRate);

		//Add the valid ratings, test the mean rating
		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		StockBook lastPre = booksInStorePreTest.get(booksInStorePostTest.size()-1);
		StockBook lastPost = booksInStorePostTest.get(booksInStorePostTest.size()-1);

		assertTrue (lastPost.getAverageRating() == (int) (avg1+avg2) / 2.0f
				&& lastPre.getNumTimesRated()+2 == lastPost.getNumTimesRated()
				&& lastPre.getISBN() == lastPost.getISBN() 
				&& lastPre.getTitle().equals(lastPost.getTitle())
				&& lastPre.getAuthor().equals(lastPost.getAuthor()) 
				&& lastPre.getPrice() == lastPost.getPrice()
				&& lastPre.getNumSaleMisses() == lastPost.getNumSaleMisses()
				&& lastPre.getTotalRating() == lastPost.getTotalRating()-(avg1+avg2)
				&& lastPre.isEditorPick() == lastPost.isEditorPick()); //assert average rating validity
		
		for (int i = 0; i < booksInStorePostTest.size()-1; i++) {
			StockBook prebook = booksInStorePreTest.get(i);
			StockBook postbook = booksInStorePostTest.get(i);
			System.out.println("Hey");
			System.out.println(postbook.getTotalRating());
			assertTrue(prebook.getISBN() == postbook.getISBN() 
			        && prebook.getTitle().equals(postbook.getTitle())
					&& prebook.getAuthor().equals(postbook.getAuthor()) 
					&& prebook.getPrice() == postbook.getPrice()
					&& prebook.getNumSaleMisses() == postbook.getNumSaleMisses()
					&& prebook.getAverageRating() != postbook.getAverageRating()
					&& prebook.getNumTimesRated()+2 == postbook.getNumTimesRated()
					&& prebook.getTotalRating() <= postbook.getTotalRating()
					&& prebook.isEditorPick() == postbook.isEditorPick());
		}
	}

	/**
	 * Test get K-top rated books with a valid K
	 *
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedValid() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		int KBooks = 5;

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		Random rnd = new Random();
		//add a random rating to the books 1-4
		for (int i = 0; i < booksInStorePreTest.size()-1; i++) {
			booksToRate.add(new BookRating(booksInStorePreTest.get(i).getISBN(), rnd.nextInt(4)+1));
		}
		int lastIndex = booksInStorePreTest.size()-1;
		int lastISBN = booksInStorePreTest.get(lastIndex).getISBN();
		booksToRate.add(new BookRating(lastISBN, 5)); //rate the last book as the highest rated book
		//System.out.println(booksToRate);
		client.rateBooks(booksToRate);
		try {
			Thread.sleep(500);
		}
		catch (Exception ex) {
			;
		}
		List<Book> list = client.getTopRatedBooks(KBooks);

		//System.out.println(list);
		//System.out.println(booksInStorePreTest.get(lastIndex).getTotalRating());
		assertTrue(booksInStorePreTest.containsAll(list)
					&& list.size() == KBooks
					&& list.contains(booksInStorePreTest.get(lastIndex)));
	}

	/**
	 * Test get K-top rated books with an invalid K
	 *
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedInvalid() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		int KBooks = -5;

		HashSet<BookRating> booksToRate = new HashSet<BookRating>();
		Random rnd = new Random();
		//add a random rating to the books
		for (int i = 0; i < booksInStorePreTest.size(); i++) {
			booksToRate.add(new BookRating(booksInStorePreTest.get(i).getISBN(), rnd.nextInt(5)+1));
		}
		client.rateBooks(booksToRate);
		
		try {
			client.getTopRatedBooks(KBooks);
			fail();
		} catch (BookStoreException ex) {
			;
		}
	}

	@Test
	public void testBuyThroughly() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Set of books to buy, for now we buy one of each book
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		for (int i = 0; i < booksInStorePreTest.size(); i++) {
			booksToBuy.add(new BookCopy(booksInStorePreTest.get(i).getISBN(), 1));
		}

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Test to see if all books now have one less in the post book instance
		for (int i = 0; i < booksInStorePostTest.size(); i++) {
			StockBook prebook = booksInStorePreTest.get(i);
			StockBook postbook = booksInStorePostTest.get(i);
			assertTrue(prebook.getISBN() == postbook.getISBN() 
			        && prebook.getTitle().equals(postbook.getTitle())
					&& prebook.getAuthor().equals(postbook.getAuthor()) 
					&& prebook.getPrice() == postbook.getPrice()
					&& prebook.getNumCopies() == postbook.getNumCopies() + 1
					&& prebook.getNumSaleMisses() == postbook.getNumSaleMisses()
					&& prebook.getAverageRating() == postbook.getAverageRating()
					&& prebook.getNumTimesRated() == postbook.getNumTimesRated()
					&& prebook.getTotalRating() == postbook.getTotalRating()
					&& prebook.isEditorPick() == postbook.isEditorPick());
		}

		//Check if "Among us" is now empty
		Optional<StockBook> amongus = booksInStorePostTest.stream().filter(book -> book.getTitle().equals("Among Us")).findFirst();
		assertTrue(amongus.isPresent());
		assertTrue(amongus.get().getNumCopies() == 0);

		booksInStorePreTest = storeManager.getBooks();

		//Try to buy another copy of "Among us"
		try {
			Set<BookCopy> tmp = new HashSet<BookCopy>();
			tmp.add(new BookCopy(amongus.get().getISBN(), 1));
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		booksInStorePostTest = storeManager.getBooks();
		
		// Unchanged as we had an error
		for (int i = 0; i < booksInStorePostTest.size(); i++) {
			StockBook prebook = booksInStorePreTest.get(i);
			StockBook postbook = booksInStorePostTest.get(i);
			if (prebook.getTitle().equals("Among Us")) {
				assertTrue(prebook.getNumSaleMisses() == postbook.getNumSaleMisses() - 1);
			} else {
				assertTrue(prebook.getNumSaleMisses() == postbook.getNumSaleMisses());
			}
			assertTrue(prebook.getISBN() == postbook.getISBN() 
			        && prebook.getTitle().equals(postbook.getTitle())
					&& prebook.getAuthor().equals(postbook.getAuthor()) 
					&& prebook.getPrice() == postbook.getPrice()
					&& prebook.getNumCopies() == postbook.getNumCopies()
					&& prebook.getAverageRating() == postbook.getAverageRating()
					&& prebook.getNumTimesRated() == postbook.getNumTimesRated()
					&& prebook.getTotalRating() == postbook.getTotalRating()
					&& prebook.isEditorPick() == postbook.isEditorPick());
		}

	}

	@Test
	public void testEditorsPickThroughly() throws BookStoreException {
		// Check if there is non in the current set of default books added to the storeManager
		List<Book> defaulteditorspicks = client.getEditorPicks(10);

		// All the titles of the editor picks books from the default set
		List<String> editorPicksTitles = new ArrayList<String>();
		editorPicksTitles.add("The Stranger");
		editorPicksTitles.add("The Color Purple");
		editorPicksTitles.add("Invisible Man");
		editorPicksTitles.add("The Great Gatsby");

		//Check if all the editor picks books are in the list from the bookstore
		assertTrue(defaulteditorspicks.stream()
					.allMatch(x -> editorPicksTitles.contains(x.getTitle())));

		//Test with K = 1,2,3,4:
		for (int K = 1; K <= 4; K++) {
			defaulteditorspicks = client.getEditorPicks(K);
			//Check expected amount returned
			assertTrue(defaulteditorspicks.size() == K);
			//Check to see if they are in editor set, as it is randomly picked
			//then no ordering can be predicted
			assertTrue(defaulteditorspicks.stream()
					.allMatch(x -> editorPicksTitles.contains(x.getTitle())));
		}

		Random rnd = new Random();

		//Cannot fetch negative features
		for (int i = 0; i < 1000; i++) {
			//Get some random negative K (add one to not have == 0)
			int K = -(rnd.nextInt(1000000) + 1);
			try {
				client.getEditorPicks(K);
				fail();
			} catch (BookStoreException ex) {
				;
			}
		}

		//Zero editor picks books should be valid
		defaulteditorspicks = client.getEditorPicks(0);
		assertTrue(defaulteditorspicks.size() == 0);

		//Remove all books, then check if we can get any
		storeManager.removeAllBooks();
		defaulteditorspicks = client.getEditorPicks(10);
		assertTrue(defaulteditorspicks.size() == 0);
	}

	@Test
	public void testClientsBuyAndRestock() throws BookStoreException {
		//Add enough stock so first client buying cannot run out of books to buy
		storeManager.addCopies(
			storeManager.getBooks().stream()
				.map(sb -> new BookCopy(sb.getISBN(), numberOfOperations)).collect(Collectors.toSet()));

		Random rnd = new Random(); 

		List<StockBook> preBooks = storeManager.getBooks();
		int buy = rnd.nextInt(numberOfOperations)+1;

		Set<BookCopy> copy = new HashSet<BookCopy>();
		for(int i = 0; i< numberOfOperations; i++)
		{
			StockBook randomBook = preBooks.get(rnd.nextInt(preBooks.size()));
			copy.add(new BookCopy(randomBook.getISBN(), buy));
		}

		Runnable client1 = () -> {
			for(BookCopy buyB : copy)
			{
				Set<BookCopy> toBuy = new HashSet<BookCopy>();
				toBuy.add(buyB);
				try {
					client.buyBooks(toBuy);
				} catch (Exception ex) {
					Thread.currentThread().setName("failed");
				}
			}
		};

		Runnable client2 = () -> {
			for(BookCopy addB : copy)
			{
				Set<BookCopy> toAdd = new HashSet<BookCopy>();
				toAdd.add(addB);
				try {
					storeManager.addCopies(toAdd);
				} catch (Exception ex) {
					Thread.currentThread().setName("failed");
				}
			}
		};
		
		Thread client1thread = new Thread(client1, "success");
		Thread client2thread = new Thread(client2, "success");
		client1thread.start();
		client2thread.start();

		try {
			//wait for both to be done
			client1thread.join();
			client2thread.join();
			assertTrue(client1thread.getName() == "success");
			assertTrue(client2thread.getName() == "success");
		} catch (Exception ex) {
			fail(ex.toString());
		}

		List<StockBook> postBooks = storeManager.getBooks();
		
		//Check we still have a consistant stock
		for(int i = 0; i < postBooks.size(); i++)
		{
			StockBook prebook = preBooks.get(i);
			StockBook postbook = postBooks.get(i);
			assertEquals(prebook.getNumCopies(), postbook.getNumCopies());
		}
	}

	@Test
	public void testClientBuysAndRestocksOtherChecks() throws BookStoreException {
		Set<BookCopy> emptyStore = 
			storeManager.getBooks().stream()
				.map(book -> new BookCopy(book.getISBN(), book.getNumCopies()))
				.collect(Collectors.toSet());

		//Buy out all the books so we can easily predict the max number of books before and after purchase:
		client.buyBooks(emptyStore);

		assertTrue(storeManager.getBooks().stream().allMatch(book -> book.getNumCopies() == 0));
		
		Random rnd = new Random();
		int purchaseSizeMax = 10;

		//Add enough stock so first client buying cannot run out of books to buy
		storeManager.addCopies(
			storeManager.getBooks().stream()
				.map(sb -> new BookCopy(sb.getISBN(), purchaseSizeMax)).collect(Collectors.toSet()));
		
		List<BookCopy> originalStock = storeManager.getBooks().stream().map(book -> new BookCopy(book.getISBN(), book.getNumCopies())).collect(Collectors.toList());
		
		//Get which books we have by their isbns
		List<Integer> isbns = storeManager.getBooks().stream().map(book -> book.getISBN()).collect(Collectors.toList());
		int totalBookCount = isbns.size();
		//Set how many should be purchased per operation (non-zero and constant, so it is either full or full - constant)
		int purchaseAmount = rnd.nextInt(purchaseSizeMax) + 1;

		//Create list of isbns ordering of the books
		List<BookCopy> orderOfPurchases = new ArrayList<BookCopy>();
		for (int i = 0; i < totalBookCount; i++)
			orderOfPurchases.add(new BookCopy(isbns.get(i), purchaseAmount));

		Runnable client1 = () -> {
			try {
				for (int i = 0; i < numberOfOperations; i++) {
					for (BookCopy bookPurchase : orderOfPurchases) {
						Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
						booksToBuy.add(bookPurchase);
						client.buyBooks(booksToBuy);
					}
					for (BookCopy bookRestock : orderOfPurchases) {
						Set<BookCopy> booksToRestock = new HashSet<BookCopy>();
						booksToRestock.add(bookRestock);
						storeManager.addCopies(booksToRestock);
					}
				}
			} catch (Exception ex) {
				Thread.currentThread().setName(ex.toString());
			}
		};
		Thread client1thread = new Thread(client1, "success");

		//This client is really attempting to stress the server with get queries!
		Runnable client2 = () -> {
			boolean keepRunning = true;
			while (keepRunning) {
				try {
					for (StockBook book : storeManager.getBooks()) {
						if (!(book.getNumCopies() == purchaseSizeMax || book.getNumCopies() == purchaseSizeMax - purchaseAmount)) {
							Thread.currentThread().setName("Book was not either original size or original size minus purchase amount");
						}
					}
				} catch (Exception ex) {
					Thread.currentThread().setName(ex.toString());
				}
				keepRunning = client1thread.getState() == State.TERMINATED;
			}
		};
		Thread client2thread = new Thread(client2, "success");
		
		client1thread.start();
		client2thread.start();

		try {
			client1thread.join();
			assertTrue(client1thread.getState() == State.TERMINATED);
			assertTrue(client1thread.getName() == "success");
			client2thread.join(); //Expect to terminate due to client1 termination
			assertTrue(client2thread.getState() == State.TERMINATED);
			assertTrue(client2thread.getName() == "success");
			assertTrue(storeManager.getBooks().stream()
				.allMatch(book1 -> 
					originalStock.stream()
						.anyMatch(book2 -> book1.getISBN() == book2.getISBN() && book1.getNumCopies() == book2.getNumCopies())));
		} catch (Exception ex) {
			fail(ex.toString());
		}
	}

	private <T> Runnable EncapsulateConsumer(Consumer<T> consumer, T parameter) {
		return () -> consumer.accept(parameter);
	}

	@Test
	public void testPotentialExclusiveDeadlock() throws BookStoreException {

		Integer numToAdd = 100000;
		Set<BookCopy> restockerRequest = 
			storeManager.getBooks().stream()
				.map(book -> new BookCopy(book.getISBN(), 1))
				.collect(Collectors.toSet());

		List<StockBook> preBooks = storeManager.getBooks();

		Consumer<Integer> restockerClient = (Integer n) -> {
			for (int i = 0; i < n; i++) {
				try {
					storeManager.addCopies(restockerRequest);
				} catch (BookStoreException ex) {
					Thread.currentThread().setName(ex.toString());
				}
			}
		};
		Thread client1Thread = new Thread(EncapsulateConsumer(restockerClient, numToAdd), "success");
		Thread client2Thread = new Thread(EncapsulateConsumer(restockerClient, numToAdd), "success");

		client1Thread.start();
		client2Thread.start();

		try {
			//wait for both to be done
			client1Thread.join();
			client2Thread.join();
			assertTrue(client1Thread.getName() == "success");
			assertTrue(client2Thread.getName() == "success");
			Integer i = 0;
			for(StockBook book : storeManager.getBooks())
			{
				assertTrue(book.getNumCopies() == preBooks.get(i).getNumCopies() + 2*numToAdd);
				i++;
			}
		} catch (Exception ex) {
			fail(ex.toString());
		}
	}

	@Test
	public void testMassClientWritesReads() throws BookStoreException {
		List<StockBook> prebooks = storeManager.getBooks();

		//Client adding n books one at a time to the entire stock
		Consumer<Integer> restockerClient = (Integer n) -> {
			Set<BookCopy> restockerRequest = 
				prebooks.stream()
					.map(book -> new BookCopy(book.getISBN(), 1))
					.filter(bookcopy -> bookcopy.getISBN() != prebooks.get(0).getISBN()
								     && bookcopy.getISBN() != prebooks.get(1).getISBN())
					.collect(Collectors.toSet());

			for (int i = 0; i < n; i++) {
				try {
					storeManager.addCopies(restockerRequest);
				} catch (BookStoreException ex) {
					Thread.currentThread().setName(ex.toString());
					break;
				}
			}
		};
		
		Consumer<SimpleEntry<Integer,Integer>> editorClient = (SimpleEntry<Integer,Integer> entry) -> {
			for (int i = 0; i < entry.getKey(); i++) {
				try {
					client.getEditorPicks(entry.getValue());
				} catch (BookStoreException ex) {
					Thread.currentThread().setName(ex.toString());
					break;
				}
			}
		};

		Consumer<SimpleEntry<Integer,Integer>> topRatingClient = (SimpleEntry<Integer,Integer> entry) -> {
			for (int i = 0; i < entry.getKey(); i++) {
				try {
					client.getTopRatedBooks(entry.getValue());
				} catch (BookStoreException ex) {
					Thread.currentThread().setName(ex.toString());
					break;
				}
			} 
		};
		
		Consumer<Integer> rateBooksClient = (Integer n) -> {
			Set<Integer> allRegisteredISBN = 
				prebooks.stream()
					.map(book -> book.getISBN())
					.filter(isbn -> isbn != prebooks.get(0).getISBN()
								 && isbn != prebooks.get(1).getISBN())
					.collect(Collectors.toSet());

			Random rnd = new Random();
			for (int i = 0; i < n; i++) {
				int rating = rnd.nextInt(6);
				try {
					client.rateBooks(allRegisteredISBN.stream().map(isbn -> new BookRating(isbn, rating)).collect(Collectors.toSet()));
				} catch(BookStoreException ex) {
					Thread.currentThread().setName(ex.toString());
					break;
				}
			}
		};

		//Should LOCK entire collection, hence the other clients who reads before writing should know it.
		Consumer<Integer> removeBook = (Integer isbn) -> {
			Set<Integer> isbnset = new HashSet<Integer>();
			isbnset.add(isbn);
			try {
				storeManager.removeBooks(isbnset);
			} catch (BookStoreException ex) {
				Thread.currentThread().setName(ex.toString());
			}
		};

		Integer blockSize = 32; //Threads
		Integer N = 1000; //Number of repeated operations

		//#blockSize clients refilling N times, therefore total stock should be original + blockSize*N for each book
		List<Thread> restockerThreads = new ArrayList<Thread>();
		List<Thread> rateBooksThreads = new ArrayList<Thread>();
		List<Thread> trafficThreads = new ArrayList<Thread>(); 
		for (int i = 0; i < blockSize; i++) {
			restockerThreads.add(new Thread(EncapsulateConsumer(restockerClient, N), "success"));
			rateBooksThreads.add(new Thread(EncapsulateConsumer(rateBooksClient, N), "success"));
			trafficThreads.add(new Thread(EncapsulateConsumer(editorClient, new SimpleEntry(N, 5)), "success"));
			trafficThreads.add(new Thread(EncapsulateConsumer(topRatingClient, new SimpleEntry(N, 5)), "success"));
		}

		Thread removeDefaultBook = new Thread(EncapsulateConsumer(removeBook, prebooks.get(0).getISBN()), "success");
		Thread removeFirstDefaultBookInCollection = new Thread(EncapsulateConsumer(removeBook, prebooks.get(1).getISBN()), "success");


		for(int i = 0; i < blockSize; i++) {
			restockerThreads.get(i).start();
			rateBooksThreads.get(i).start();
			trafficThreads.get(i*2).start();
			trafficThreads.get(i*2 + 1).start();
		}

		try {
			Thread.sleep(20); //Have the other threads do "some" work (by assumption)
		} catch (Exception ex) {
			fail(ex.toString());
		}
		removeDefaultBook.start();
		try {
			Thread.sleep(20); //Have the other threads do "some" work (by assumption)
		} catch (Exception ex) {
			fail(ex.toString());
		}
		removeFirstDefaultBookInCollection.start();

		try {
			//Wait for all transactions to be done
			removeDefaultBook.join();
			if (removeDefaultBook.getName() != "success") {
				fail(removeDefaultBook.getName());
			}
			assertTrue(removeDefaultBook.getName() == "success");
			removeFirstDefaultBookInCollection.join();
			if (removeFirstDefaultBookInCollection.getName() != "success") {
				fail(removeFirstDefaultBookInCollection.getName());
			}
			assertTrue(removeFirstDefaultBookInCollection.getName() == "success");
			for(int i = 0; i < blockSize; i++) {
				restockerThreads.get(i).join();
				if (restockerThreads.get(i).getName() != "success") {
					fail(i + ": " + restockerThreads.get(i).getName());
				}
				assertTrue(restockerThreads.get(i).getName() == "success");
			}
			for(int i = 0; i < blockSize; i++) {
				rateBooksThreads.get(i).join();
				assertTrue(rateBooksThreads.get(i).getName() == "success");
			}
			for(int i = 0; i < 2*blockSize; i++) {
				trafficThreads.get(i).join();
				assertTrue(trafficThreads.get(i).getName() == "success");
			}
			List<StockBook> postbooks = storeManager.getBooks();

			//Post books has two less books
			assertTrue(prebooks.size() == postbooks.size() + 2);
			
			//Post book is still a subset of prebooks
			for (int i = 0; i < postbooks.size(); i++) {
				assertTrue(prebooks.get(i+2).getISBN() == postbooks.get(i).getISBN());
			}
			//All post books has a blockSize * N total ratings
			for (int i = 0; i < postbooks.size(); i++) {
				assertTrue(postbooks.get(i).getNumTimesRated() == blockSize * N);
			}
			//All post books have added blockSize * N extra copies
			for (int i = 0; i < postbooks.size(); i++) {
				assertTrue(postbooks.get(i).getNumCopies() == prebooks.get(i+2).getNumCopies() + blockSize * N);
			}
		} catch (Exception ex) {
			fail(ex.toString());
		}
	}

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
