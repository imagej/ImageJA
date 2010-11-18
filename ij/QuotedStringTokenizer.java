package ij;
import java.util.*;
import java.util.NoSuchElementException;
// Quoted String Tokenizer
// Splits input into array of strings, delimited by delims[] characters
// Delims[] may include single characters, which act as simple delimiters
// ...or may include multiple characters, which will "quote" strings starting with
// the first characters of delims[i] and end with the final character
// Such quoted strings may contain any other character, including other delimiters
// If no "endquote" is found, then that "openquote" is considered as an ordinary non-delimiter character
// Consecutive whitespace delimiters are folded in with the preceeding delimiter.
// Quoting characters are considered as whitespace for folding purposes
// Presence of 'Comment' strings will cause discard of all remaining characters in the line, including the comment string
// Delimiters and comment characters may be treated as ordinary characters by escaping with 'escape' character
// Two styles of escaping:
//   simple: 
//       escape char only used to escape delimiters, or terminating quoted delimiter; other 'escape' chars treated as normal
//   unixStyle (default): 
//       paired escapes always condensed to single character; otherwise as simple
// Suports 'split'     for "String"-style interface
//         'nextToken' for "StringTokenizer"-style interface
//
public class QuotedStringTokenizer implements Enumeration {
	public int debugMode = 0;
	private String input;       // string being parsed in operation
	private int current;        // position of next input character
	private char prevDelim;     // used to compress consecutive whitespace delimitiers
	private String delims[];    // list of delimiters in operation
	private String comments[];  // list of comments in operation
	private char escape;        // escape character in 
	private boolean unixStyle;  // true for Unix-style escaping
	private static final String[] defaultDelims = { "\"\"", "''", " ", ",", "\t", "\n", "\r" };
	private static final String[] defaultComments = {};

	//----------------------------------------------------------------------------------------------------------
	// Constructors
	//----------------------------------------------------------------------------------------------------------
	public QuotedStringTokenizer() {
		this( null );
	}
	
	public QuotedStringTokenizer( String initDelims[] ) {
		this(initDelims, null);
	}
	
	public QuotedStringTokenizer( String initDelims[], String initComments[] ) {
		this(initDelims, initComments, '\\');
	}
	
	public QuotedStringTokenizer( String[] initDelims, String[] initComments, char initEscape ) {
		setDelims(initDelims);
		setComments(initComments);
		setEscape(initEscape);
		setUnixStyle(true);
	}
	
	//----------------------------------------------------------------------------------------------------------
	// Member variable access functions
	//----------------------------------------------------------------------------------------------------------
	public final String[] setDelims( String[] initDelims )
	{
		String[] ret = delims;
		delims = (null == initDelims) ? defaultDelims : initDelims;
		return ret;
	}
	public final String[] getDelims() { return delims; };
	
	public final String[] setComments( String[] initComments )
	{
		String[] ret = comments;
		comments = (null == initComments) ? defaultComments : initComments;
		return ret;
	}
	public final String[] getComments() { return comments; };

	public char setEscape( char initEscape )
	{
		char ret = escape;
		escape = initEscape;
		return ret;
	}
	public char getEscape() { return escape; }

	public boolean setUnixStyle(boolean newStyle)
	{
		boolean ret = unixStyle;
		unixStyle = newStyle;
		return ret;
	}
	public boolean getUnixStyle() { return unixStyle; }

	//----------------------------------------------------------------------------------------------------------
	// Public actions
	//----------------------------------------------------------------------------------------------------------
	
	// For enumeration interface
	public boolean hasMoreElements() { return hasMoreTokens(); };
	public Object nextElement() { return nextToken(); };
	
	// Cannot just check for more characters, as may be
	// trailing whitespace (if delimiters) or terminating comments
	public boolean hasMoreTokens()
	{
		boolean ret = false;
		// Scan from the current position for something that could be a token
		if( null!=input) {
			TrailLoop:
			for(int i=current; i<input.length(); i++ ) {
				// If char is a whitespace delimiter, then have not yet found a token
				char inchar = input.charAt(i);
				for(int j=0;j<delims.length;j++) if(inchar==delims[j].charAt(0) && Character.isWhitespace(inchar)) continue TrailLoop;
				// If first non-whitespace is a comment start, then definitely no token left
				String commentTest = input.substring(i);
				for(int j=0; j<comments.length; j++) if(commentTest.startsWith(comments[j])) break TrailLoop;
				// Something else, so will be the start of a new token
				ret = true;
				break;
			}
		}
		return ret;
	}
	
	public void setText(String input)
	{
		setText(input,0);
	}
	
	public void setText(String newInput, int start)
	{
		input = newInput;
		current = start;
		prevDelim = 'a'; // to allow leading null tokens
	}
	
	public String nextToken() throws NoSuchElementException
	{
		int tokstart = -1;
		int tokend = -1;         
		char terminatingDelim = '\0';
		boolean doEscape = false;
	
		String ret = "";
		parseLoop:
		if( !hasMoreTokens() )
			throw new NoSuchElementException();
		else for( ; current < input.length(); current++ )
		{
			    
			// First scan the current position for unescaped comment strings 
			// (can't just search full input string easily as comment strings are not comments
			// if inside paired delimiters. If comment string found, then terminate the line now
			int i=0;
			if (!doEscape) for( ; i<comments.length; i++) if( input.startsWith(comments[i],current) ) {
				if( debugMode >= 2 ) IJ.log(String.format("Found comment '%s' starting at %d", comments[i], current));
				tokend = current;
				current = input.length();
				break parseLoop;
			}
			
			// start scanning the list of delimiters
			for( i=0; i<delims.length; i++)
			{
				char startChar = delims[i].charAt(0);
				// We found an unescaped delimiter, so start processing the output.
				if( input.charAt(current) == startChar && !doEscape)
				{                    
					if( 1 < delims[i].length() )
					{
						// paired delimiter search for the terminator
						tokend = input.indexOf(delims[i].charAt(delims[i].length()-1), current+1);
						if( debugMode >= 3 ) IJ.log(String.format("First terminator for bracketing '%s' found at %d", delims[i], tokend));
						// if this is escaped, then carry on looking
						while( tokend > current+1 && escape == input.charAt(tokend-1))
						{
							// but don't treat that as an escape if it is itself escaped
							if( unixStyle )
							{
								boolean escaped = true;
								int j=tokend-2;
								for( ; j>current && input.charAt(j) == escape; j-- ) escaped = !escaped;
								if (!escaped) break;
							}
							else
								if( tokend > current+2 && escape == input.charAt(tokend-2)) break;
							tokend = input.indexOf(delims[i].charAt(delims[i].length()-1), tokend+1);
							if( debugMode >= 3 ) IJ.log(String.format("Next terminator found at %d", tokend));
						}
						// if we found the end delimiter, and there's no previous token in progress, accept the new string,
						// and pretend we're whitespace terminated to compress following delimiter
						if( tokend > current && 0 > tokstart)
						{
							tokstart = current+1;
							current = tokend;
							prevDelim = ' ';
							terminatingDelim = delims[i].charAt(delims[i].length()-1);  // record the delimiter we terminated with
							if( debugMode >= 2 ) IJ.log(String.format("Found quoted string: range=%d..%d",
													  tokstart, current-2));
						}
						// we found a valid quoted string, but there is a preceding token in progress,
						// return that one, we'll get this quoted string as the next token
						else if (tokend > current)
						{
							tokend = current;
							current--;
						}
						// if terminator not found, then pretend we found no delimiter
						// and when we break out below we'll treat it as a normal char
						else
						{
							if( debugMode >= 3 ) IJ.log(String.format("No terminator found for '%s', skipping to token completion", delims[i]));
							i = delims.length;
						}
					}
					// handling of unpaired delimiters
					else
					{
						if( debugMode >= 3 ) IJ.log(String.format("Unpaired delimiter '%s' found at %d", delims[i], current));
						if( tokstart >= 0 )  // token definition currently in progress
						{
							if( tokend < 0 ) tokend = current; // mark end of token, if not yet marked
							prevDelim = startChar;
						}
						else if( !Character.isWhitespace(startChar) ) 
						{
							if(!Character.isWhitespace(prevDelim)) tokstart = tokend = current;
							prevDelim = startChar;                            
						}
					}
					break;  // out to token completion
				}
			}
			// if we didn't find a delimiter, then this character is added to the string
			if( i >= delims.length )
			{
				if( tokstart < 0 ) tokstart = current;
				if( input.charAt(current) != escape )
					doEscape = false;
				else if (unixStyle)
					doEscape = !doEscape;
				else
					doEscape = (current > 0) && input.charAt(current-1) != escape;
				if( debugMode >= 3 ) IJ.log(String.format("Ordinary char '%c' found at %d, escape %s", input.charAt(current), current, doEscape ? "on" : "off"));
			}
			// if we completed a token, then return it
			if( tokstart >= 0 && tokend >= 0 )
			{
				break;
			}
		}
		if( debugMode >= 2) IJ.log("Parse loop ended");
		
		// Extract the parsed string
		if( tokstart >= 0 )
		{
			if (-1 == tokend) tokend = current;
			ret = deescape(input.substring(tokstart, tokend),terminatingDelim);
			if( debugMode >= 2 ) IJ.log(String.format("Added token '%s': range=%d..%d", 
													   ret, tokstart, tokend-1));
		}
		//if we had a final non-whitespace delim then may want a null token at the end
		//else if(!Character.isWhitespace(prevDelim))
		//    tokens.add("");
		current++;
		return ret;
	}
	
	public String[] split( String newString )
	{
		ArrayList<String> tokens = new ArrayList<String>();
		setText(newString, 0);
		while( hasMoreTokens() )
		{
			tokens.add(nextToken());
		}
		
		// Return the dynamic array to output string array        
		String[] output = new String[tokens.size()];
		if( tokens.size() > 0 )
		{
			if (debugMode >= 1) IJ.log(String.format("Returning %d tokens",tokens.size()));
			tokens.toArray(output);
		}
		else
			if (debugMode >= 1) IJ.log("Returning no tokens");
		return output;
	}
	
	// Remove any escape characters that were being used to escape delimiters in the supplied string
	// If a terminating delimiter is defined, then only remove escapes that escaped this char
	// Otherwise remove escapes escaping all delimiters
	private String deescape(String input, char terminatingDelim)
	{
		String output = "";
		int in_start = 0;
		int esc_end = 0;
		if( debugMode >= 3 ) IJ.log(String.format("De-escaping string '%s'", input));
		// find next '\' char
		while( -1 < (esc_end = input.indexOf(escape,esc_end)) ) {
			if( debugMode >= 3) IJ.log(String.format("Opening escape at %d",esc_end));
			// skip consecutive escape chars
			esc_end++;
			char esc_char = '\0';
			while( esc_end < input.length() && escape == (esc_char = input.charAt(esc_end))) {
				if( unixStyle ) break;  // unix_style, always reduce pairs of escapes to single
				esc_end++;
			}

			// Check the following character - if not a delimiter, then nothing to escape
			int i=0;
			if( esc_end == input.length() ) {
				// trailing escape, so we know we finished with a delimiter somehow, and no search required
				if( debugMode >= 3) IJ.log(String.format("Trailing escape before %d",esc_end));
			}
			else if (unixStyle && escape == esc_char) {
				// double escape, so let it condense to just one
				esc_end++;
				if( debugMode >= 3) IJ.log(String.format("Double escape before %d",esc_end));
			}    
			else if ('\0' != terminatingDelim) {
				if(esc_char == terminatingDelim) {
					if( debugMode >= 3) IJ.log(String.format("Escaped specific delimiter at %d",esc_end));
				}
				else {
				    // not single specified delimiter, so pretend we found no matches
					i = delims.length;
				}
			}
			else for( ; i<delims.length; i++ ) {
				if(esc_char == delims[i].charAt(0) || (delims[i].length() > 1 && esc_char == delims[i].charAt(delims[i].length()-1))) {
					if( debugMode >= 3) IJ.log(String.format("Escaped delimiter at %d",esc_end));
					break;
				}
			}
			if( i == delims.length && debugMode >= 3) IJ.log(String.format("Escape of non-delimiter before %d ignored",esc_end));
			// If not escaping a delimiter, also need to check for escaping comment strings
			if (i == delims.length && comments.length > 0) {
				String commentTest = input.substring(esc_end);
			    for( i=0; i<comments.length; i++ )
					if(commentTest.startsWith(comments[i])) {
						if( debugMode >= 3) IJ.log(String.format("Escaped comment at %d",esc_end));
					    break;
					}
				i = (i == comments.length) ? delims.length : 0;  // set no match or definitely matched
			}
			if (i == delims.length) continue;
		
			// Found something we need to escape. Just remove one escape char
			if( esc_end > in_start+1 ) {
				if( debugMode >= 3 ) IJ.log(String.format("Reducing escape string %d to before %d", in_start, esc_end));
				output = output.concat(input.substring(in_start,esc_end-1));
			}
			in_start = esc_end;   // and set start of next possible concatenation
		}
		if (in_start < input.length() )
			output = output.concat(input.substring(in_start));
		return output;
	}	
}
	
