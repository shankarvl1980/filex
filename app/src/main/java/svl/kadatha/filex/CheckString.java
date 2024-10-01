package svl.kadatha.filex;

public class CheckString {
    public static boolean whetherStringContainsSpecialCharacters(String string) {
        char[] c_array = string.toCharArray();
        for (char c : c_array) {
            if (c == '/' || c == '*' || c == ':' || c == '?' || c == '\\' ||
                    c == '\n' || c == '+' || c == '-' || c == '!' || c == '@' ||
                    c == '%' || c == '^' || c == '&' || c == '#' || c == '=' ||
                    c == '"' || c == '\'') {
                return true;
            }
        }
        return false;
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
