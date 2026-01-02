package svl.kadatha.filex;

import java.util.Set;

public class MimePOJO {
    private final String file_type;
    private final String mime_type;
    private final Set<String> ext_set;

    MimePOJO(String file_type, String mime_type, Set<String> ext_set) {
        this.file_type = file_type;
        this.mime_type = mime_type;
        this.ext_set = ext_set;
    }

    public String getFile_type() {
        return file_type;
    }

    public String getMime_type() {
        return mime_type;
    }

    public Set<String> getExtSet() {
        return ext_set;
    }

}
