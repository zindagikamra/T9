import java.util.ArrayList;
import java.util.Iterator;


public class T9
{
	// Initializes state of the object by reading the specified file
	// of words.
	//private static final int R = 256;// extended ASCII
	private static final int R = 8;
	
	private class Node {
		private ArrayList<String> val = new ArrayList<String>();
		private Node[] next = new Node[8];
		private Node previous;
	}
	
	private class TrieST<Value> 
	{
	    private Node root = new Node();
	    private Node current = root;
	    //private Node previous;
	    private int position = 0;

	   /****************************************************
	    * Is the key in the symbol table?
	    ****************************************************/
	    /*public boolean contains(String key) {
	        return get(key) != null;
	    }*/

	    public Value get(String key) {
	        /*Node x = get(root, key, 0);
	        if (x == null) return null;
	        return (Value) x.val;*/
	        
	        Node x = get(current, key, position);
	        if (x == null) return null;
	        return (Value) x.val;
	    }

	    private Node get(Node x, String key, int d) {
	        if (x == null) return null;
	        if (d == key.length()) return x;
	        char c = key.charAt(d);
	        return get(x.next[c%50], key, d+1);
	    }

	   /****************************************************
	    * Insert key-value pair into the symbol table.
	    ****************************************************/
	    public void put(String key, ArrayList<String> val) {
	        root = put(root, key, val, 0);
	    }

	    private Node put(Node x, String key, ArrayList<String> val, int d) {
	        if (x == null) x = new Node();
	        if (d == key.length()) {
	            x.val = val;
	            return x;
	        }
	        char c = key.charAt(d);
	        x.next[c%50] = put(x.next[c%50], key, val, d+1);
	        return x;
	    }

	    // find the key that is the longest prefix of s
	    /*public String longestPrefixOf(String query) {
	        int length = longestPrefixOf(root, query, 0, 0);
	        return query.substring(0, length);
	    }

	    // find the key in the subtrie rooted at x that is the longest
	    // prefix of the query string, starting at the dth character
	    private int longestPrefixOf(Node x, String query, int d, int length) {
	        if (x == null) return length;
	        if (x.val != null) length = d;
	        if (d == query.length()) return length;
	        char c = query.charAt(d);
	        return longestPrefixOf(x.next[c], query, d+1, length);
	    }


	    public Iterable<String> keys() {
	        return keysWithPrefix("",  0);
	    }*/

	    /*public Iterable<String> keysWithPrefix(String prefix) {
	        Queue<String> queue = new Queue<String>();
	        Node x = get(root, prefix, 0);
	        collect(x, prefix, queue);
	        return queue;
	    }

	    private void collect(Node x, String key, Queue<String> queue) {
	        if (x == null) return;
	        if (x.val != null) queue.enqueue(key);
	        for (int c = 0; c < R; c++)
	            collect(x.next[c], key + (char) c, queue);
	    }*/
	    
	    public Iterable<String> keysWithPrefix(String prefix, int max) {
	        ArrayList<String> queue = new ArrayList<String>();
	        //Node x = get(root, prefix, 0);
	        Node x = current;
	        collect(x,  queue, max);
	        if(x != null)
	        {
	        	queue.removeAll(x.val);
	        }
	        if(queue.size() > max)
	        {
	        	return new ArrayList<String>();
	        }
	        return queue;
	    }

	    private void collect(Node x, ArrayList<String> queue, int max) 
	    {
	    	/*if(queue.size() > max)
	    	{
	    		//queue = new ArrayList<String>();
	    		return;
	    	}*/
	    	
	    	if (x == null) 
	        {
	        	return;
	        }
	        if (x.val != null) 
	        {
	        	queue.addAll(queue.size(), x.val);
	        }
	        for (int c = 0; c < R; c++)
	        {
	        	
	        	collect(x.next[c], queue, max);
	        }
	    	
	        //return queue;
	    }
	}
	
	private String currentDigits;
	private TrieST<ArrayList<String>> trie;
	
	public T9(String dictionary)
	{
		currentDigits = "";
		trie = new TrieST<ArrayList<String>>();
		
        In in = new In("testinput/" + dictionary);
        String[] words = in.readAllStrings();
        
        for (String word : words)
        {
        	//System.out.println(word);
        	String key = "";
        	int d = 0;
        	Node current = trie.root;
        	while(d < word.length())
        	{	
        		char c = word.charAt(d);
        		if(c == 'A' || c == 'B' || c == 'C')
        		{
        			key += "2";
        		}
        		else if(c == 'D' || c == 'E' || c == 'F')
        		{
        			key += "3";
        			
        		}
        		else if(c == 'G' || c == 'H' || c == 'I')
        		{
        			key += "4";
        		}
        		else if(c == 'J' || c == 'K' || c == 'L')
        		{
        			key += "5";
        		}
        		else if(c == 'M' || c == 'N' || c == 'O')
        		{
        			key += "6";
        		}
        		else if(c == 'P' || c == 'Q' || c == 'R' || c == 'S')
        		{
        			key += "7";
        		}
        		else if(c == 'T' || c == 'U' || c == 'V')
        		{
        			key += "8";
        		}
        		else
        		{
        			key += "9";
        		}
        		
        		d++;
        	}
        	ArrayList<String> l = trie.get(key);
    		
    		if(l == null)
    		{
    			//System.out.println("I am null");
    			ArrayList<String> list = new ArrayList<String>();
    			list.add(word);
    			trie.put(key, list);
    		}
    		else
    		{
    			//System.out.println("I am not null");
    			l.add(word);
        		trie.put(current, key, l, 0);
    		}
        	
        	
        }
	}
	
	// This simulates the pressing of a key on the phone.  Normally, this
	// updates the internal state by appending num to the end of the current
	// digit sequence, and then returns true.  However, if appending num
	// to the current digit sequence would produce a sequence
	// that does not spell or prefix any words in the dictionary, then
	// the internal state is left unmodified, and this returns false.
	// You may assume 2 <= num <= 9.
	public boolean press(int num)
	{
		
		currentDigits += num;
		if(trie.get(currentDigits) == null)
		{
			currentDigits = currentDigits.substring(0, currentDigits.length()-1);
			return false;
		}
		Node c = trie.current;
		trie.current = trie.get(trie.current, currentDigits, trie.position);
		trie.current.previous = c;
		trie.position++;
		return true;
	}
	
	// This simulates the pressing of the backspace key on the phone.  Normally,
	// this updates the internal state by removing the last digit from the
	// current digit sequence, and then returns true.  However, if the
	// internal state did not contain any digits on entry to this method, then
	// the internal state is left unmodified, and this returns false.
	public boolean back()
	{
		if(!currentDigits.isEmpty())
		{
			currentDigits = currentDigits.substring(0, currentDigits.length()-1);
			trie.current = trie.current.previous;
			trie.position--;
			return true;
		}
		return false;
		
		
	}
	
	// Returns the sequence of digits pressed by the user.  If no digits
	// are currently stored, this returns the empty String "".
	public String getCurrentDigitSequence()
	{
		return currentDigits; 
	}
	
	// This simulates the user choosing to reset and start over with a new
	// word (typically in response to the user selecting a previously spelled
	// word to appear in the text message, ready to begin typing the next word).
	// This clears the state such that a subsequent call to
	// getCurrentDigitSequence would return "". 
	public void clear()
	{
		currentDigits = "";
		trie.current = trie.root;
		trie.position = 0;
	}
	
	// This returns all words spelled by the current digit sequence
	public Iterable<String> getWordsSpelled()
	{
		return trie.get(currentDigits);
	}
	
	// Normally, this returns all words *prefixed* by the current digit
	// sequence.  However, if the number of such words is greater than max,
	// this returns an empty Iterable.  This method should not return words
	// that are exactly spelled out by the sequence of digits pressed by the
	// user (i.e., words that would be returned by getWordsSpelled).
	public Iterable<String> getWordsPrefixed(int max)
	{
		/* ArrayList<String> list = new ArrayList<String>();
		 Node endOfPrefix = trie.get(trie.root, currentDigits, 0);
		 ArrayList<String> = */
		return trie.keysWithPrefix(currentDigits, max);
		 
	}
	
	
	public static void main(String[] args)
	{
		T9 test = new T9("dictionary-cp1.txt");
		
		//test.press(2);
		test.press(8);
		test.press(2);
		test.clear();
		test.press(2);
		//test.press(4);
		//test.press(3);
		//test.press(6);
		//test.press(9);
		//System.out.println(test.getCurrentDigitSequence());
		
		Iterable<String> itb = test.getWordsPrefixed(70);
		Iterator<String> itr = itb.iterator();
		
		while(itr.hasNext())
		{
			System.out.println(itr.next());
		}
		
		//test.back();
		//System.out.println("|" + test.getCurrentDigitSequence() + "|");
	}
}