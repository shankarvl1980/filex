package svl.kadatha.filex;

public class MimePOJO {
    private final String file_type;
    private final String mime_type;
    private final String regex;

    MimePOJO(String file_type, String mime_type, String regex) {
        this.file_type = file_type;
        this.mime_type = mime_type;
        this.regex = regex;
    }

    public String getFile_type() {
        return file_type;
    }

    public String getMime_type() {
        return mime_type;
    }

    public String getRegex() {
        return regex;
    }

}
