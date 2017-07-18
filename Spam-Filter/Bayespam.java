import java.io.*;
import java.util.*;

public class Bayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL )
			{
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
    }
	
    /// This is a class with two log probabilities (for regular and spam) 
    static class Multiple_Logprob
    {
		double prob_regular = 0;
		double prob_spam = 0;
	
		public void setLogProb(double prob, String type)
		{
			if (type == "regular")
			{
				prob_regular = prob;
			} else {
				prob_spam=prob;
			}
		}
    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();
 
    /// Creates a hashtable for the vocabulary and their log probabilities
    private static Hashtable <String, Multiple_Logprob> vocab2 = new Hashtable <String, Multiple_Logprob> ();
    
    /// These variables are used in the program to compute the probabilities
    private static double aPrioriRegular;
    private static double aPrioriSpam;
    private static int nWordsRegular;
    private static int nWordsSpam;
    
    /// This function calculates the apriori probability
    private static double aPriori(File[] a)
    {
		double length = a.length;
		double lengthTotal = listing_regular.length + listing_spam.length;
		return length/lengthTotal;
    }
    
	/// This function calculates the number of words for regular and spam
    private static int nWords(String type)
    {	
		int n_words = 0;
		if(type == "regular"){
			for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
			{   
				String word;
				Multiple_Counter counter;
				word = e.nextElement();
				counter = vocab.get(word);
				n_words += counter.counter_regular;
			}
		} else {
			for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
			{   
				String word;
				Multiple_Counter counter;
				word = e.nextElement();
				counter = vocab.get(word);
				n_words += counter.counter_spam;
			}
		}
		return n_words;
    }
 
    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) )
		{                  								// if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately
        vocab.put(word, counter);                       // put the word with its counter into the hashtable
    }

    /// Creates hash table with log probabilities
    private static void addLog()
    {
		double tuner = 0.0001;								// The tuner is used for not getting 0-valued probabilities
		for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
		{   
			Multiple_Counter counter = new Multiple_Counter();
			Multiple_Logprob logprob = new Multiple_Logprob();
            String word;
            word = e.nextElement();
            counter = vocab.get(word);
            double logprob_regular;
            double logprob_spam;
			if(counter.counter_regular > 0)
			{
				logprob_regular = Math.log((double)counter.counter_regular/(double)nWordsRegular);
			} else {
				logprob_regular = Math.log(tuner/(double)(nWordsRegular+nWordsSpam));
			}
			
            logprob.setLogProb(logprob_regular, "regular");
			
            if(counter.counter_spam > 0)
			{
				logprob_spam = Math.log((double)counter.counter_spam/(double)nWordsSpam);
            } else {
				logprob_spam = Math.log(tuner/(double)(nWordsRegular+nWordsSpam));
			}
			
            logprob.setLogProb(logprob_spam, "spam");
            vocab2.put(word, logprob);
        }
    }
    
    /// This function reads a test message, calculates the sum of the posteri probability and concludes if it is regular or spam
    private static String testMessage(FileInputStream i_s, String type)
    throws IOException
    {
		double sumPosteriRegular=0;
		double sumPosteriSpam=0;
		
        BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
        String line;
        String word;
            
        while ((line = in.readLine()) != null)                      // read a line
        {
            StringTokenizer st = new StringTokenizer(line);         // parse it into words
            while (st.hasMoreTokens())                  			// while there are still words left..
            {
				word = st.nextToken().replaceAll("[^a-zA-Z]", "").toLowerCase(); 	
                if (word.length() >= 4 && vocab.containsKey(word)){
					sumPosteriRegular += vocab2.get(word).prob_regular;
					sumPosteriSpam += vocab2.get(word).prob_spam;    
				}	
            }
        }
		in.close();
        if (sumPosteriRegular > sumPosteriSpam)
		{
			return "regular!";
		} else {
			return "spam!";
		}
    }

    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
    }

    
    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            word = e.nextElement();
            counter  = vocab.get(word);
            System.out.println(word + " | in regular: " + counter.counter_regular + " in spam: " + counter.counter_spam);
		}
    }


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages(MessageType type)
    throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;
            
            while ((line = in.readLine()) != null)                      		// read a line
            {
                StringTokenizer st = new StringTokenizer(line);         		// parse it into words
        
                while (st.hasMoreTokens())                  				// while there are stille words left..
                {
		    word = st.nextToken().replaceAll("[^a-zA-Z]", "").toLowerCase(); 	/// First, all characters other than a-z and A-Z
                    if (word.length() >= 4) {						/// are removed. After that the word is converted
					addWord(word, type);						/// to lowercase. If the word has length 4 or more
					}									/// then it is added to the vocabulary.
                }
            }

			in.close();
        }
    }
   
    public static void main(String[] args)
    throws IOException
    {
	// Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        /// Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

	/// Calculate the a priori and nWords for regular and spam messages
	aPrioriRegular = Math.log(aPriori(listing_regular));
	aPrioriSpam = Math.log(aPriori(listing_spam));
	nWordsRegular = nWords("regular");
	nWordsSpam = nWords("spam");
	
        /// Creates hash table with log probabilities
        addLog();
        
        /// Print out the hash table
	printVocab();

	/// Print the a priori and nWords for regular and spam messages
        System.out.println("a priori (log) regular message: " + aPrioriRegular);
        System.out.println("a priori (log) spam    message: " + aPrioriSpam);
        System.out.println("nWords in regular: " + nWordsRegular);
        System.out.println("nWords in spam: " + nWordsSpam);

	/// Location of the directory taken from the command line (second argument)
        File test_location = new File( args[1] );
        if ( !test_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }
		
        /// Initialize the regular and spam lists from the test files 
        listDirs(test_location);
        
	/// Variables used to generate a confusion matrix
        int regular_true = 0;
        int regular_false = 0;
        int spam_true = 0;
        int spam_false = 0;
        
        /// Read all the files in the test folder and classify them as regular or spam
        for (int i=0; i < listing_regular.length; i++)
		{
			FileInputStream i_s = new FileInputStream( listing_regular[i] );
			if (testMessage(i_s, "regular").equals("spam!"))
			{
				regular_false++;
			}
		}
		for (int i=0; i < listing_spam.length; i++){
			FileInputStream i_s = new FileInputStream( listing_spam[i] );
			if (testMessage(i_s, "spam").equals("regular!"))
			{
				spam_false++;
			}
		}
		
		/// Calculate the correctly classified messages
		regular_true=listing_regular.length-regular_false;
		spam_true=listing_spam.length-spam_false;
		
		/// Prints the confusion matrix, with the percentage of correctly classified messages per category
		System.out.println("Correctly regular: " + regular_true + " | Falsely regular: " + spam_false + " | " + 100*((double)regular_true/(((double)spam_false)+(double)regular_true)) + "%");
		System.out.println("Correctly spam: " + spam_true + " | Falsely spam: " + regular_false + " | " + 100*((double)spam_true/((double)regular_false+(double)spam_true)) + "%");
    }
}
