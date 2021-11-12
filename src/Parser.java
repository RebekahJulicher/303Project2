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
	
	private static void defineVar(String line){
		String[] strArray = line.split(" ");
		
		if (vars.get(vars.size()-1).get(strArray[1]) != null) {
				System.out.println("SYNTAX ERROR: Var " + strArray[1] + " has already been" +
									"defined, cannot define it again!");
				System.exit(1);
		}
		
		if (line.contains("==") || line.contains("<=") || line.contains(">=") || line.contains("<>") ||
			line.contains(">") || line.contains("<") || line.contains(" and ") ||
			line.contains(" not ") || line.contains(" or ")){
			// HANDLE VARS/COMMAND LINE ARGS
			vars.get(vars.size() - 1).put(strArray[1], 1);
			// TODO: Write appropriate thing to file
			return;
		}
		
		if (line.contains("+") || line.contains("-") || line.contains("/") || line.contains("*") ||
				line.contains("%") || line.contains("-") || isInt(strArray[3])){
			// HANDLE VARS/COMMAND LINE ARGS
			vars.get(vars.size() - 1).put(strArray[1], 0);
			// TODO: Write appropriate thing to file
			return;
		}
		
		//HANDLE VARS AND COMMAND LINE ARGS
		/*
		// Checking if we're defining var in terms of another var
		for (HashMap<String, VarContent> x : vars){
			if (x.get(strArray[3]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				VarContent original = x.get(strArray[3]);
				if (original.type == 0) vars.get(vars.size() - 1).put(strArray[1],
													new VarContent(original.integerVal));
				else vars.get(vars.size() - 1).put(strArray[1],
														new VarContent(original.boolVal));
				return;
			}
		}
		// TODO: ERROR OUT
		 
		 */
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