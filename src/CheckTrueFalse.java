import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.util.*;

/**
 * @author james spargo
 *
 */
public class CheckTrueFalse {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if( args.length != 3){
			//takes three arguments
			System.out.println("Usage: " + args[0] +  " [wumpus-rules-file] [additional-knowledge-file] [input_file]\n");
			exit_function(0);
		}
		
		//create some buffered IO streams
		String buffer;
		BufferedReader inputStream;
		BufferedWriter outputStream;
		
		//create the knowledge base and the statement
		LogicalExpression knowledge_base = new LogicalExpression();
		LogicalExpression statement = new LogicalExpression();

		//open the wumpus_rules.txt
		try {
			inputStream = new BufferedReader( new FileReader( args[0] ) );
			
			//load the wumpus rules
			System.out.println("loading the wumpus rules...");
			knowledge_base.setConnective("and");
		
			while(  ( buffer = inputStream.readLine() ) != null ) 
                        {
				if( !(buffer.startsWith("#") || (buffer.equals( "" )) )) 
                                {
					//the line is not a comment
					LogicalExpression subExpression = readExpression( buffer );
					knowledge_base.setSubexpression( subExpression );
				} 
                                else 
                                {
					//the line is a comment. do nothing and read the next line
				}
			}		
			
			//close the input file
			inputStream.close();

		} catch(Exception e) 
                {
			System.out.println("failed to open " + args[0] );
			e.printStackTrace();
			exit_function(0);
		}
		//end reading wumpus rules
		
		
		//read the additional knowledge file
		try {
			inputStream = new BufferedReader( new FileReader( args[1] ) );
			
			//load the additional knowledge
			System.out.println("loading the additional knowledge...");
			
			// the connective for knowledge_base is already set.  no need to set it again.
			// i might want the LogicalExpression.setConnective() method to check for that
			//knowledge_base.setConnective("and");
			
			while(  ( buffer = inputStream.readLine() ) != null) 
                        {
                                if( !(buffer.startsWith("#") || (buffer.equals("") ))) 
                                {
					LogicalExpression subExpression = readExpression( buffer );
					knowledge_base.setSubexpression( subExpression );
                                } 
                                else 
                                {
				//the line is a comment. do nothing and read the next line
                                }
                          }
			
			//close the input file
			inputStream.close();

		} catch(Exception e) {
			System.out.println("failed to open " + args[1] );
			e.printStackTrace();
			exit_function(0);
		}
		//end reading additional knowledge
		
		knowledge_base.setConnective("and");
		// check for a valid knowledge_base
		if( !valid_expression( knowledge_base ) ) {
			System.out.println("invalid knowledge base");
			exit_function(0);
		}
		
		// print the knowledge_base
		knowledge_base.print_expression("\n");
		
		
		// read the statement file
		try {
			inputStream = new BufferedReader( new FileReader( args[2] ) );
			
			System.out.println("\n\nLoading the statement file...");
			//buffer = inputStream.readLine();
			
			// actually read the statement file
			// assuming that the statement file is only one line long
			while( ( buffer = inputStream.readLine() ) != null ) {
				if( !buffer.startsWith("#") ) {
					    //the line is not a comment
						statement = readExpression( buffer );
                                                break;
				} else {
					//the line is a commend. no nothing and read the next line
				}
			}
			
			//close the input file
			inputStream.close();

		} catch(Exception e) {
			System.out.println("failed to open " + args[2] );
			e.printStackTrace();
			exit_function(0);
		}
		// end reading the statement file
		
		// check for a valid statement
		if( !valid_expression( statement ) ) {
			System.out.println("invalid statement");
			exit_function(0);
		}
		
		//print the statement
		statement.print_expression( "" );
		//print a new line
		System.out.println("\n");
						
		//testing
		
		boolean entailsStatement = true;
		boolean entailsInverseStatement = true;

		// Boolean[0] is true/false boolean[1] is hardcoded or not So if true we edit
		HashMap<String, Boolean[]> symbols = getAllSymbols(knowledge_base, statement);
		int num_to_check = 0;
		for (Boolean[] b : symbols.values()) {
		    //System.out.println(b[1]);
		    if (b[1]) {
		    	num_to_check++;
		    }
		}
		for (int i = 0; i<Math.pow(2, num_to_check); i++) {
			int temp = i;
			for (String sym : symbols.keySet()) {
			    if (symbols.get(sym)[1]) {
				symbols.replace(sym, new Boolean[]{temp%2 != 0, symbols.get(sym)[1]});
				temp >>= 1;
			    }
			}
			
			boolean entailSt = entailsStatement(knowledge_base, statement, symbols);
			
			LogicalExpression inverse = new LogicalExpression();
			inverse.setConnective("not");
			inverse.setSubexpression(statement);
			
			boolean entailInv = entailsStatement(knowledge_base, inverse, symbols);
			
			
			if (!entailInv) {
				entailsInverseStatement = false;
			}
			if (!entailSt) {
				entailsStatement = false;
			}
			
		}
		
		

		// Definitly True
		try {
		    FileWriter fileWriter = new FileWriter("results.txt");
		    if ( entailsStatement && !entailsInverseStatement ) {
			fileWriter.write("definitely true:\nThis should be the output if the knowledge base entails the statement, and the knowledge base does not entail the negation of the statement.");
		    }
		    // Definitly False
		    if ( !entailsStatement && entailsInverseStatement ) {
			fileWriter.write("definitely false:\nThis should be the output if the knowledge base entails the negation of the statement, and the knowledge base does not entail the statement.");
		    }
		    // Possibly True of False
		    if ( !entailsStatement && !entailsInverseStatement ) {
			fileWriter.write("possibly true, possibly false:\nThis should be the output if the knowledge base entails neither the statement nor the negation of the statement.");
		    }
		    // True and False
		    if ( entailsStatement && entailsInverseStatement ) {
			fileWriter.write("both true and false: This should be the output if the knowledge base entails both the statement and the negation of the statement.");
		    }
		    fileWriter.close();
		} catch(Exception e) {
			System.out.println("failed to open results.txt\n");
			e.printStackTrace();
			exit_function(0);
		}

		

	} //end of main

	/* this method reads logical expressions
	 * if the next string is a:
	 * - '(' => then the next 'symbol' is a subexpression
	 * - else => it must be a unique_symbol
	 * 
	 * it returns a logical expression
	 * 
	 * notes: i'm not sure that I need the counter
	 * 
	 */
	public static LogicalExpression readExpression( String input_string ) 
        {
          LogicalExpression result = new LogicalExpression();
          
          //testing
          //System.out.println("readExpression() beginning -"+ input_string +"-");
          //testing
          //System.out.println("\nread_exp");
          
          //trim the whitespace off
          input_string = input_string.trim();
          
          if( input_string.startsWith("(") ) 
          {
            //its a subexpression
          
            String symbolString = "";
            
            // remove the '(' from the input string
            symbolString = input_string.substring( 1 );
            //symbolString.trim();
            
            //testing
            //System.out.println("readExpression() without opening paren -"+ symbolString + "-");
				  
            if( !symbolString.endsWith(")" ) ) 
            {
              // missing the closing paren - invalid expression
              System.out.println("missing ')' !!! - invalid expression! - readExpression():-" + symbolString );
              exit_function(0);
              
            }
            else 
            {
              //remove the last ')'
              //it should be at the end
              symbolString = symbolString.substring( 0 , ( symbolString.length() - 1 ) );
              symbolString.trim();
              
              //testing
              //System.out.println("readExpression() without closing paren -"+ symbolString + "-");
              
              // read the connective into the result LogicalExpression object					  
              symbolString = result.setConnective( symbolString );
              
              //testing
              //System.out.println("added connective:-" + result.getConnective() + "-: here is the string that is left -" + symbolString + "-:");
              //System.out.println("added connective:->" + result.getConnective() + "<-");
            }
            
            //read the subexpressions into a vector and call setSubExpressions( Vector );
            result.setSubexpressions( read_subexpressions( symbolString ) );
            
          } 
          else 
          {   	
            // the next symbol must be a unique symbol
            // if the unique symbol is not valid, the setUniqueSymbol will tell us.
            result.setUniqueSymbol( input_string );
          
            //testing
            //System.out.println(" added:-" + input_string + "-:as a unique symbol: readExpression()" );
          }
          
          return result;
        }

	/* this method reads in all of the unique symbols of a subexpression
	 * the only place it is called is by read_expression(String, long)(( the only read_expression that actually does something ));
	 * 
	 * each string is EITHER:
	 * - a unique Symbol
	 * - a subexpression
	 * - Delineated by spaces, and paren pairs
	 * 
	 * it returns a vector of logicalExpressions
	 * 
	 * 
	 */
	
	public static Vector<LogicalExpression> read_subexpressions( String input_string ) {

	Vector<LogicalExpression> symbolList = new Vector<LogicalExpression>();
	LogicalExpression newExpression;// = new LogicalExpression();
	String newSymbol = new String();
	
	//testing
	//System.out.println("reading subexpressions! beginning-" + input_string +"-:");
	//System.out.println("\nread_sub");

	input_string.trim();

	while( input_string.length() > 0 ) {
		
		newExpression = new LogicalExpression();
		
		//testing
		//System.out.println("read subexpression() entered while with input_string.length ->" + input_string.length() +"<-");

		if( input_string.startsWith( "(" ) ) {
			//its a subexpression.
			// have readExpression parse it into a LogicalExpression object

			//testing
			//System.out.println("read_subexpression() entered if with: ->" + input_string + "<-");
			
			// find the matching ')'
			int parenCounter = 1;
			int matchingIndex = 1;
			while( ( parenCounter > 0 ) && ( matchingIndex < input_string.length() ) ) {
					if( input_string.charAt( matchingIndex ) == '(') {
						parenCounter++;
					} else if( input_string.charAt( matchingIndex ) == ')') {
						parenCounter--;
					}
				matchingIndex++;
			}
			
			// read until the matching ')' into a new string
			newSymbol = input_string.substring( 0, matchingIndex );
			
			//testing
			//System.out.println( "-----read_subExpression() - calling readExpression with: ->" + newSymbol + "<- matchingIndex is ->" + matchingIndex );

			// pass that string to readExpression,
			newExpression = readExpression( newSymbol );

			// add the LogicalExpression that it returns to the vector symbolList
			symbolList.add( newExpression );

			// trim the logicalExpression from the input_string for further processing
			input_string = input_string.substring( newSymbol.length(), input_string.length() );

		} else {
			//its a unique symbol ( if its not, setUniqueSymbol() will tell us )

			// I only want the first symbol, so, create a LogicalExpression object and
			// add the object to the vector
			
			if( input_string.contains( " " ) ) {
				//remove the first string from the string
				newSymbol = input_string.substring( 0, input_string.indexOf( " " ) );
				input_string = input_string.substring( (newSymbol.length() + 1), input_string.length() );
				
				//testing
				//System.out.println( "read_subExpression: i just read ->" + newSymbol + "<- and i have left ->" + input_string +"<-" );
			} else {
				newSymbol = input_string;
				input_string = "";
			}
			
			//testing
			//System.out.println( "readSubExpressions() - trying to add -" + newSymbol + "- as a unique symbol with ->" + input_string + "<- left" );
			
			newExpression.setUniqueSymbol( newSymbol );
			
	    	//testing
	    	//System.out.println("readSubexpression(): added:-" + newSymbol + "-:as a unique symbol. adding it to the vector" );

			symbolList.add( newExpression );
			
			//testing
			//System.out.println("read_subexpression() - after adding: ->" + newSymbol + "<- i have left ->"+ input_string + "<-");
			
		}
		
		//testing
		//System.out.println("read_subExpression() - left to parse ->" + input_string + "<-beforeTrim end of while");
		
		input_string.trim();
		
		if( input_string.startsWith( " " )) {
			//remove the leading whitespace
			input_string = input_string.substring(1);
		}
		
		//testing
		//System.out.println("read_subExpression() - left to parse ->" + input_string + "<-afterTrim with string length-" + input_string.length() + "<- end of while");
	}
	return symbolList;
}


	/* this method checks to see if a logical expression is valid or not 
	 * a valid expression either:
	 * ( this is an XOR )
	 * - is a unique_symbol
	 * - has:
	 *  -- a connective
	 *  -- a vector of logical expressions
	 *  
	 * */
	public static boolean valid_expression(LogicalExpression expression)
	{
		
		// checks for an empty symbol
		// if symbol is not empty, check the symbol and
		// return the truthiness of the validity of that symbol

		if ( !(expression.getUniqueSymbol() == null) && ( expression.getConnective() == null ) ) {
			// we have a unique symbol, check to see if its valid
			return valid_symbol( expression.getUniqueSymbol() );

			//testing
			//System.out.println("valid_expression method: symbol is not empty!\n");
			}

		// symbol is empty, so
		// check to make sure the connective is valid
	  
		// check for 'if / iff'
		if ( ( expression.getConnective().equalsIgnoreCase("if") )  ||
		      ( expression.getConnective().equalsIgnoreCase("iff") ) ) {
			
			// the connective is either 'if' or 'iff' - so check the number of connectives
			if (expression.getSubexpressions().size() != 2) {
				System.out.println("error: connective \"" + expression.getConnective() +
						"\" with " + expression.getSubexpressions().size() + " arguments\n" );
				return false;
				}
			}
		// end 'if / iff' check
	  
		// check for 'not'
		else   if ( expression.getConnective().equalsIgnoreCase("not") ) {
			// the connective is NOT - there can be only one symbol / subexpression
			if ( expression.getSubexpressions().size() != 1)
			{
				System.out.println("error: connective \""+ expression.getConnective() + "\" with "+ expression.getSubexpressions().size() +" arguments\n" ); 
				return false;
				}
			}
		// end check for 'not'
		
		// check for 'and / or / xor'
		else if ( ( !expression.getConnective().equalsIgnoreCase("and") )  &&
				( !expression.getConnective().equalsIgnoreCase( "or" ) )  &&
				( !expression.getConnective().equalsIgnoreCase("xor" ) ) ) {
			System.out.println("error: unknown connective " + expression.getConnective() + "\n" );
			return false;
			}
		// end check for 'and / or / not'
		// end connective check

	  
		// checks for validity of the logical_expression 'symbols' that go with the connective
		for( Enumeration e = expression.getSubexpressions().elements(); e.hasMoreElements(); ) {
			LogicalExpression testExpression = (LogicalExpression)e.nextElement();
			
			// for each subExpression in expression,
			//check to see if the subexpression is valid
			if( !valid_expression( testExpression ) ) {
				return false;
			}
		}

		//testing
		//System.out.println("The expression is valid");
		
		// if the method made it here, the expression must be valid
		return true;
	}
	
	public static HashMap<String, Boolean[]> getAllSymbols(LogicalExpression knowledge_base, LogicalExpression statement) {
		HashMap<String, Boolean[]> knowledgeBaseSymbols = addSymbolsToMap(knowledge_base);
		HashMap<String, Boolean[]> statementSymbols = addSymbolsToMap(statement);
		HashMap<String, Boolean[]> symbols = new HashMap<String, Boolean[]>();
		
		for (String sym : knowledgeBaseSymbols.keySet()) {
			if (!symbols.containsKey(sym)) {
				symbols.put(sym, new Boolean[]{null, true});
			}
		}
		for (String sym : statementSymbols.keySet()) {
			if (!symbols.containsKey(sym)) {
				symbols.put(sym, new Boolean[]{null, true});
			}
		}
		symbols = setRequiredSymbols(symbols, knowledge_base);
		return symbols;
	}
	
	public static HashMap<String, Boolean[]> setRequiredSymbols(HashMap<String, Boolean[]> symbs, LogicalExpression knowledge_base) {
		
		for (LogicalExpression rule : knowledge_base.getSubexpressions()) {
			if (rule.getConnective() == null) {
				if (symbs.containsKey(rule.getUniqueSymbol())) {
					 if(symbs.get(rule.getUniqueSymbol())[1] == false && symbs.get(rule.getUniqueSymbol())[0] == false) {
						 //we found a contradiction
					 }
					symbs.replace(rule.getUniqueSymbol(), new Boolean[]{true, false});
				}
			}
			else if (rule.getConnective().toLowerCase().equals("not")) {
				if (rule.getSubexpressions().get(0).getUniqueSymbol() != null && symbs.containsKey(rule.getSubexpressions().get(0).getUniqueSymbol())) {
					if(symbs.get(rule.getUniqueSymbol()) != null && symbs.get(rule.getUniqueSymbol())[1] == false && symbs.get(rule.getUniqueSymbol())[0] == true) {
						 //we found a contradiction
					 }
					symbs.replace(rule.getSubexpressions().get(0).getUniqueSymbol(), new Boolean[]{false, false});
				}
			}
			else if (rule.getConnective().toLowerCase().equals("if")) {
				if (isLogicalExpressionTrue(rule.getSubexpressions().get(0), symbs) != null && isLogicalExpressionTrue(rule.getSubexpressions().get(0), symbs)) {
					symbs = setRequiredSymbols(symbs, rule.getSubexpressions().get(1));
				}
			}
			else if (rule.getConnective().toLowerCase().equals("and")) {
				
			}
			else if (rule.getConnective().toLowerCase().equals("xor")) {
				
			}
		}
		return symbs;
	}

	
	public static HashMap<String, Boolean[]> addSymbolsToMap(LogicalExpression expression) {
		HashMap<String, Boolean[]> symbols = new HashMap<String, Boolean[]>();
		
		if (expression.getConnective() == null) {
			symbols.put(expression.getUniqueSymbol(), null);
			return symbols;
		}
		
		for (LogicalExpression subExpression : expression.getSubexpressions()) {
			HashMap<String, Boolean[]> newSymbols = addSymbolsToMap(subExpression);
			
			for (String symbol : newSymbols.keySet()) {
				if (!symbols.containsKey(symbol)) {
					symbols.put(symbol, null);
				}
			}
		}
		
		return symbols;
	}
	

	/** this function checks to see if a unique symbol is valid */
	//////////////////// this function should be done and complete
	// originally returned a data type of long.
	// I think this needs to return true /false
	//public long valid_symbol( String symbol ) {
	public static boolean valid_symbol( String symbol ) {
		if (  symbol == null || ( symbol.length() == 0 )) {
			
			//testing
			//System.out.println("String: " + symbol + " is invalid! Symbol is either Null or the length is zero!\n");
			
			return false;
		}

		for ( int counter = 0; counter < symbol.length(); counter++ ) {
			if ( (symbol.charAt( counter ) != '_') &&
					( !Character.isLetterOrDigit( symbol.charAt( counter ) ) ) ) {
				
				System.out.println("String: " + symbol + " is invalid! Offending character:---" + symbol.charAt( counter ) + "---\n");
				
				return false;
			}
		}
		
		// the characters of the symbol string are either a letter or a digit or an underscore,
		//return true
		return true;
	}

    private static void exit_function(int value) {
    	System.out.println("exiting from checkTrueFalse");
    	System.exit(value);
    }
    
    //returns true if our knowledgebase and statement are true with the given set of boolean assignments
    //otherwise returns false
    public static boolean entailsStatement(LogicalExpression kb, LogicalExpression statement, HashMap<String, Boolean[]> booleanAssignments) {
    	if(!isLogicalExpressionTrue(kb, booleanAssignments)) {
    		return true;
    	}
    	return (isLogicalExpressionTrue(statement, booleanAssignments));
    }
    
    //returns true if the logical expression is true for the given set of symbols
    //returns false if the expression is false, or if we somehow have an invalid connective
    public static Boolean isLogicalExpressionTrue(LogicalExpression kb, HashMap<String, Boolean[]> symbols) {
    	if(kb.getConnective() == null) {
    		return symbols.get(kb.getUniqueSymbol())[0];
    	}
    	switch(kb.getConnective().toLowerCase()) {
    		case "and":
    			for(LogicalExpression expression : kb.getSubexpressions()) {
    				Boolean isTrue = isLogicalExpressionTrue(expression, symbols);
    				if(isTrue == null) {
    					return null;
    				}
    				if(!isTrue) {
    					return false;
    				}
    			}
    			return true;
    		case "or":
    			int numNull = 0;
    			for(LogicalExpression expression : kb.getSubexpressions()) {
    				Boolean isTrue = isLogicalExpressionTrue(expression, symbols);
    				if(isTrue == null) {
    					numNull++;
    				}
    				if(isTrue) {
    					return true;
    				}
    			}
    			if(numNull == kb.getSubexpressions().size()) {
    				return null;
    			}
    			return false;
    		case "xor":
    			boolean trueFound = false;
    			for(LogicalExpression expression : kb.getSubexpressions()) {
    				Boolean isTrue = isLogicalExpressionTrue(expression, symbols);
    				if(isTrue == null) {
    					return null;
    				}
    				if(isTrue) {
    					if(!trueFound) {
    						trueFound = true;
    					}
    					else {
    						return false;
    					}
    				}
    			}
    			return trueFound;
    		case "not":
    			Boolean isTrue = isLogicalExpressionTrue(kb.getSubexpressions().get(0), symbols);
    			if(isTrue == null) {
    				return null;
    			}
    			return !isTrue;
    		case "if":
    			Boolean isTrue1 = isLogicalExpressionTrue(kb.getSubexpressions().get(0), symbols);
    			Boolean isTrue2 = isLogicalExpressionTrue(kb.getSubexpressions().get(1), symbols);
    			if(isTrue1 == null || isTrue2 == null) {
    				return null;
    			}
    			return (!isTrue1 || isTrue2);
    		case "iff":
    			Boolean isTrue3 = isLogicalExpressionTrue(kb.getSubexpressions().get(0), symbols);
    			Boolean isTrue4 = isLogicalExpressionTrue(kb.getSubexpressions().get(1), symbols);
    			if(isTrue3 == null || isTrue4 == null) {
    				return null;
    			}
    			return ((isTrue3 && isTrue4) || (!isTrue3 && !isTrue4));
    	}
    	return false;
    }
}
