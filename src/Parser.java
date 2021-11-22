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
	static ArrayList< HashMap<String, Integer> > vars = null;
	static int indentationLevel = 0;
	static File javaFile = null;
	static FileWriter writer = null;
	
	public static void main(String[] args) throws IOException {
		patterns = new HashMap<String, Pattern>();
		vars = new ArrayList< HashMap<String, Integer> >();
		vars.add(new HashMap<String,Integer>());
		setUpPatterns();

		try {
			javaFile = new File("compiled/" + args[0].replaceAll(".txt", ".java"));
			if (javaFile.createNewFile()) 
				System.out.println("File created: " + javaFile.getName());
			else
				System.out.println("File already exists");
			System.out.println(javaFile.getName());
			writer = new FileWriter(javaFile);
		} catch (IOException e) {
			System.out.println("Error making file");
			e.printStackTrace();
		}

		try {
			File file = new File("scripts/" + args[0]);
			Scanner in = new Scanner(file);
			boolean commentBlock = false;
			
			writer.write("package compiled;\npublic class " + args[0].substring(0,args[0].length()-4) +
							" {\n\tpublic static void main(String[] args) {\n");
			writer.flush();
			
			while (in.hasNextLine()) {
				lineNum++;
				String line = in.nextLine().trim();
				if (line.length() > 0) {
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
						//System.out.println("     preparing to translate line...");
						translate(line, IDPattern(line));
				}
			}
			writer.write("\t}\n}");
			writer.flush();
			
			if (indentationLevel < 0) 
				System.out.println("SYNTAX ERROR: More \"end\" statements than \"start\" statements in code.");
			if (indentationLevel > 0) 
				System.out.println("SYNTAX ERROR: More \"start\" statements than \"end\" statements in code.");
			
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " could not be found!");
			e.printStackTrace();
		}
		System.out.println("FINISHED");
	}
	
	
	private static boolean isInt(String expr) {
		if (expr == null)
			return false;
		try {
			Integer.parseInt(expr);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	private static boolean isAssignment(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("make"))
			return true;
		return false;
	}
	
	private static boolean isVarSet(String expr) {
		String words[] = expr.split(" ");
		if (words[1].equals("="))
			return true;
		return false;
	}
	
	private static boolean isIf(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("if"))
			return true;
		return false;
	}

	private static boolean isElseIf(String expr) {
		String words[] = expr.split(" ");
		if ((words[0] + words[1]).equals("elseif"))
			return true;
		return false;
	}
	
	private static boolean isElse(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("else"))
			return true;
		return false;
	}
	
	private static boolean isWhile(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("while"))
			return true;
		return false;
	}

	private static boolean isFor(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("for"))
			return true;
		return false;
	}

	private static boolean isPrint(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("print"))
			return true;
		return false;
	}
	
	private static boolean isPrintL(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("printl"))
			return true;
		return false;
	}
	
	private static boolean isStart(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("start")) {
			return true;
		}
		return false;
	}

	private static boolean isEnd(String expr) {
		String words[] = expr.split(" ");
		if (words[0].equals("end")) {
			indentationLevel--;
			return true;
		}
		return false;
	}

	private static void setUpPatterns() {
		Pattern number = Pattern.compile("-?\\d+(\\.\\d+)?");
		patterns.put("number", number);

		String digit = "\\d";
		String bool = "true|false";
		Pattern integer = Pattern.compile(digit + "+");
		patterns.put("int", integer);
		
		String commandLineArg = "args\\[" + integer + "\\]";
		Pattern commandLineArgP = Pattern.compile(commandLineArg);
		patterns.put("commandLineArg", commandLineArgP);
		
		String variable = "[\\w&&[^\\d]]+[\\d*[\\w&&[^\\d]]*]*";
		// value is going to need bool_expr, i_expr, and string
		String value = "(" + integer + "|" + bool + "|" + commandLineArg + "|" + variable + ")";
		
		Pattern var_assgn = Pattern.compile("var " + variable + " = " + value);
		patterns.put("var_assgn", var_assgn);

		// Need to make bool_expr code without breaking java

		String print_matter = ""; // fill this out
		Pattern print = Pattern.compile("print(" + print_matter + ")");
		patterns.put("print", print);
		
		// In the end we need to add all the patterns into the patterns collection
	}
	
	private static String IDPattern(String line) {
		if (isStart(line))
			return "start";
		if (isEnd(line))
			return "end";
		if (isAssignment(line))
			return "assign";
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
		if (isPrintL(line))
			return "printl";

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
		String initialTab = "\t\t";
		for (int i = 0; i < indentationLevel; i++)
			initialTab += "\t";
		writer.write(initialTab);
		System.out.println("Translating line: " + line);
		System.out.println("This line is a: " + patternName);
		if (patternName.equals("assign")) 
			writer.write(translatedAssign(line));
		else if (patternName.equals("if"))
			writer.write(translatedIf(line));
		else if (patternName.equals("else_if"))
			writer.write(translatedElseIf(line));
		else if (patternName.equals("else"))
			writer.write(translatedElse(line));
		else if (patternName.equals("while"))
			writer.write(translatedWhile(line));
		else if (patternName.equals("for"))
			writer.write(translatedFor(line));
		else if (patternName.equals("print"))
			writer.write(translatedPrint(line));
		else if (patternName.equals("printl"))
			writer.write(translatedPrintL(line));
		else if (patternName.equals("start")) {
			writer.write(translatedStart(line));
			indentationLevel++;
		}
		else if (patternName.equals("end"))
			writer.write(translatedEnd(line));

		writer.write("\n");
		System.out.println("Line translated");
		writer.flush();
			
	}
	
	private static String translatedAssign(String line) {
		String[] words = line.split(" ");
		String arg = line.substring(5);
		defineVar(line);
		
		if (checkBoolExpr(arg)) {
			String replacedAnd = arg.replaceAll("and", "&&");
			String replacedOr = replacedAnd.replaceAll("or", "||");
			return "boolean " + replacedOr + ";";
		} else if (checkIntExpr(arg)) {
			String replacedArg = arg.replaceAll("args", "Integer.valueOf(args");
			String replacedEndArg = replacedArg.replaceAll("]", "])");
			return "int "  + replacedEndArg + ";";
		} else {
			System.out.println("SYNTAX ERROR: Invalid variable assignment");
			System.exit(1);
		}
		/*
		if (!checkBoolExpr(arg, null))
			System.exit(1);
		String replacedAnd = arg.replaceAll("and", "&&");
		String replacedOr = replacedAnd.replaceAll("or", "||");
		return "if (" + replacedOr + ")";
		*/
		return null;
	}
	
	private static String translatedIf(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		if (!checkBoolExpr(arg))
			System.exit(1);
		String replacedAnd = arg.replaceAll("and", "&&");
		String replacedOr = replacedAnd.replaceAll("or", "||");
		return "if (" + replacedOr + ")";
	}

	private static String translatedElse(String line) {
		return "else ";
	}

	private static String translatedElseIf(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		if (!checkBoolExpr(arg))
			System.exit(1);
		String replacedAnd = arg.replaceAll("and", "&&");
		String replacedOr = replacedAnd.replaceAll("or", "||");
		return "else if (" + replacedOr + ")";
	}

	private static String translatedWhile(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		if (!checkBoolExpr(arg))
			System.exit(1);
		String replacedAnd = arg.replaceAll("and", "&&");
		String replacedOr = replacedAnd.replaceAll("or", "||");
		return "while (" + replacedOr + ")";
	}

	private static String translatedFor(String line) {
		vars.add(new HashMap<String,Integer>());
		String[] words = line.split(" ");
		String counter = words[1];
		for (HashMap<String, Integer> x : vars){
			if (x.get(counter) != null) {
				System.out.println("Line " + lineNum + ": " + "SYNTAX ERROR: Invalid variable 1 used in for loop.");
				System.exit(1);
			}
		}
		vars.get(vars.size() - 1).put(counter, 0);
		String start = words[3];
		try { Integer.parseInt(start); }
		catch(NumberFormatException e) {
			boolean found = false;
			Integer startVar;
			for (HashMap<String, Integer> x : vars){
				startVar = x.get(start);
				if (startVar != null) {
					found = true;
					if (startVar != 0) {
						System.out.println("Line " + lineNum + ": " + "SYNTAX ERROR: Invalid variable 1 used in for loop.");
						System.exit(1);
					}
				}
			}
			if (!found) {
				System.out.println("Line " + lineNum + ": " + "SYNTAX ERROR: Invalid variable 2 used in for loop.");
				System.exit(1);
			}
		}
		//System.out.println("        translating for (int " + counter + " = " + startInt + ");");
		String end = words[5];
		try { Integer.parseInt(end); }
		catch(NumberFormatException e) {
			boolean found = false;
			//for (HashMap<String, Integer> x : vars) System.out.println(x.toString());
			//System.out.println(end);
			
			Integer endVar;
			for (HashMap<String, Integer> x : vars){
				endVar = x.get(end);
				if (endVar != null) {
					found = true;
					if (endVar != 0) {
						System.out.println("Line " + lineNum + ": " + "SYNTAX ERROR: Invalid variable 3 used in for loop.");
						System.exit(1);
					}
				}
			}
			if (!found) {
				System.out.println("Line " + lineNum + ": " + "SYNTAX ERROR: Invalid variable 4 used in for loop.");
				System.exit(1);
			}
		}
		boolean exists = false;
		for (HashMap<String, Integer> variableSet : vars) {
			if (variableSet.containsKey(counter)) {
				exists = true;
				if (!checkIntExpr(counter)) {
					System.out.println("Line " + lineNum + ": " + "SYNTAX ERROR: Can't pass in a non-integer as a counter variable in a for-loop.");
					System.exit(1);
				}
			}
		}
		
		String addOn = exists ? "" : "int ";
		// need to see how this performs when startInt == endInt
		return "for (int " + addOn + counter + " = " + start + "; " + counter + " < " + end + "; " + counter + "++)";
		/*
		if (startInt < endInt)  
			return "for (" + addOn + counter + " = " + start + "; " + counter + " < " + end + "; " + counter + "++)";
		else if (startInt > endInt)  
			return "for (" + addOn + counter + " = " + start + "; " + counter + " > " + end + "; " + counter + "--)";
		else
			return "for (" + addOn + counter + " = " + start + "; " + counter + " == " + end + "; " + counter + "++)";
		*/
	}

	private static String translatedPrint(String line) {
		String[] words = line.split(" ");
		int index = line.indexOf(words[1]);
		String arg = line.substring(index);
		return "System.out.print(" + arg + ");";
	}
	
	private static String translatedPrintL(String line) {
		String[] words = line.split(" ");
		//System.out.println(words[0]);
		String arg = line.substring(7);
		return "System.out.println(" + arg + ");";
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
		line.contains("%") || line.contains("-") || isInt(strArray[0]);
	}
	
	private static boolean handleExpr(boolean isDefinition, boolean isBool, String content, String name) {
		Integer type = vars.get(vars.size()-1).get(name);
		
		if (isDefinition && isBool) {
			if (!checkBoolExpr(content)) System.exit(1);
			vars.get(vars.size() - 1).put(name, 1);
			return true;
			//return "boolean " + name + " = " + content + ";";
		}
		else if (isDefinition && !isBool) {
			if (!checkIntExpr(content)) System.exit(1);
			vars.get(vars.size() - 1).put(name, 0);
			return true;
			//return "int " + name + " = " + content + ";";
		}
		else if (!isDefinition && isBool) {
			if (type != 1){
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + name + 
						" is not of type boolean, so it cannot be set to a boolean!");
				System.exit(1);
			}
			if (!checkBoolExpr(content)) System.exit(1);
			return true;
			//return name + " = " + content + ";";
		}
		else {
			if (type != 0){
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + name + 
						" is not of type int, so it cannot be set to an int!");
				System.exit(1);
			}
			if (!checkIntExpr(content)) System.exit(1);
			return true;
			//return name + " = " + content + ";";
		}
	}
	
	private static boolean defineVar(String line){
		String[] strArray = line.split(" ");
		String content = "";
		for (int i = 3; i < strArray.length; i++) content += strArray[i];
		if (vars.get(vars.size()-1).get(strArray[1]) != null) {  // If name already exists, error
				System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[1] + " has already been " +
									"defined, cannot define it again!");
				System.exit(1);
		}
		
		// Checking for boolean expression
		if (isBoolExpr(line)) return handleExpr(true, true, content, strArray[1]);
		
		// Checking for int expression
		if (containsIntExpr(content)) return handleExpr(true, false, content, strArray[1]); 
		
		//HANDLE VARS AND COMMAND LINE ARGS
		if (patterns.get("commandLineArg").matcher(strArray[3]).matches()) {
			vars.get(vars.size() - 1).put(strArray[1], 0);
			return true;
			//return "int " + strArray[1] + " = Integer.valueOf(" + strArray[3] + ");";
		}
		
		// Checking if we're defining var in terms of another var
		for (HashMap<String, Integer> x : vars){
			if (x.get(strArray[1]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				Integer original = x.get(strArray[3]);
				vars.get(vars.size() - 1).put(strArray[1], original);
				if (original == 0) return true;
				//if (original == 0) return "int " + strArray[1] + " = " + strArray[3] + ";";
				return true;
				//return "boolean " + strArray[1] + " = " + strArray[3] + ";";
			}
		}
		System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Variable assignment to nonexistent variable");
		System.exit(1);
		return true;
		//return "\\";
	}
	
	
	private static boolean isOp(char item) {
		return item == '+' || item == '-' || item == '*' || item == '/' || item == '%';
	}

	private static boolean setVar(String line){
		String[] strArray = line.split(" ");
		Integer type = vars.get(vars.size()-1).get(strArray[1]);
		String content = "";
		for (int i = 3; i < strArray.length; i++) content += strArray[i];
		
		if (type == null){
			System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Var " + strArray[1] + " has not been" +
									"defined, cannot set it!");
			System.exit(1);
		}
		
		if (isBoolExpr(line)) return handleExpr(false, true, content, strArray[0]);
		
		if (containsIntExpr(content)) return handleExpr(false, false, content, strArray[0]);
		
		//HANDLE VARS AND COMMAND LINE ARGS
		if (patterns.get("commandLineArg").matcher(strArray[3]).matches()) {
			return true;
			//return strArray[1] + " = " + content + ";";
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
				return true;
				//return strArray[1] + " = " + strArray[3] + ";";
			}
		}
		System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Variable assignment to nonexistent variable");
		System.exit(1);
		return true;
		//return "\\";
	}
	
	// THIS CURRENTLY DOES NOT ACCOUNT FOR EXTRA SPACES BETWEEN OPS AND PARENTHESES
	private static boolean checkIntExpr(String line) {
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
			//System.out.println(soFar);
			if (strArray[i].length() > 0) {
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
					return true;
					/*
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
					*/
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
					}
					if (!precededByVal) {
						System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Integer expression contains nonexistent variable");
						return false;
					}
				}
			}
		}
		if (!precededByVal) System.out.println("Line: " + lineNum + ": " + "SYNTAX ERROR: Integer expression does not end with val");
		return precededByVal;
	}

	private static boolean checkBoolExpr(String expr) {
		String[] parts = expr.split("\\s+");
		
		//System.out.println(parts[0]);

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
			
			if (curr.length() > 0) {
				if (curr.charAt(0) == '(') {
					openPar++;
					curr = curr.substring(1);
				}
				else if (curr.charAt(curr.length()-1) == ')') {
					closedPar++;
					curr = curr.substring(0, curr.length()-1);
				}
				
				if (curr == "true" || curr == "false") {
					if (precededByVal) {
						System.out.println("Line " + lineNum + ": " + "NOT BOOLEAN EXPR: Val op val ordering not preserved at bool value");
						return false;
					}
					precededByVal = true;
				}
				else if(isInt(curr)) {
					if (precededByVal) {
						System.out.println("Line " + lineNum + ": " + "NOT BOOLEAN EXPR: Val op val ordering not preserved at int value");
						return false;
					}
					precededByVal = true;
					soFar += " " + curr;
				}
				else if (boolOps.contains(curr) && currIsInt) {  // If curr is a bool operator and the last read expression is an int expr
					if (!precededByVal) {
						System.out.println("Line " + lineNum + ": " + "NOT BOOLEAN EXPR: Val op val ordering not preserved at boolOp");
						return false;
					}
					precededByVal = false;
					if (!checkIntExpr(soFar)) {
						System.out.println("Line: " + lineNum + ": " + "NOT BOOLEAN EXPR: Invalid integer expression");
						return false;
					}
					soFar = "";
					currIsInt = false;
				}
				else if (compOps.contains(curr)) {    // If curr is a comparative operator
					if (!precededByVal) {
						System.out.println("Line " + lineNum + ": " + "NOT BOOLEAN EXPR: Val op val ordering not preserved at compOp");
						return false;
					}
					if (!currIsInt) {
						System.out.println("Line: " + lineNum + ": " + "NOT BOOLEAN EXPR: Comparative operator not preceded by int expr.");
						return false;
					}
					precededByVal = false;
					if (!checkIntExpr(soFar)) {
						System.out.println("Line: " + lineNum + ": " + "NOT BOOLEAN EXPR: Invalid integer expression");
						return false;
					}
					soFar = "";
				}
				else if (!boolVals.contains(curr) && !isInt(curr)) {  // If not a bool val and not an integer, check for vars and int ops
					if (curr.length() == 1 && isOp(curr.charAt(0))) {
						if (!precededByVal) {
							System.out.println("Line " + lineNum + ": " + "NOT BOOLEAN EXPR: Val op val ordering not preserved in bool expr at other val");
							return false;
						}
						if (!currIsInt) {
							System.out.println("Line: " + lineNum + ": " + "NOT BOOLEAN EXPR: Comparative operator not preceded by int expr.");
							return false;
						}
						precededByVal = false;
						soFar += " " + curr.charAt(0);
					}
					// If curr string is command line argument
					else if (patterns.get("commandLineArg").matcher(curr).matches()) {
						return false;
						//TODO: Fix this, args not working because it is null
						//if (args.length <= Integer.valueOf(curr.charAt(4))) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
						/*
						System.out.println(curr.charAt(4));
						System.out.println(Integer.parseInt("" + curr.charAt(4)));
						if (args.length <= Integer.parseInt(curr.charAt(4) + "")) { // MAY NEED TO BE 4 IF WE REMOVE $ FROM ARG
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
						*/
					}
					else {
						// CHECKING VAL OP VAL ORDERING
						if (precededByVal) {
							System.out.println("Line: " + lineNum + ": " + "NOT BOOLEAN EXPR: Val op val ordering not preserved in bool expr.");
							return false;
						}
						precededByVal = true;
						
						Integer type = null;
						for (HashMap<String, Integer> x : vars){
							if (x.get(curr) != null) type = x.get(curr);
						}
						if (type == null) {
							System.out.println("Line " + lineNum + ": " + "NOT BOOLEAN EXPR: Invalid var in bool expr.");
							return false;
						}
						else if (type == 0) {    // If type is int
							currIsInt = true;
							soFar += " " + curr;
						}
						else {    // If type is bool, check to be sure it's not encroaching on int expr
							if (currIsInt) {
								System.out.println("Line: " + lineNum + ": " + "NOT BOOLEAN EXPR: Bool used in integer expr segment of bool expr.");
								return false;
							}
						}
					}
				}
			}
				
		}
		if (!(openPar == closedPar)) {
			System.out.println(openPar + ", " +  closedPar);
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