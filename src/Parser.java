import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
	static Set<Pattern> patterns;
	static Map<String, String> vars;
	
	public static void main(String[] args) {
		patterns = new HashSet<Pattern>();
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
	

	private static void setUpPatterns() {
		String digit = "\\d";
		String bool = "true|false";
		Pattern integer = Pattern.compile(digit + "+");
		patterns.add(integer);
		
		String commandLineArg = "$arg[" + integer + "]";
		
		String variable = "[\\w&&[^\\d]]+[\\d*[\\w&&[^\\d]]*]*";
		String value = "(" + integer + "|" + bool + "|" + commandLineArg + "|" + variable + ")";
		
		Pattern var_assgn = Pattern.compile("var " + variable + " = " + value);
		patterns.add(var_assgn);
		
		// Need to make i_expr code without breaking java
		Pattern expr_root = Pattern.compile("(" + variable + "|" + integer + ")");
		Pattern un_expr = Pattern.compile("(~" + expr_root + "|" + expr_root + ")");
		Pattern assoc_expr = Pattern.compile("(" + un_expr + "|"); // Need to build on this somehow
		Pattern assoc_op = Pattern.compile("[*/%]");
		// have a bit more left to go

		
		// Need to make bool_expr code without breaking java

		String print_matter = ""; // fill this out
		Pattern print = Pattern.compile("print(" + print_matter + ")");
		patterns.add(print);
		
		// In the end we need to add all the patterns into the patterns collection
	}
	
	private static Pattern IDPattern(String line) {
		 // This is where we iterate through the patterns and find a match
		for (Pattern curr : patterns) {
			Matcher m = curr.matcher(line);
			if (m.matches())
				return curr;
		}
		return null;
	}

	private static void translate(String line, Pattern idPattern) {
		// Want to output the correct translation to Java
		
		// output translation to file
	}
}