package compiled;
public class program2 {
	public static void main(String[] args) {
		int x = Integer.valueOf(args[0]);
		boolean flag = false;
		if (x > 1)
		{
			int i = 2;
			while ((! flag) && (x % i == 0))
			{
				if (x % i == 0)
				{
					flag = true;
					}
				i = i + 1;
				}
			}
		if (flag)
		{
			System.out.println("not prime");
			}
		else 
		{
			System.out.println("prime");
			}
	}
}