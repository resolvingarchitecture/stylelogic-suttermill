public interface EngineControls
{
  public void showGroupErrorStats();
	public void showGroupDistanceStats();
	public boolean haltProcessing();
	public void setGroupingIterations(int n);
	public void startGroupingEngine();
	public void setGroupingIntermission(int seconds);
}