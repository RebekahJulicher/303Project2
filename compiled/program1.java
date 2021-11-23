package compiled;
public class program1 {
	public static void main(String[] args) {
		int x = Integer.valueOf(args[0]);
		int y = Integer.valueOf(args[1]);
		int m = Integer.valueOf(args[2]);
		for (int i = 1; i < m; i++)
		{
			if ((i % x == 0) || (i % y == 0))
			{
				System.out.println(i);
				}
			}
	}
}