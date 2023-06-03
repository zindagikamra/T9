import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class T9Visualizer extends JPanel implements MouseListener, KeyListener, ListSelectionListener
{
	// Change this if you'd like to load a different dictionary
	private static final String DICTIONARY = "dictionary-sowpods.txt";
	
	private static final int KEYPAD_WIDTH = 300;
	private static final int KEYPAD_HEIGHT = 300;
	private static final int DIGITS_LABEL_CHAR_WIDTH = 16;
	
	// Special values for pressedKey
	private static final int PRESSED_KEY_NONE = -1;
	private static final int PRESSED_KEY_BACKSPACE = 0;
	private static final int PRESSED_KEY_CLEAR = 1;
	
	/*
	 * X coordinates:
	 * 		first column: 14 thru 84
	 * 		second column: 109 thru 189
	 * 		third column: 212 thru 285
	 * 
	 * Y coordinates:
	 * 		first row: 16 thru 62
	 * 		second row: 90 thru 138
	 * 		third row: 164 thru 214
	 */	
	private static final int[][] COLUMN_TO_X_RANGES = new int[][] {
		{14, 84}, {109, 189}, {212, 285}
	};
	private static final int[][] ROW_TO_Y_RANGES = new int[][] {
		{16, 62}, {90, 138}, {164, 214}
	};
	private static final int[][] DIGIT_TO_ROWCOL = new int[][] {
			null, null, new int[] { 0, 1 }, new int[] { 0, 2 },
			new int[] { 1, 0 }, new int[] { 1, 1 }, new int[] { 1, 2 },
			new int[] { 2, 0 }, new int[] { 2, 1 }, new int[] { 2, 2 },
	};
	
	// GUI
	private JFrame frame;
	private JTextArea generatedTextLabel;
	private JLabel digitsLabel;
	private JTextField maxPredField;
	private JList<WordWithMarkup> wordsSpelled;
	private PhoneImageComponent phoneImageComponent;
	
	// Events
	private Object keypressLock;
	private int pressedKey;

	// Data
	private String digitsStr;
	private String typedMessage;
	private T9 t9;
	
	// ----------------------------------------------------------------
	// PhoneImageComponent manages the phone keypad image
	// ----------------------------------------------------------------
	
	public static class PhoneImageComponent extends Component
	{
		private Image phoneImage;
		private String disabledMessage;
		private int highlightedButton;
		private T9Visualizer t9Visualizer;
		private JFrame frame;
		
		public PhoneImageComponent(T9Visualizer t9VisualizerP, JFrame frameP)
		{
	        ImageIcon phoneIcon = new ImageIcon("PhoneKeypad.jpg");
	        phoneImage = phoneIcon.getImage();
	        setFocusable(true);
	        disabledMessage = null;
	        highlightedButton = -1;
	        t9Visualizer = t9VisualizerP;
	        frame = frameP;
		}
		
		@Override
		public void paint(Graphics g)
		{
			if (disabledMessage == null)
			{
				// Draw the phone keypad
				g.drawImage(phoneImage, 0, 0, getWidth(), getHeight(), null /* ImageObserver */);
				
				// If a button is pressed, but T9 hasn't returned yet,
				// put a green oval on top so user sees that the press
				// is still being processed
				if (highlightedButton != -1)
				{
					int[] rowcol = DIGIT_TO_ROWCOL[highlightedButton];
					int[] xRanges = COLUMN_TO_X_RANGES[rowcol[1]];
					int[] yRanges = ROW_TO_Y_RANGES[rowcol[0]];
					int xCenter = (xRanges[0] + xRanges[1]) / 2;
					int yCenter = (yRanges[0] + yRanges[1]) / 2;
					g.setColor(new Color(0, 255, 0, 100));
					g.fillOval(xCenter - 15, yCenter - 15, 30, 30);
				}
			}
			else
			{
				// Keypad is disabled while the dictionary loads
	            Font font = new Font(Font.DIALOG, Font.BOLD, 30);
				g.setFont(font);
				g.setColor(Color.BLACK);
				g.drawString(
						disabledMessage, 
						0 /* x */, 
						30 /* y */);
			}
		}
		
		public void disable(String message)
		{
			disabledMessage = message;
			frame.repaint();
		}
		
		public void enable()
		{
			disabledMessage = null;
			frame.repaint();
		}
		
		public void highlightButton(int digit)
		{
			highlightedButton = digit;
			frame.repaint();
		}

		public void unhighlightButtons()
		{
			highlightedButton = -1;
			frame.repaint();
		}
	}

	// ----------------------------------------------------------------
	// WordWithMarkup pairs a raw word string with its markup string.
	// Instances of this are placed in the list control, so prefixed words
	// can appear visually distinct from spelled words
	// ----------------------------------------------------------------
	public static class WordWithMarkup
	{
		private String word;
		private String wordWithMarkup;
		
		public WordWithMarkup(String wordP, String wordWithMarkupP)
		{
			word = wordP;
			wordWithMarkup = wordWithMarkupP;
		}
		
		public String toString()
		{
			return wordWithMarkup;
		}
		
		public String getRawWord()
		{
			return word;
		}
	}
	
	// ----------------------------------------------------------------
	// Layout: 2 stripes arranged vertically
	// 
	// Stripe 1:
	//
	// Digits BackspaceButton
	//
	// Phone pic			Max predictions: (edit ctl)
	//						(listbox)
	//
	// Stripe 2:
	// 
	// Text generated
	// ----------------------------------------------------------------
	
	public T9Visualizer(JFrame frameP) 
	{
		super();
		
		digitsStr = "";
		typedMessage = "";
		frame = frameP;
		
		keypressLock = new Object();
		pressedKey = PRESSED_KEY_NONE;
		
		// Stripe 1
		JPanel phonePanel = initPhonePanel(frame);
		
		// Stripe 2
		JPanel generatedTextPanel = initGeneratedTextPanel();
		
		setFocusable(true);
		
		// Assemble above into banner layout
		setLayout(new BorderLayout());
		add(phonePanel, BorderLayout.PAGE_START);
		add(generatedTextPanel, BorderLayout.CENTER);
		setVisible(true);
	}
	
	// Stripe 1
	private JPanel initPhonePanel(JFrame frame)
	{
		// Backspace button
		JButton backspaceButton = new JButton("Backspace");
		backspaceButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				onBackspaceClicked();
			}
		});
		
		// Digits field
		digitsLabel = new JLabel(getSpaces(DIGITS_LABEL_CHAR_WIDTH));
		Font digitsFont = new Font("Courier", Font.PLAIN, 20);
		digitsLabel.setFont(digitsFont);

		// Phone keypad
		phoneImageComponent = new PhoneImageComponent(this, frame);
		JLabel maxPredLabel = new JLabel("Max predictions:");
		maxPredField = new JTextField("100", 3);
		
		// Words Spelled list
		JScrollPane scrollPane = new JScrollPane();
		wordsSpelled = new JList<WordWithMarkup>();
		scrollPane.setViewportView(wordsSpelled);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		wordsSpelled.setPrototypeCellValue(new WordWithMarkup("INCONSEQUENTIALLY", "INCONSEQUENTIALLY"));
		wordsSpelled.setFixedCellWidth(140);
		wordsSpelled.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		wordsSpelled.setListData(new WordWithMarkup[] { }); 
		wordsSpelled.setVisibleRowCount(17);
		wordsSpelled.setLayoutOrientation(JList.VERTICAL);
		wordsSpelled.addListSelectionListener(this);
		
		// Entire phone panel layout
		
		JPanel phonePanel = new JPanel();
		
		JPanel digitsPanel = new JPanel();
		digitsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		digitsPanel.add(digitsLabel);
		digitsPanel.add(backspaceButton);
		
		JPanel predictionsPanel = new JPanel();
		predictionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		predictionsPanel.add(maxPredLabel);
		predictionsPanel.add(maxPredField);
		
		GroupLayout phoneLayout = new GroupLayout(phonePanel);
		phonePanel.setLayout(phoneLayout);
		phoneLayout.setAutoCreateGaps(true);
		phoneLayout.setAutoCreateContainerGaps(true);
		
		phoneLayout.setHorizontalGroup(
				phoneLayout.createSequentialGroup()
				.addGroup(phoneLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(digitsPanel)
						.addComponent(phoneImageComponent, KEYPAD_WIDTH, KEYPAD_WIDTH, KEYPAD_WIDTH))
				.addGroup(phoneLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(predictionsPanel)
						.addComponent(scrollPane)
						));

		phoneLayout.setVerticalGroup(
				phoneLayout.createSequentialGroup()
				.addGroup(phoneLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(digitsPanel)
						.addComponent(predictionsPanel))
				.addGroup(phoneLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(scrollPane)
						.addComponent(phoneImageComponent, KEYPAD_HEIGHT, KEYPAD_HEIGHT, KEYPAD_HEIGHT)
						));
						
		return phonePanel;
	}
	
	// Stripe 2
	private JPanel initGeneratedTextPanel()
	{
		JPanel generatedTextPanel = new JPanel();
		
		JScrollPane scrollPane = new JScrollPane();
		generatedTextLabel = new JTextArea(3, 50);
		generatedTextLabel.setLineWrap(true);
		generatedTextLabel.setWrapStyleWord(true);

		scrollPane.setViewportView(generatedTextLabel);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		generatedTextPanel.setLayout(new FlowLayout());
		generatedTextPanel.add(scrollPane);
		
		return generatedTextPanel;
	}

	private String getSpaces(int numSpaces)
	{
		String ret = "";
		
		for (int i=0; i < numSpaces; i++)
		{
			ret += " ";
		}
		
		return ret;
	}
	
	// -------------------------------------------------
	// MAIN THREAD event processing
	// These methods respond to state changes (caused by
	// AWT Event Thread) and then call into student's
	// T9 object
	// -------------------------------------------------

	public void loadDictionaryAndProcessEvents()
	{
		phoneImageComponent.disable("Reading dictionary...");
		t9 = new T9(DICTIONARY);
		phoneImageComponent.enable();

		phoneImageComponent.addMouseListener(this);
		phoneImageComponent.addKeyListener(this);
		addKeyListener(this);
		
		// Main thread loop to process state changes by AWT Thread
		while (true)
		{
			processKeypress();
		}
	}
	
	// Main thread: READ event, if any, and process it
	private void processKeypress()
	{
		int pressedKeyCur = PRESSED_KEY_NONE;
		
		// Clean read of keypress state
		synchronized(keypressLock)
		{
			pressedKeyCur = pressedKey;
		}
		
		// No state change, do nothing
		if (pressedKeyCur == PRESSED_KEY_NONE)
		{
			return;
		}

		if (pressedKeyCur == PRESSED_KEY_BACKSPACE)
		{
			processBackspaceClicked();
		}
		else if (pressedKeyCur == PRESSED_KEY_CLEAR)
		{
			processClear();
		}
		else
		{
			// Otherwise, numerical digit was pressed
			processPhoneButtonClicked(pressedKeyCur);
		}
		
		// Clean reset of state
		synchronized(keypressLock)
		{
			pressedKey = PRESSED_KEY_NONE;
		}
	}
	
	private void sleep(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	// MAIN thread: send press command to T9
	private void processPhoneButtonClicked(int digit)
	{
		phoneImageComponent.highlightButton(digit);
		if (t9.press(digit))
		{
			digitsStr += digit;
			onDigitsChanged();
		}
		sleep(20);	// To show brief flash of green highlight over phone key
		phoneImageComponent.unhighlightButtons();
	}
	
	// MAIN thread: send back command to T9
	private void processBackspaceClicked()
	{
		if (digitsStr.isEmpty())
		{
			return;
		}

		if (t9.back())
		{
			digitsStr = digitsStr.substring(0, digitsStr.length() - 1);
			onDigitsChanged();
		}
	}

	// MAIN thread: send clear command to T9
	private void processClear()
	{
		t9.clear();
		digitsStr = "";
		onDigitsChanged();
	}

	// MAIN thread: call getWordsSpelled/Prefixed
	private void onDigitsChanged()
	{
		digitsLabel.setText(digitsStr + getSpaces(DIGITS_LABEL_CHAR_WIDTH - digitsStr.length()));

		int max = -1;
		try
		{
			max = Integer.parseInt(maxPredField.getText());
		}
		catch(NumberFormatException e)
		{
			JOptionPane.showMessageDialog(null, "Please enter a number for 'max predictions'", "Invalid Input", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ArrayList<WordWithMarkup> words = new ArrayList<WordWithMarkup>();
		for (String word : t9.getWordsSpelled())
		{
			words.add(new WordWithMarkup(word, "<html>" + word + "</html>"));
		}
		for (String word : t9.getWordsPrefixed(max))
		{
			words.add(new WordWithMarkup(word, "<html><i><font size=\"3\" color=\"red\">" + word + "</font></i></html>"));
		}
		
		wordsSpelled.setListData(words.toArray(new WordWithMarkup[] {}));
	}
	

	// -----------------------------------------------------------
	// AWT EVENT THREAD MOUSE LISTENER for clicks on phone image
	// -----------------------------------------------------------
	
	@Override
	public void mouseClicked(MouseEvent e) 
	{
		requestFocusInWindow();
		int digit = mousePointToDigit(e.getPoint());
		if (digit < 2 || digit > 9)
		{
			return;
		}
		
		onPhoneButtonClicked(digit);
	}

	@Override
	public void mouseEntered(MouseEvent arg0) { }

	@Override
	public void mouseExited(MouseEvent arg0) { }

	@Override
	public void mousePressed(MouseEvent arg0) { }

	@Override
	public void mouseReleased(MouseEvent arg0) { }

	private int mousePointToDigit(Point point)
	{
		int column = -1;
		for (int i=0; i < COLUMN_TO_X_RANGES.length; i++)
		{
			if (COLUMN_TO_X_RANGES[i][0] <= point.x && point.x <= COLUMN_TO_X_RANGES[i][1])
			{
				column = i;
				break;
			}
		}
		
		if (column == -1)
		{
			return -1;
		}
		
		int row = -1;
		for (int i=0; i < ROW_TO_Y_RANGES.length; i++)
		{
			if (ROW_TO_Y_RANGES[i][0] <= point.y && point.y <= ROW_TO_Y_RANGES[i][1])
			{
				row = i;
				break;
			}
		}
		
		if (row == -1)
		{
			return -1;
		}
		
		return (row*3 + column) + 1;
	}
	
	
	// ----------------------------------------------------------------
	// AWT EVENT THREAD KEY LISTENER for digits pressed on keyboard
	// ----------------------------------------------------------------
	
    @Override
    public void keyPressed(KeyEvent arg0) { }
    
    @Override
    public void keyReleased(KeyEvent arg0) { }
    
    @Override
    public void keyTyped(KeyEvent arg0) 
    {
    	char typed = arg0.getKeyChar();
    	if (typed == KeyEvent.VK_BACK_SPACE)
    	{
    		onBackspaceClicked();
    	}
    	else if ('2' <= typed && typed <= '9')
    	{
    		onPhoneButtonClicked(typed - '0');
    	}
    }
    
	// ---------------------------------------------------------------------------------
	// AWT EVENT THREAD: helpers to notify main thread about events via state changes 
	// ---------------------------------------------------------------------------------
	
    // AWT Event thread: STORE event for main thread to process
	private void resetDigits()
	{
		// Cleanly change state so main thread will know to process this click
    	synchronized(keypressLock)
    	{
    		pressedKey = PRESSED_KEY_CLEAR;
    	}
		phoneImageComponent.requestFocusInWindow();
	}

	// AWT Event thread: STORE event for main thread to process
    private void onPhoneButtonClicked(int digit)
    {
    	// Cleanly change state so main thread will know to process this click
    	synchronized(keypressLock)
    	{
    		pressedKey = digit;
    	}
    }
    
    // AWT Event thread: STORE event for main thread to process
    private void onBackspaceClicked()
    {
    	// Cleanly change state so main thread will know to process this click
    	synchronized(keypressLock)
    	{
    		pressedKey = PRESSED_KEY_BACKSPACE;
    	}
		phoneImageComponent.requestFocusInWindow();
    }


	// ----------------------------------------------------------------
	// AWT EVENT THREAD LIST SELECTION LISTENER for selections in list
    // of words matching digits pressed
	// ----------------------------------------------------------------
    
	@Override
	public void valueChanged(ListSelectionEvent event) 
	{
		if (event.getValueIsAdjusting())
		{
			return;
		}
		
		WordWithMarkup selectedValue = wordsSpelled.getSelectedValue();
		if (selectedValue == null)
		{
			return;
		}
		
		typedMessage += selectedValue.getRawWord() + " ";
		generatedTextLabel.setText(typedMessage);
		resetDigits();
	}
	
	
	// ----------------------------------------------------------------
	// MAIN THREAD, main method
	// ----------------------------------------------------------------
	
	public static void main(String[] args) 
	{
		// If you'd like to load a different dictionary, find the
		// DICTIONARY field at the top of this file and change its value
		
        JFrame frame = new JFrame("T9");
		T9Visualizer t9Visualizer = new T9Visualizer(frame);
		
        frame.add(t9Visualizer);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        t9Visualizer.loadDictionaryAndProcessEvents();
	}	
}