package svl.kadatha.filex;

import java.util.Comparator;

public class FileComparator {

    // Singleton instance of NaturalSortComparator
    private static final NaturalSortComparator NATURAL_SORT_COMPARATOR = new NaturalSortComparator();

    public static Comparator<FilePOJO> FilePOJOComparate(String SORT, boolean compare_total_file_size) {
        switch (SORT) {
            case "d_name_desc":
                return new SORT_D_FILE_POJO_NAME_DESC();

            case "d_name_asc":
                return new SORT_D_FILE_POJO_NAME_ASC();

            case "d_date_asc":
                return new SORT_D_FILE_POJO_TIME_ASC();

            case "d_date_desc":
                return new SORT_D_FILE_POJO_TIME_DESC();

            case "d_size_desc":
                if (compare_total_file_size) return new SORT_D_FILE_POJO_TOTAL_SIZE_DESC();
                else return new SORT_D_FILE_POJO_SIZE_DESC();

            case "d_size_asc":
                if (compare_total_file_size) return new SORT_D_FILE_POJO_TOTAL_SIZE_ASC();
                else return new SORT_D_FILE_POJO_SIZE_ASC();

            case "f_name_asc":
                return new SORT_F_FILE_POJO_NAME_ASC();

            case "f_name_desc":
                return new SORT_F_FILE_POJO_NAME_DESC();

            case "f_date_asc":
                return new SORT_F_FILE_POJO_TIME_ASC();

            case "f_date_desc":
                return new SORT_F_FILE_POJO_TIME_DESC();

            case "f_size_desc":
                if (compare_total_file_size) return new SORT_F_FILE_POJO_TOTAL_SIZE_DESC();
                else return new SORT_F_FILE_POJO_SIZE_DESC();

            case "f_size_asc":
                if (compare_total_file_size) return new SORT_F_FILE_POJO_TOTAL_SIZE_ASC();
                else return new SORT_F_FILE_POJO_SIZE_ASC();

            default:
                return new SORT_D_FILE_POJO_NAME_ASC();
        }
    }

    public static Comparator<AppManagerListFragment.AppPOJO> AppPOJOComparate(String SORT) {
        switch (SORT) {
            case "d_name_desc":
            case "f_name_desc":
                return new SORT_APP_POJO_NAME_DESC();

            case "d_name_asc":
            case "f_name_asc":
                return new SORT_APP_POJO_NAME_ASC();

            case "d_date_asc":
            case "f_date_asc":
                return new SORT_APP_POJO_TIME_ASC();

            case "d_date_desc":
            case "f_date_desc":
                return new SORT_APP_POJO_TIME_DESC();

            case "d_size_desc":
            case "f_size_desc":
                return new SORT_APP_POJO_SIZE_DESC();

            case "d_size_asc":
            case "f_size_asc":
                return new SORT_APP_POJO_SIZE_ASC();

            default:
                return new SORT_APP_POJO_NAME_ASC();
        }
    }

    // Natural Sort Comparator
    private static class NaturalSortComparator implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            int indexA = 0, indexB = 0;
            int lengthA = a.length();
            int lengthB = b.length();

            while (indexA < lengthA && indexB < lengthB) {
                char charA = a.charAt(indexA);
                char charB = b.charAt(indexB);

                boolean isDigitA = Character.isDigit(charA);
                boolean isDigitB = Character.isDigit(charB);

                StringBuilder chunkA = new StringBuilder();
                StringBuilder chunkB = new StringBuilder();

                // Extract chunk from a
                while (indexA < lengthA && Character.isDigit(a.charAt(indexA)) == isDigitA) {
                    chunkA.append(a.charAt(indexA++));
                }

                // Extract chunk from b
                while (indexB < lengthB && Character.isDigit(b.charAt(indexB)) == isDigitB) {
                    chunkB.append(b.charAt(indexB++));
                }

                String chunkAStr = chunkA.toString();
                String chunkBStr = chunkB.toString();

                int result = 0;

                if (isDigitA && isDigitB) {
                    // Remove leading zeros
                    int nonZeroA = 0;
                    while (nonZeroA < chunkAStr.length() && chunkAStr.charAt(nonZeroA) == '0') nonZeroA++;
                    int nonZeroB = 0;
                    while (nonZeroB < chunkBStr.length() && chunkBStr.charAt(nonZeroB) == '0') nonZeroB++;

                    // Compare lengths of remaining digits
                    int lenA = chunkAStr.length() - nonZeroA;
                    int lenB = chunkBStr.length() - nonZeroB;
                    if (lenA != lenB) {
                        return lenA - lenB;
                    }

                    // Compare digit by digit
                    for (int i = nonZeroA, j = nonZeroB; i < chunkAStr.length(); i++, j++) {
                        char digitA = chunkAStr.charAt(i);
                        char digitB = chunkBStr.charAt(j);
                        if (digitA != digitB) {
                            return digitA - digitB;
                        }
                    }
                    // All digits are equal, continue
                } else {
                    // Compare lexicographically, case-insensitive
                    result = chunkAStr.compareToIgnoreCase(chunkBStr);
                    if (result != 0) {
                        return result;
                    }
                }
                // If equal, continue to next chunks
            }

            // If one string is a prefix of the other
            return lengthA - lengthB;
        }
    }

    // FilePOJO Comparators
    private static class SORT_D_FILE_POJO_NAME_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (f1.getIsDirectory() && !f2.getIsDirectory())
                return -1;
            else if (!f1.getIsDirectory() && f2.getIsDirectory())
                return 1;
            else
                return NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
        }
    }

    private static class SORT_F_FILE_POJO_NAME_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else
                return NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
        }
    }

    private static class SORT_D_FILE_POJO_NAME_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (f1.getIsDirectory() && !f2.getIsDirectory())
                return -1;
            else if (!f1.getIsDirectory() && f2.getIsDirectory())
                return 1;
            else {
                int i = NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
                return -i;
            }
        }
    }

    private static class SORT_F_FILE_POJO_NAME_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else {
                int i = NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
                return -i;
            }
        }
    }

    private static class SORT_D_FILE_POJO_TIME_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (f1.getIsDirectory() && !f2.getIsDirectory())
                return -1;
            else if (!f1.getIsDirectory() && f2.getIsDirectory())
                return 1;
            else
                return Long.compare(f1.getDateLong(), f2.getDateLong());
        }
    }

    private static class SORT_F_FILE_POJO_TIME_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else
                return Long.compare(f1.getDateLong(), f2.getDateLong());
        }
    }

    private static class SORT_D_FILE_POJO_TIME_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (f1.getIsDirectory() && !f2.getIsDirectory())
                return -1;
            else if (!f1.getIsDirectory() && f2.getIsDirectory())
                return 1;
            else {
                int i = Long.compare(f1.getDateLong(), f2.getDateLong());
                return -i;
            }
        }
    }

    private static class SORT_F_FILE_POJO_TIME_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else {
                int i = Long.compare(f1.getDateLong(), f2.getDateLong());
                return -i;
            }
        }
    }

    private static class SORT_D_FILE_POJO_SIZE_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (f1.getIsDirectory() && !f2.getIsDirectory())
                return -1;
            else if (!f1.getIsDirectory() && f2.getIsDirectory())
                return 1;
            else if (f1.getIsDirectory() && f2.getIsDirectory())
                return NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
            else {
                return Long.compare(f1.getSizeLong(), f2.getSizeLong());
            }
        }
    }

    private static class SORT_F_FILE_POJO_SIZE_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else if (f1.getIsDirectory() && f2.getIsDirectory())
                return NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
            else
                return Long.compare(f1.getSizeLong(), f2.getSizeLong());
        }
    }

    private static class SORT_D_FILE_POJO_SIZE_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (f1.getIsDirectory() && !f2.getIsDirectory())
                return -1;
            else if (!f1.getIsDirectory() && f2.getIsDirectory())
                return 1;
            else if (f1.getIsDirectory() && f2.getIsDirectory()) {
                int i = Long.compare(f1.getTotalSizeLong(), f2.getTotalSizeLong());
                return -i;
            } else {
                int i = Long.compare(f1.getSizeLong(), f2.getSizeLong());
                return -i;
            }
        }
    }

    private static class SORT_F_FILE_POJO_SIZE_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else if (f1.getIsDirectory() && f2.getIsDirectory()) {
                int i = Long.compare(f1.getTotalSizeLong(), f2.getTotalSizeLong());
                return -i;
            } else {
                int i = Long.compare(f1.getSizeLong(), f2.getSizeLong());
                return -i;
            }
        }
    }

    // SORT_D_FILE_POJO_TOTAL_SIZE_ASC Comparator
    private static class SORT_D_FILE_POJO_TOTAL_SIZE_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            // Prioritize directories over files
            if (f1.getIsDirectory() && !f2.getIsDirectory()) {
                return -1;
            } else if (!f1.getIsDirectory() && f2.getIsDirectory()) {
                return 1;
            }

            if (f1.getIsDirectory() && f2.getIsDirectory()) {
                // Both are directories: compare by total size ascending
                return Long.compare(f1.getTotalSizeLong(), f2.getTotalSizeLong());
            } else {
                // Both are files: compare by size ascending
                return Long.compare(f1.getSizeLong(), f2.getSizeLong());
            }
        }
    }

    // SORT_D_FILE_POJO_TOTAL_SIZE_DESC Comparator
    private static class SORT_D_FILE_POJO_TOTAL_SIZE_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            // Prioritize directories over files
            if (f1.getIsDirectory() && !f2.getIsDirectory()) {
                return -1;
            } else if (!f1.getIsDirectory() && f2.getIsDirectory()) {
                return 1;
            }

            if (f1.getIsDirectory() && f2.getIsDirectory()) {
                // Both are directories: compare by total size descending
                return Long.compare(f2.getTotalSizeLong(), f1.getTotalSizeLong());
            } else {
                // Both are files: compare by size descending
                return Long.compare(f2.getSizeLong(), f1.getSizeLong());
            }
        }
    }

    private static class SORT_F_FILE_POJO_TOTAL_SIZE_ASC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else if (f1.getIsDirectory() && f2.getIsDirectory())
                return Long.compare(f1.getTotalSizeLong(), f2.getTotalSizeLong());
            else
                return Long.compare(f1.getSizeLong(), f2.getSizeLong());
        }
    }

    private static class SORT_F_FILE_POJO_TOTAL_SIZE_DESC implements Comparator<FilePOJO> {
        @Override
        public int compare(FilePOJO f1, FilePOJO f2) {
            if (!f1.getIsDirectory() && f2.getIsDirectory())
                return -1;
            else if (f1.getIsDirectory() && !f2.getIsDirectory())
                return 1;
            else if (f1.getIsDirectory() && f2.getIsDirectory()) {
                int i = Long.compare(f1.getTotalSizeLong(), f2.getTotalSizeLong());
                return -i;
            } else {
                int i = Long.compare(f1.getSizeLong(), f2.getSizeLong());
                return -i;
            }
        }
    }

    // AppPOJO Comparators
    private static class SORT_APP_POJO_NAME_ASC implements Comparator<AppManagerListFragment.AppPOJO> {
        @Override
        public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2) {
            return NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
        }
    }

    private static class SORT_APP_POJO_NAME_DESC implements Comparator<AppManagerListFragment.AppPOJO> {
        @Override
        public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2) {
            int i = NATURAL_SORT_COMPARATOR.compare(f1.getLowerName(), f2.getLowerName());
            return -i;
        }
    }

    private static class SORT_APP_POJO_TIME_ASC implements Comparator<AppManagerListFragment.AppPOJO> {
        @Override
        public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2) {
            return Long.compare(f1.getDateLong(), f2.getDateLong());
        }
    }

    private static class SORT_APP_POJO_TIME_DESC implements Comparator<AppManagerListFragment.AppPOJO> {
        @Override
        public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2) {
            int i = Long.compare(f1.getDateLong(), f2.getDateLong());
            return -i;
        }
    }

    private static class SORT_APP_POJO_SIZE_ASC implements Comparator<AppManagerListFragment.AppPOJO> {
        @Override
        public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2) {
            return Long.compare(f1.getSizeLong(), f2.getSizeLong());
        }
    }

    private static class SORT_APP_POJO_SIZE_DESC implements Comparator<AppManagerListFragment.AppPOJO> {
        @Override
        public int compare(AppManagerListFragment.AppPOJO f1, AppManagerListFragment.AppPOJO f2) {
            int i = Long.compare(f1.getSizeLong(), f2.getSizeLong());
            return -i;
        }
    }
}
