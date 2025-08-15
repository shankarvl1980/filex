package svl.kadatha.filex;

import android.os.Parcel;
import android.os.Parcelable;

public class FilePOJO implements Parcelable {
    public static final Creator<FilePOJO> CREATOR = new Creator<FilePOJO>() {
        @Override
        public FilePOJO createFromParcel(Parcel in) {
            return new FilePOJO(in);
        }

        @Override
        public FilePOJO[] newArray(int size) {
            return new FilePOJO[size];
        }
    };
    private final String lower_name;
    private final String package_name;
    private FileObjectType fileObjectType;
    private String name;
    private String path;
    private boolean isDirectory;
    private long dateLong;
    private String date;
    private long sizeLong;
    private String size;
    private int type;
    private String ext;
    private float alfa;
    private int play_overlay_visible;
    private int pdf_overlay_visible;
    private int totalFiles;
    private long totalSizeLong;
    private String totalSize;
    private double totalSizePercentageDouble;
    private String totalSizePercentage;
    private String checksum;
    private boolean whetherExternal;

    FilePOJO(FileObjectType fileObjectType, String n, String p_n, String p, boolean dir, long dl, String d, long sl, String s, int t,
             String ext, float a, int play_o, int pdf_o, int tf, long tsl, String ts, double tspd, String tsp, String checksum) {
        this.fileObjectType = fileObjectType;
        this.name = n;
        this.lower_name = n.toLowerCase();
        this.package_name = p_n;
        this.path = p;
        this.isDirectory = dir;
        this.dateLong = dl;
        this.date = d;
        this.sizeLong = sl;
        this.size = s;
        this.type = t;
        this.ext = ext;
        this.alfa = a;
        this.play_overlay_visible = play_o;
        this.pdf_overlay_visible = pdf_o;
        this.totalFiles = tf;
        this.totalSizeLong = tsl;
        this.totalSize = ts;
        this.totalSizePercentageDouble = tspd;
        this.totalSizePercentage = tsp;
        this.checksum = checksum;
    }

    protected FilePOJO(Parcel in) {
        lower_name = in.readString();
        package_name = in.readString();
        int fileObjectTypeOrdinal = in.readInt();
        if (fileObjectTypeOrdinal == -1) {
            fileObjectType = null;
        } else {
            fileObjectType = FileObjectType.values()[fileObjectTypeOrdinal];
        }
        name = in.readString();
        path = in.readString();
        isDirectory = in.readByte() != 0;
        dateLong = in.readLong();
        date = in.readString();
        sizeLong = in.readLong();
        size = in.readString();
        type = in.readInt();
        ext = in.readString();
        alfa = in.readFloat();
        play_overlay_visible = in.readInt();
        pdf_overlay_visible = in.readInt();
        totalFiles = in.readInt();
        totalSizeLong = in.readLong();
        totalSize = in.readString();
        totalSizePercentageDouble = in.readDouble();
        totalSizePercentage = in.readString();
        checksum = in.readString();
        whetherExternal = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(lower_name);
        dest.writeString(package_name);
        dest.writeInt(fileObjectType == null ? -1 : fileObjectType.ordinal());
        dest.writeString(name);
        dest.writeString(path);
        dest.writeByte((byte) (isDirectory ? 1 : 0));
        dest.writeLong(dateLong);
        dest.writeString(date);
        dest.writeLong(sizeLong);
        dest.writeString(size);
        dest.writeInt(type);
        dest.writeString(ext);
        dest.writeFloat(alfa);
        dest.writeInt(play_overlay_visible);
        dest.writeInt(pdf_overlay_visible);
        dest.writeInt(totalFiles);
        dest.writeLong(totalSizeLong);
        dest.writeString(totalSize);
        dest.writeDouble(totalSizePercentageDouble);
        dest.writeString(totalSizePercentage);
        dest.writeString(checksum);
        dest.writeByte((byte) (whetherExternal ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and setters
    public FileObjectType getFileObjectType() {
        return this.fileObjectType;
    }

    public void setFileObjectType(FileObjectType f) {
        this.fileObjectType = f;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getLowerName() {
        return lower_name;
    }

    public String getPackage_name() {
        return this.package_name;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String p) {
        this.path = p;
    }

    public boolean getIsDirectory() {
        return this.isDirectory;
    }

    public void setIsDirectory(boolean dir) {
        this.isDirectory = dir;
    }

    public long getDateLong() {
        return this.dateLong;
    }

    public void setDateLong(long dl) {
        this.dateLong = dl;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String d) {
        this.date = d;
    }

    public long getSizeLong() {
        return this.sizeLong;
    }

    public void setSizeLong(long sl) {
        this.sizeLong = sl;
    }

    public String getSize() {
        return this.size;
    }

    public void setSize(String s) {
        this.size = s;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int t) {
        this.type = t;
    }

    public String getExt() {
        return this.ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public float getAlfa() {
        return this.alfa;
    }

    public void setAlfa(float alfa) {
        this.alfa = alfa;
    }

    public int getPlayOverlayVisibility() {
        return this.play_overlay_visible;
    }

    public void setPlayOverlayVisibility(int play_overlay_visible) {
        this.play_overlay_visible = play_overlay_visible;
    }

    public int getPdfOverlayVisibility() {
        return this.pdf_overlay_visible;
    }

    public void setPdfOverlayVisibility(int pdf_overlay_visible) {
        this.pdf_overlay_visible = pdf_overlay_visible;
    }

    public int getTotalFiles() {
        return this.totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalSizeLong() {
        return this.totalSizeLong;
    }

    public void setTotalSizeLong(long totalSizeLong) {
        this.totalSizeLong = totalSizeLong;
    }

    public String getTotalSize() {
        return this.totalSize;
    }

    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }

    public double getTotalSizePercentageDouble() {
        return this.totalSizePercentageDouble;
    }

    public void setTotalSizePercentageDouble(double totalSizePercentageDouble) {
        this.totalSizePercentageDouble = totalSizePercentageDouble;
    }

    public String getTotalSizePercentage() {
        return this.totalSizePercentage;
    }

    public void setTotalSizePercentage(String totalSizePercentage) {
        this.totalSizePercentage = totalSizePercentage;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public boolean getWhetherExternal() {
        return whetherExternal;
    }

    public void setWhetherExternal(boolean whetherExternal) {
        this.whetherExternal = whetherExternal;
    }
}
