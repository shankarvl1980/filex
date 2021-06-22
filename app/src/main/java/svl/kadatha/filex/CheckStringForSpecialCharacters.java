package svl.kadatha.filex;

public class CheckStringForSpecialCharacters
{
	public static boolean whetherStringContains(String string)
	{
		char[] c_array=string.toCharArray();
		for(char c: c_array)
		{
			if(c == '/'|| c=='*' || c==':' || c=='?' || c=='\\')
			{
				return true;
			}
			
		}
		return false;
		
	}
}
