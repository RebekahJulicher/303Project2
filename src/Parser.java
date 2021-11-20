package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
	static int lineNum = 0;
	static HashMap<String, Pattern> patterns;
	static ArrayList< HashMap<String, Integer> > vars;
	static int indentationLevel = 0;
	static File javaFile = null;
	static FileWriter writer = null;
	
	public static void main(String[] args) throws IOException {
		patterns = new HashMap<String, Pattern>();
		vars = new ArrayList< HashMap<String, Integer> >();
		setUpPatterns();

		try {
			javaFile = new File(args[0] + "java");
			writer = new FileWriter(javaFile.getName());
			if (javaFile.createNewFile()) 
				System.out.println("File created: " + javaFile.getName());
			else
				System.out.println("File already exists");
		} catch (IOException e) {
			System.out.println("Error making file");
			e.printStackTrace();
		}

		try {
			File file = new File(args[0]);
			Scanner in = new Scanner(file);
			boolean commentBlock = false;
			
			writer.write("public class ParsedCode {\n\tpublic static void main(String[] args) {");
			
			while (in.hasNextLine()) {
				lineNum++;
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
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: More \"end\" statements than \"start\" statements in code.");
				if (indentationLevel > 0) 
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: More \"start\" statements than \"end\" statements in code.");

			}
			in.close();
			writer.write("\t}\n}");
		} catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " could not be found!");
			e.printStackTrace();
		}
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
	
	private static boolean isWhile(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("while"))
			return true;
		return false;
	}

	private static boolean isFor(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("for"))
			return true;
		return false;
	}

	private static boolean isPrint(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("print"))
			return true;
		return false;
	}
	
	private static boolean isStart(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("start")) {
			indentationLevel++;
			return true;
		}
		return false;
	}

	private static boolean isEnd(String expr) {
		String words[] = expr.split(expr);
		if (words[0].equals("end")) {
			indentationLevel--;
			return true;
		}
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
		if (isWhile(line))
			return "while";
		if (isFor(line))
			return "for";
		if (isPrint(line))
			return "print";
		if (isStart(line))
			return "start";
		if (isEnd(line))
			return "end";

		 // This is where we iterate through the patterns and find a match
		for (String patternName : patterns.keySet()) {
			Pattern tempPatter = patterns.get(patternName);
			Matcher m = tempPatter.matcher(line);
			if (m.matches())
				return patternName;
		}
		return null;
	}

	private static void translate(String line, String patternName) throws IOException {
		// output translation to file
		System.out.println("Translating line: " + line);
		if (patternName.equals("if"))
			writer.write(translatedIf(line));
		if (patternName.equals("else_if"))
			writer.write(translatedElseIf(line));
		if (patternName.equals("else"))
			writer.write(translatedElse(line));
		if (patternName.equals("while"))
			writer.write(translatedWhile(line));
		if (patternName.equals("for"))
			writer.write(translatedFor(line));
		if (patternName.equals("print"))
			writer.write(translatedPrint(line));
		if (patternName.equals("start"))
			writer.write(translatedStart(line));
		if (patternName.equals("end"))
			writer.write(translatedEnd(line));

		System.out.println("Line translated");
			
	}

	private static String translatedIf(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		return "if (" + arg + ")";
	}

	private static String translatedElse(String line) {
		return "else ";
	}

	private static String translatedElseIf(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		return "else if (" + arg + ")";
	}

	private static String translatedWhile(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		return "while (" + arg + ")";
	}

	private static String translatedFor(String line) {
		String[] words = line.split(" ");
		String counter = words[1];
		String start = words[3];
		int startInt = Integer.parseInt(start);
		String end = words[5];
		int endInt = Integer.parseInt(end);
		boolean exists = false;
		for (HashMap<String, Integer> variableSet : vars) {
			if (variableSet.containsKey(counter)) {
				exists = true;
				if (variableSet.get(counter) != 0) {
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Can't pass in a non-integer as a counter variable in a for-loop.");
					System.exit(1);
				}
			}
		}
		
		String addOn = exists ? "" : "int ";
		// need to see how this performs when startInt == endInt
		if (startInt < endInt)  
			return "for (" + addOn + counter + " = " + start + "; " + counter + " < " + end + "; " + counter + "++)";
		else if (startInt > endInt)  
			return "for (" + addOn + counter + " = " + start + "; " + counter + " > " + end + "; " + counter + "--)";
		else
			return "for (" + addOn + counter + " = " + start + "; " + counter + " == " + end + "; " + counter + "++)";
	}

	private static String translatedPrint(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		return "System.out.print(" + arg + ");";
	}

	private static String translatedStart(String line) {
		return "{";
	}
	
	private static String translatedEnd(String line) {
		return "}";
	}

	private static boolean isBoolExpr(String line) {
		return line.contains("==") || line.contains("<=") || line.contains(">=") || line.contains("<>") ||
		line.contains(">") || line.contains("<") || line.contains(" and ") || line.contains(" not ") ||
		line.contains(" or ") || line.contains("true") || line.contains("false");
	}
	
	private static boolean containsIntExpr(String line) {
		String[] strArray = line.split(" ");
		return line.contains("+") || line.contains("-") || line.contains("/") || line.contains("*") ||
		line.contains("%") || line.contains("-") || isInt(strArray[3]);
	}
	
	private static String handleExpr(boolean isDefinition, boolean isBool, String content, String name, String[] args) {
		Integer type = vars.get(vars.size()-1).get(name);
		
		if (isDefinition && isBool) {
			if (!checkBoolExpr(content, args)) System.exit(1);
			vars.get(vars.size() - 1).put(name, 1);
			return "boolean " + name + " = " + content + ";";
		}
		else if (isDefinition && !isBool) {
			if (!checkIntExpr(content, args)) System.exit(1);
			vars.get(vars.size() - 1).put(name, 0);
			return "int " + name + " = " + content + ";";
		}
		else if (!isDefinition && isBool) {
			if (type != 1){
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + name + 
						" is not of type boolean, so it cannot be set to a boolean!");
				System.exit(1);
			}
			if (!checkBoolExpr(content, args)) System.exit(1);
			return name + " = " + content + ";";
		}
		else {
			if (type != 0){
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + name + 
						" is not of type int, so it cannot be set to an int!");
				System.exit(1);
			}
			if (!checkIntExpr(content, args)) System.exit(1);
			return name + " = " + content + ";";
		}
	}
	
	private static String defineVar(String line, String[] args){
		String[] strArray = line.split(" ");
		String content = "";
		for (int i = 3; i < strArray.length; i++) content += strArray[i];
		if (vars.get(vars.size()-1).get(strArray[1]) != null) {  // If name already exists, error
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[1] + " has already been " +
									"defined, cannot define it again!");
				System.exit(1);
		}
		
		// Checking for boolean expression
		if (isBoolExpr(line)) return handleExpr(true, true, content, strArray[1], args);
		
		// Checking for int expression
		if (containsIntExpr(line)) return handleExpr(true, false, content, strArray[1], args); 
		
		//HANDLE VARS AND COMMAND LINE ARGS
		if (patterns.get("commandLineArg").matcher(strArray[3]).matches()) {
			if (args.length <= Integer.valueOf(strArray[3].charAt(5))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid arg index");
				System.exit(1);
			}
			if (patterns.get("number").matcher(args[Integer.valueOf(strArray[3].charAt(5))]).matches()) {
				
				vars.get(vars.size() - 1).put(args[Integer.valueOf(strArray[3].charAt(5))], 0);
				return "int " + strArray[1] + " = Integer.valueOf(" + strArray[3] + ");";
			}
			else if (args[Integer.valueOf(strArray[3].charAt(5))].equals("true") ||
						args[Integer.valueOf(strArray[3].charAt(5))].equals("false")){
				
				vars.get(vars.size() - 1).put(args[Integer.valueOf(strArray[3].charAt(5))], 1);
				return "boolean " + strArray[1] + " = " + strArray[3] + ";";
			}
			else {
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Command line argument input is not int");
				System.exit(1);
			}
		}
		
		// Checking if we're defining var in terms of another var
		for (HashMap<String, Integer> x : vars){
			if (x.get(strArray[1]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				Integer original = x.get(strArray[3]);
				vars.get(vars.size() - 1).put(strArray[1], original);
				if (original == 0) return "int " + strArray[1] + " = " + strArray[3] + ";";
				return "boolean " + strArray[1] + " = " + strArray[3] + ";";
			}
		}
		System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Variable assignment to nonexistent variable");
		System.exit(1);
		return "\\";
	}
	
	
	private static boolean isOp(char item) {
		return item == '+' || item == '-' || item == '*' || item == '/' || item == '%';
	}

	private static String setVar(String line, String[] args){
		String[] strArray = line.split(" ");
		Integer type = vars.get(vars.size()-1).get(strArray[1]);
		String content = "";
		for (int i = 3; i < strArray.length; i++) content += strArray[i];
		
		if (type == null){
			System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[1] + " has not been" +
									"defined, cannot set it!");
			System.exit(1);
		}
		
		if (isBoolExpr(line)) return handleExpr(false, true, content, strArray[0], args);
		
		if (containsIntExpr(line)) return handleExpr(false, false, content, strArray[0], args);
		
		//HANDLE VARS AND COMMAND LINE ARGS
		if (patterns.get("commandLineArg").matcher(strArray[3]).matches()) {
			if (args.length <= Integer.valueOf(strArray[3].charAt(5))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid arg index");
				System.exit(1);
			}
			if (patterns.get("number").matcher(args[Integer.valueOf(strArray[3].charAt(5))]).matches()) {
				if (type != 0){
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[0] + " is not of " + 
										"type int, so it cannot be set to an int!");
					System.exit(1);
				}
				return strArray[1] + " = " + content + ";";
			}
			else if (args[Integer.valueOf(strArray[3].charAt(5))].equals("true") ||
						args[Integer.valueOf(strArray[3].charAt(5))].equals("false")){
				
				if (type != 1){
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[0] + " is not of " + 
										"type boolean, so it cannot be set to a boolean!");
					System.exit(1);
				}
				return strArray[1] + " = " + content + ";";
			}
			else {
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Command line argument input is not int");
				System.exit(1);
			}
		}
		
		// Checking if we're defining var in terms of another var
		for (HashMap<String, Integer> x : vars){
			if (x.get(strArray[1]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				Integer newVal = x.get(strArray[3]);
				if (!type.equals(newVal)){
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[0] + " is not of" + 
										" the same type as var "+ strArray[3] + ", invalid!");
					System.exit(1);
				}
				return strArray[1] + " = " + strArray[3] + ";";
			}
		}
		System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Variable assignment to nonexistent variable");
		System.exit(1);
		return "\\";
	}
	
	// THIS CURRENTLY DOES NOT ACCOUNT FOR EXTRA SPACES BETWEEN OPS AND PARENTHESES
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
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Operator directly after parenthesis");
						return false;
					}
					numOpenPar++;
				}
				else if (c == ')') {
					if (i > 0 && isOp(line.charAt(i-1))){
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Operator directly after parenthesis");
						return false;
					}
					numClosedPar++;
					if (numClosedPar > numOpenPar) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Unmatched parentheses");
						return false;
					}
				}
				else soFar += c;
			}
			if (numOpenPar != numClosedPar) {
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Unmatched parentheses");
				return false;
			}
			
			// Handling value/operator pairing checking
			String[] strArray = soFar.split(" ");
			for (int i = 0; i < strArray.length; i++) {
				// If curr string is operator
				if (isOp(strArray[i].charAt(0))) {
					if (!precededByVal) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Operator not preceded by value");
						return false;
					}
					precededByVal = false;
				}
				// If curr string is number
				else if (patterns.get("number").matcher(strArray[i]).matches()) {
					if (precededByVal) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Value not preceded by operator");
						return false;
					}
					precededByVal = true;
				}
				// If curr string is command line argument
				else if (patterns.get("commandLineArg").matcher(strArray[i]).matches()) {
					if (args.length <= Integer.valueOf(strArray[i].charAt(5))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid arg index");
						return false;
					}
					if (patterns.get("number").matcher(args[Integer.valueOf(strArray[i].charAt(5))]).matches()) {
						if (precededByVal) {
							System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Value not preceded by operator");
							return false;
						}
						precededByVal = true;
					}
				}
				// Else if curr string can only otherwise be a variable name
				else {
					if (patterns.get("number").matcher(strArray[i]).matches()) {
						if (precededByVal) {
							System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Value not preceded by operator");
							return false;
						}
						precededByVal = true;
					}
					for (HashMap<String, Integer> x : vars){
						if (x.get(strArray[i]) != null && !precededByVal) precededByVal = true;
						else {
							System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Integer expression contains nonexistent variable");
							return false;
						}
					}
				}
			}
			if (!precededByVal) System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Integer expression does not end with val");
			return precededByVal;
		}

	private static boolean checkBoolExpr(String expr, String[] args) {
		String[] parts = expr.split(" ");

		String[] boolOpsArray = {"and", "or", "not"};
		List<String> boolOps = Arrays.asList(boolOpsArray);
		String[] compOpsArray = {"==", "<=", ">=", "!=", "<>"};
		List<String> compOps = Arrays.asList(compOpsArray);
		String[] boolValsArray = {"true", "false"};
		List<String> boolVals = Arrays.asList(boolValsArray);
		
		boolean currIsInt = false;
		boolean precededByVal = false; // For making sure we're following val op val op

		String soFar = "";
		int openPar = 0;
		int closedPar = 0;
		for (int i = 0; i < parts.length; i++) {
			String curr = parts[i];
			if (curr.charAt(0) == '(') {
				openPar++;
				curr = curr.substring(1);
			}
			else if (curr.charAt(curr.length()-1) == ')') {
				closedPar++;
				curr = curr.substring(0, curr.length());
			}
			
			if (boolOps.contains(curr) && currIsInt) {  // If curr is a bool operator and the last read expression is an int expr
				if (!precededByVal) {
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Val op val ordering not preserved in bool expr.");
					return false;
				}
				precededByVal = false;
				if (!checkIntExpr(soFar, null)) {
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid integer expression");
					return false;
				}
				soFar = "";
				currIsInt = false;
			}
			else if (compOps.contains(curr)) {    // If curr is a comparative operator
				if (!precededByVal) {
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Val op val ordering not preserved in bool expr.");
					return false;
				}
				if (!currIsInt) {
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Comparative operator not preceded by int expr.");
					return false;
				}
				precededByVal = false;
				if (!checkIntExpr(soFar, null)) {
					System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid integer expression");
					return false;
				}
				soFar = "";
			}
			if (!boolVals.contains(curr) && !isInt(curr)) {  // If not a bool val and not an integer, check for vars and int ops
				if (curr.length() == 1 && isOp(curr.charAt(0))) {
					if (!precededByVal) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Val op val ordering not preserved in bool expr.");
						return false;
					}
					if (!currIsInt) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Comparative operator not preceded by int expr.");
						return false;
					}
					precededByVal = false;
					soFar += " " + curr.charAt(0);
				}
				// If curr string is command line argument
				else if (patterns.get("commandLineArg").matcher(curr).matches()) {
					if (args.length <= Integer.valueOf(curr.charAt(5))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid arg index");
						return false;
					}
					if (precededByVal) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Value not preceded by operator");
						return false;
					}
					if (patterns.get("bool").matcher(args[Integer.valueOf(curr.charAt(5))]).matches() && currIsInt) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Bool used in integer expr segment of bool expr.");
						return false;
					}
					else if (patterns.get("number").matcher(args[Integer.valueOf(curr.charAt(5))]).matches()) {
						currIsInt = true;
						soFar += " " + curr;
					}
					else {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid command line argument format");
						return false;
					}
					precededByVal = true;
				}
				else {
					// CHECKING VAL OP VAL ORDERING
					if (precededByVal) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Val op val ordering not preserved in bool expr.");
						return false;
					}
					precededByVal = true;
					
					Integer type = vars.get(vars.size()-1).get(curr);
					if (type == null) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Invalid var in bool expr.");
						return false;
					}
					else if (type == 0) {    // If type is int
						currIsInt = true;
						soFar += " " + curr;
					}
					else {    // If type is bool, check to be sure it's not encroaching on int expr
						if (currIsInt) {
							System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Bool used in integer expr segment of bool expr.");
							return false;
						}
					}
				}
			}
				
		}
		if (!(openPar == closedPar)) {
			System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Boolean expr parentheses mismatch");
			return false;
		}
		if (!precededByVal) {
			System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Expression ends with operator");
			return false;
		}
		return true;
	}
}