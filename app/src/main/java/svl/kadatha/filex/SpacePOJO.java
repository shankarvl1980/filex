package svl.kadatha.filex;

public class SpacePOJO
{

    private final long total_space;
	private long used_space;
	private long available_space;
	
	private final String total_space_readable;
	private String used_space_readable;
	
	
	
	SpacePOJO(String path,long total_space,long available_space)
	{
        this.total_space=total_space;
		this.available_space=available_space;
		this.used_space=this.total_space-this.available_space;
		
		this.total_space_readable=FileUtil.humanReadableByteCount(this.total_space,Global.BYTE_COUNT_BLOCK_1000);
		this.used_space_readable=FileUtil.humanReadableByteCount(this.used_space,Global.BYTE_COUNT_BLOCK_1000);
	}
	
	public void putAvailableSpace(long space)
	{
		this.available_space=space;
		this.used_space=this.total_space-this.available_space;
		this.used_space_readable=FileUtil.humanReadableByteCount(this.used_space,Global.BYTE_COUNT_BLOCK_1000);
	}
	
	public String getTotalSpaceReadable()
	{
		return this.total_space_readable;
	}

	public long getTotalSpace()
	{
		return total_space;
	}
	
	public String getUsedSpaceReadable()
	{
		return this.used_space_readable;
	}
	
	public long getAvailableSpace()
	{
		return this.available_space;
	}
}
