package svl.kadatha.filex;

public class CheckString {
    public static boolean whetherStringContainsSpecialCharactersForTableName(String string) {
        char[] c_array = string.toCharArray();
        for (char c : c_array) {
            if (c == '/' || c == '*' || c == ':' || c == '?' || c == '\\' ||
                    c == '\n' || c == '+' || c == '-' || c == '!' || c == '@' ||
                    c == '%' || c == '^' || c == '&' || c == '#' || c == '=' ||
                    c == '"' || c == '\'' || c == '<' || c == '>' || c == '|' ||
                    c == '\0') {
                return true;
            }
        }
        return false;
    }


    public static boolean whetherStringContainsSpecialCharacters(String input) {
        // Define the prohibited characters
        char[] prohibitedChars = {'/', '\\', ':', '*', '?', '"', '<', '>', '|', '\0'};

        // Check each character in the input string
        for (char c : input.toCharArray()) {
            for (char prohibited : prohibitedChars) {
                if (c == prohibited) {
                    return true; // Found a prohibited character
                }
            }
        }
        return false; // No prohibited characters found
    }

    public static boolean isStringOnlyAlphabet(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if ((!(ch >= 'A' && ch <= 'Z'))
                    && (!(ch >= 'a' && ch <= 'z'))) {
                return false;
            }
        }
        return true;
    }
}
