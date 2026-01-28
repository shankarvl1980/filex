package svl.kadatha.filex;

import java.util.EnumMap;

public final class NavSessionStore {
    private static final EnumMap<FileObjectType, Long> map = new EnumMap<>(FileObjectType.class);

    public static synchronized long current(FileObjectType t) {
        Long v = map.get(t);
        if (v == null) { v = 1L; map.put(t, v); }
        return v;
    }

    public static synchronized void bump(FileObjectType t) {
        map.put(t, current(t) + 1);
    }
}
