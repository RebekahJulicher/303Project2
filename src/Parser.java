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
	static HashMap<String, String> vars;
	
	public static void main(String[] args) {
		patterns = new HashMap<String, Pattern>();
		vars = new HashMap<String, String>();
		setUpPatterns();
		try {
			File file = new File(args[0]);
			Scanner in = new Scanner(file);
			while (in.hasNextLine()) {
				String line = in.nextLine().trim();
				translate(line, IDPattern(line));
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " could not be found!");
			e.printStackTrace();
		}
	}
	
	private static void defineVar(String line){
		String[] strArray = line.split(" ");
		/*
		for (String x : vars.keySet()) {
			if (vars.get(strArray[1]) != null){
				System.out.println("SYNTAX ERROR: Var " + strArray[1] + " has already been" +
									"defined, cannot define it again!");
				//e.printStackTrace();
				System.exit(1);
			}
		}
		*/
		if (vars.get(strArray[0]) != null) {
				System.out.println("SYNTAX ERROR: Var " + strArray[1] + " has already been" +
									"defined, cannot define it again!");
				//e.printStackTrace();
				System.exit(1);
		}

		Matcher m = patterns.get("bool_expr").matcher(strArray[3]);
		if (m.matches()){
			vars.get(vars.size() - 1).add(strArray[1], new VarContent(boolEval(strArray[3]));
			return;
		}
		m = patterns.get("i_expr").matcher(strArray[3]);
		if (m.matches()){
			vars.get(vars.size() - 1).add(strArray[1], new VarContent(intEval(strArray[3]));
			return;
		}
		m = patterns.get("string").matcher(strArray[3]);
		if (m.matches()){
			vars.get(vars.size() - 1).add(strArray[1],
						new VarContent(strArray[3].substring(0, strArray.length()-1));
			return;
		}

		// Checking if we're defining var in terms of another var
		for (Map<String, VarContent) x : vars){
			if (x.get(strArray[3]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				VarContent original = x.get(strArray[3]);
				if (original.type == 0) vars.get(vars.size() - 1).add(strArray[1],
													new VarContent(original.integerVal));
				else if (original.type == 1) vars.get(vars.size() - 1).add(strArray[1],
														new VarContent(original.boolVal));
				else vars.get(vars.size() - 1).add(strArray[1], new VarContent(original.stringVal));
				return;
			}
		}
		// TODO: ERROR OUT
	}

	private static void setVar(String line){
		String[] strArray = line.split(" ");
		VarContent content = vars.get(vars.size()-1).get(strArray[0]);
		
		if (content == null){
			System.out.println("SYNTAX ERROR: Var " + strArray[0] + " has not been" +
									"defined, cannot set it!");
			e.printStackTrace();
			System.exit();
		}
		Matcher m = patterns.get("bool_expr").matcher(strArray[2]);
		if (m.matches()){
			if (content.type != 1){
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of" + 
									"type boolean, so it cannot be set to a boolean!");
				e.printStackTrace();
				System.exit();
			}
			content.boolVal = boolEval(strArray[2]);
			return;
		}
		m = patterns.get("i_expr").matcher(strArray[2]);
		if (m.matches()){
			if (content.type != 0){
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of" +
									"type int, so it cannot be set to an int!");
				e.printStackTrace();
				System.exit();
			}
			content.integerVal = intEval(strArray[2]);
			return;
		}
		m = patterns.get("string").matcher(strArray[2]);
		if (m.matches()){
			if (content.type != 0){
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of " +
									"type string, so it cannot be set to a string!");
				e.printStackTrace();
				System.exit();
			}
			content.integerVal = strArray[3].substring(0, strArray.length()-1);
			return;
		}
		// Checking if we're defining var in terms of another var
		for (Map<String, VarContent) x : vars){
			if (x.get(strArray[2]) != null){
				// THIS IS ASSUMING WE WANT TO DO COPIES INSTEAD OF POINTERS FOR THIS
				VarContent original = x.get(strArray[2]);
				if (original.type == content.type){
					if (original.type == 0) content.integerVal = original.integerVal;
					else if (original.type == 1) content.boolVal = original.boolVal;
					else content.stringVal = original.stringVal;
					return;
				}
				System.out.println("SYNTAX ERROR: Var " + strArray[0] + " is not of the " +
									"sametype as var " + strArray[2]);
				e.printStackTrace();
				System.exit();
			}
		}
		// TODO: ERROR OUT
	}

	private static int intEval(String expr){
		if (isInt(expr)) return Integer.parseInt(expr);
		return 0;
		// TODO: add recursive expression evaluating;
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

	// Pass in a boolean or boolean expression
	private static boolean boolEval(String expr){
		if (expr.equals("true")) return true;
		if (expr.equals("false")) return false;
		else
			return false; // had to add this in so it wouldn't keep erroring
		// TODO: add recursive expression evaluating
	}

	private static void setUpPatterns() {
		String comment = "$+[\\d*[\\w&&[^\\d]]*]*";
		String string = '"' + "[\\d*[\\w&&[^\\d]]*]*" + '"';

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
		Pattern expr_root = Pattern.compile("(" + variable + "|" + integer + ")");
		Pattern un_expr = Pattern.compile("(~" + expr_root + "|" + expr_root + ")");
		Pattern assoc_expr = Pattern.compile("(" + un_expr + "|"); // Need to build on this somehow
		Pattern assoc_op = Pattern.compile("[*/%]");
		// have a bit more left to go

		
		// Need to make bool_expr code without breaking java

		String print_matter = ""; // fill this out
		Pattern print = Pattern.compile("print(" + print_matter + ")");
		patterns.put("print", print);
		
		// In the end we need to add all the patterns into the patterns collection
	}
	
	private static String IDPattern(String line) {
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

	static class VarContent {
		// 0 for int, 1 for boolean, 2 for string
		int type;
		int integerVal;
		boolean boolVal;
		String stringVal;
		public VarContent(int val){
			type = 0;
			integerVal = val;
		}

		public VarContent(boolean val){
			type = 1;
			boolVal = val;
		}

		public VarContent(String val){
			type = 2;
			stringVal = val;
		}
	}

}