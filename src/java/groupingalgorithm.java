public interface GroupingAlgorithm
{
	public void init(int kValue);
	public void start();
	public boolean isolateUser(int userIndex);
	public String showGroupErrorStats();
	public String showGroupDistanceStats();
	public float compileHappiness();
}