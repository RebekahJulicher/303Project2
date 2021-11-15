package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
	static HashMap<String, Pattern> patterns;
	static ArrayList< HashMap<String, Integer> > vars;
	static int indentationLevel = 0;
	
	public static void main(String[] args) {
		patterns = new HashMap<String, Pattern>();
		vars = new ArrayList< HashMap<String, Integer> >();
		setUpPatterns();
		try {
			File file = new File(args[0]);
			Scanner in = new Scanner(file);
			boolean commentBlock = false;
			while (in.hasNextLine()) {
				String line = in.nextLine().trim();
				if (Pattern.matches("\\S+#.*", line)) // remove trailing comment from line
					line = line.split("#", 2)[0];

				boolean ignore = false;
				if (commentBlock)
					ignore = true;
				if (line.charAt(0) == '$')
					commentBlock = !commentBlock;
				if (line.charAt(0) == '#' && !commentBlock)
					ignore = true;

				if (!ignore)
					translate(line, IDPattern(line));
				
				if (indentationLevel < 0) 
					System.out.println("SYNTAX ERROR: More \"end\" statements than \"start\" statements in code.");
				if (indentationLevel > 0) 
					System.out.println("SYNTAX ERROR: More \"start\" statements than \"end\" statements in code.");

			}
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " could not be found!");
			e.printStackTrace();
		}
	}
	
	private static void defineVar(String line, String[] args){
		String[] strArray = line.split(" ");
		String content = "";
		for (int i = 3; i < strArray.length; i++) content += strArray[i];
		if (vars.get(vars.size()-1).get(strArray[1]) != null) {  // If name already exists, error
				System.out.println("SYNTAX ERROR: Var " + strArray[1] + " has already been" +
									"defined, cannot define it again!");
				System.exit(1);
		}
		
		// Checking for boolean expression
		if (line.contains("==") || line.contains("<=") || line.contains(">=") || line.contains("<>") ||
			line.contains(">") || line.contains("<") || line.contains(" and ") ||
			line.contains(" not ") || line.contains(" or ")){
			// HANDLE VARS/COMMAND LINE ARGS
			vars.get(vars.size() - 1).put(strArray[1], 1);
			// TODO: Write appropriate thing to file
			return;
		}
		
		// Checking for int expression
		if (line.contains("+") || line.contains("-") || line.contains("/") || line.contains("*") ||
				line.contains("%") || line.contains("-") || isInt(strArray[3])){
			
			if (!checkIntExpr(content, args)) System.exit(1);
			vars.get(vars.size() - 1).put(strArray[1], 0);
			// TODO: Write appropriate thing to file
			return;
		}
		
		//HANDLE VARS AND COMMAND LINE ARGS
		if (patterns.get("commandLineArg").matcher(strArray[3]).matches()) {
			if (args.length <= Integer.valueOf(strArray[3].charAt(5))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
				System.out.println("SYNTAX ERROR: Invalid arg index");
				System.exit(1);
			}
			if (patterns.get("number").matcher(args[Integer.valueOf(strArray[3].charAt(5))]).matches()) {
				vars.get(vars.size() - 1).put(args[Integer.valueOf(strArray[3].charAt(5))], 0);
			}
			else if (args[Integer.valueOf(strArray[3].charAt(5))].equals("true") ||
						args[Integer.valueOf(strArray[3].charAt(5))].equals("false")){
				vars.get(vars.size() - 1).put(args[Integer.valueOf(strArray[3].charAt(5))], 1);
			}
			else {
				System.out.println("SYNTAX ERROR: Command line argument input is not int or boolean");
				System.exit(1);
			}
			// TODO: Write appropriate thing to file
			return;
		}
		
		// Checking if we're defining var in terms of another var
		for (HashMap<String, Integer> x : vars){
			if (x.get(strArray[1]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				Integer original = x.get(strArray[3]);
				vars.get(vars.size() - 1).put(strArray[1], original);
				// TODO: Write appropriate thing to file
				return;
			}
		}
		System.out.println("SYNTAX ERROR: Variable assignment to nonexistent variable");
		System.exit(1);
	}
	
	
	private static boolean isOp(char item) {
		return item == '+' || item == '-' || item == '*' || item == '/' || item == '%';
	}
	
	
	// TODO: THIS CURRENTLY DOES NOT ACCOUNT FOR EXTRA SPACES BETWEEN OPS AND PARENTHESES
	private static boolean checkIntExpr(String line, String[] args) {
		int numOpenPar = 0;
		int numClosedPar = 0;
		boolean precededByVal = false;
		String soFar = "";
		// Checking for equal parentheses and removing them from the issue
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '(') {
				if (i < line.length()-1 && isOp(line.charAt(i+1))){
					System.out.println("SYNTAX ERROR: Operator directly after parenthesis");
					return false;
				}
				numOpenPar++;
			}
			else if (c == ')') {
				if (i > 0 && isOp(line.charAt(i-1))){
					System.out.println("SYNTAX ERROR: Operator directly after parenthesis");
					return false;
				}
				numClosedPar++;
				if (numClosedPar > numOpenPar) {
					System.out.println("SYNTAX ERROR: Unmatched parentheses");
					return false;
				}
			}
			else soFar += c;
		}
		if (numOpenPar != numClosedPar) {
			System.out.println("SYNTAX ERROR: Unmatched parentheses");
			return false;
		}
		
		// Handling value/operator pairing checking
		String[] strArray = soFar.split(" ");
		for (int i = 0; i < strArray.length; i++) {
			// If curr string is operator
			if (isOp(strArray[i].charAt(0))) {
				if (!precededByVal) {
					System.out.println("SYNTAX ERROR: Operator not preceded by value");
					return false;
				}
				precededByVal = false;
			}
			// If curr string is number
			else if (patterns.get("number").matcher(strArray[i]).matches()) {
				if (precededByVal) {
					System.out.println("SYNTAX ERROR: Value not preceded by operator");
					return false;
				}
				precededByVal = true;
			}
			// If curr string is command line argument
			else if (patterns.get("commandLineArg").matcher(strArray[i]).matches()) {
				if (args.length <= Integer.valueOf(strArray[i].charAt(5))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
					System.out.println("SYNTAX ERROR: Invalid arg index");
					return false;
				}
				if (patterns.get("number").matcher(args[Integer.valueOf(strArray[i].charAt(5))]).matches()) {
					if (precededByVal) {
						System.out.println("SYNTAX ERROR: Value not preceded by operator");
						return false;
					}
					precededByVal = true;
				}
			}
			// Else if curr string can only otherwise be a variable name
			else {
				if (patterns.get("number").matcher(strArray[i]).matches()) {
					if (precededByVal) {
						System.out.println("SYNTAX ERROR: Value not preceded by operator");
						return false;
					}
					precededByVal = true;
				}
				for (HashMap<String, Integer> x : vars){
					if (x.get(strArray[i]) != null && !precededByVal) precededByVal = true;
					else {
						System.out.println("SYNTAX ERROR: Integer expression contains nonexistent variable");
						return false;
					}
				}
			}
		}
		if (!precededByVal) System.out.println("SYNTAX ERROR: Integer expression does not end with val");
		return precededByVal;
	}

	private static void setVar(String line){
		String[] strArray = line.split(" ");
		Integer type = vars.get(vars.size()-1).get(strArray[1]);
		
		if (type == null){
			System.out.println("SYNTAX ERROR: Var " + strArray[0] + " has not been" +
									"defined, cannot set it!");
			System.exit(1);
		}
		
		if (line.contains("==") || line.contains("<=") || line.contains(">=") || line.contains("<>") ||
			line.contains(">") || line.contains("<") || line.contains(" and ") ||
			line.contains(" not ") || line.contains(" or ")){
			if (type != 1){
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of" + 
									"type boolean, so it cannot be set to a boolean!");
				System.exit(1);
			}
			// TODO: Write appropriate thing to file
			return;
		}
		
		if (line.contains("+") || line.contains("-") || line.contains("/") || line.contains("*") ||
				line.contains("%") || line.contains("-") || isInt(strArray[3])){
			if (type != 0){
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of" + 
									"type int, so it cannot be set to an int!");
				System.exit(1);
			}
			// TODO: Write appropriate thing to file
			return;
		}
		
		// TODO: Handle variables
		/*
		// Checking if we're defining var in terms of another var
		for (HashMap<String, VarContent> x : vars){
			if (x.get(strArray[2]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				VarContent original = x.get(strArray[2]);
				if (original.type == content.type){
					if (original.type == 0) content.integerVal = original.integerVal;
					else content.boolVal = original.boolVal;
					return;
				}
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of the " +
									"same type as var " + strArray[2]);
				System.exit(1);
			}
		}
		System.out.println("SYNTAX ERROR: Invalid variable declaration.");
		System.exit(1);
		// TODO: ERROR OUT
		 */
	}
	
	private static boolean isInt(String expr) {
		if (expr == null)
			return false;
		try {
			int x = Integer.parseInt(expr);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	private static boolean isIf(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("if"))
			return true;
		return false;
	}

	private static boolean isElseIf(String expr) {
		String words[] = expr.split(expr);
		if ((words[0] + words[1]).equals("elseif"))
			return true;
		return false;
	}
	
	private static boolean isElse(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("else"))
			return true;
		return false;
	}


	private static void setUpPatterns() {
		Pattern number = Pattern.compile("-?\\d+(\\.\\d+)?");
		patterns.put("number", number);
		//String comment = "$+[\\d*[\\w&&[^\\d]]*]*";
		String comment = "$.*";
		//String string = "\"[\\d*[\\w&&[^\\d]]*]*\"";
		String string = "\".*\"";
		
		String character = "'.'";

		String digit = "\\d";
		String bool = "true|false";
		Pattern integer = Pattern.compile(digit + "+");
		patterns.put("int", integer);
		
		String commandLineArg = "$arg[" + integer + "]";
		
		String variable = "[\\w&&[^\\d]]+[\\d*[\\w&&[^\\d]]*]*";
		// value is going to need bool_expr, i_expr, and string
		String value = "(" + integer + "|" + bool + "|" + commandLineArg + "|" + variable + ")";
		
		Pattern var_assgn = Pattern.compile("var " + variable + " = " + value);
		patterns.put("var_assgn", var_assgn);
		
		// Need to make i_expr code without breaking java
		Pattern commu_op = Pattern.compile("[+-]");
		Pattern assoc_op = Pattern.compile("[*/%]");
		String expr_root_string = "(" + variable + "|" + integer + ")";
		Pattern un_expr = Pattern.compile("(~" + expr_root_string + "|" + expr_root_string + ")");
		Pattern assoc_expr = Pattern.compile(un_expr + "(" + assoc_op + un_expr + ")*");
		Pattern i_expr = Pattern.compile(assoc_expr + "(" + commu_op + assoc_expr + ")*");
		
		expr_root_string = "(" + variable + "|" + integer + "|" + i_expr + ")";
		Pattern expr_root = Pattern.compile(expr_root_string);
		// expr_root string outdated

		// Need to make bool_expr code without breaking java

		String print_matter = ""; // fill this out
		Pattern print = Pattern.compile("print(" + print_matter + ")");
		patterns.put("print", print);
		
		// In the end we need to add all the patterns into the patterns collection
	}
	
	private static String IDPattern(String line) {
		if (isIf(line))
			return "if";
		if (isElseIf(line))
			return "else_if";
		if (isElse(line))
			return "else";
		 // This is where we iterate through the patterns and find a match
		for (String patternName : patterns.keySet()) {
			Pattern tempPatter = patterns.get(patternName);
			Matcher m = tempPatter.matcher(line);
			if (m.matches())
				return patternName;
		}
		return null;
	}

	private static void translate(String line, String string) {
		// Want to output the correct translation to Java

		
		// output translation to file
	}
	
	

	//THIS IS NOT NECESSARY, WE COULD MAP THIS TO AN INT ABOVE
	static class VarContent {
		// 0 for int, 1 for boolean
		int type;
		public VarContent(int type){
			this.type = type;
		}

	}

}