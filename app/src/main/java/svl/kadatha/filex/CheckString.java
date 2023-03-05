package svl.kadatha.filex;

public class CheckString
{
	public static boolean whetherStringContainsSpecialCharacters(String string)
	{
		char[] c_array=string.toCharArray();
		for(char c: c_array)
		{
			if(c == '/'|| c=='*' || c==':' || c=='?' || c=='\\' || c=='\n')
			{
				return true;
			}
			
		}
		return false;
		
	}

	public static boolean isStringOnlyAlphabet(String str)
	{

		// If string is empty or null
		if (str == null || str.equals("")) {

			// Return false
			return false;
		}

		// If we reach here we have character/s in string
		for (int i = 0; i < str.length(); i++) {

			// Getting character at indices
			// using charAt() method
			char ch = str.charAt(i);
			if ((!(ch >= 'A' && ch <= 'Z'))
					&& (!(ch >= 'a' && ch <= 'z'))) {
				return false;
			}
		}

		// String is only alphabetic
		return true;
	}

	public static boolean isStringWithoutSpaces(String str)
	{
		return true;
	}
}
