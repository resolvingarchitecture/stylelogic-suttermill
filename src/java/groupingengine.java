import java.io.IOException;
import java.sql.*;

import java.util.*;

public class GroupingEngine implements EngineControls //extends Frame
{
	MiningData			mine;
//	GroupInfo			groups[];
//	UserInfo			users[];
	float				totalHappiness;
	kMeanSquared		kMean;
	JDBC				db;
	private int			itemCount,kValue=80;
	LinkedList			taskList;

	ProcessingDaemon	processingDaemon;
	GroupingDaemon		groupingDaemon;
	LazyWriter			lazyWriter;
	TaskListener		listener;

	DatabasePortal		dbPortal;

	int	GroupingDaemon_timeout = 600;
	int	ProcessingDaemon_timeout = 10;
	int	LazyWriter_timeout = 6;
	int	TaskListener_timeout = 1;

	final static byte	MIN_RATING_COUNT = 15;
				 int	grouping_iterations = 5;

	private Interface	gui;
	private	boolean		halt = false, active=false;
	private int			processRequests=0;

	public GroupingEngine()
	{
		long		startTime,endTime0,endTime1,endTime2,endTime3;
		Connection 	initConn=null;

		System.out.println("...................");
		System.out.println("   Sutters Mill    ");
		System.out.println("Now Initializating ");
		System.out.println("...................");

		startTime = System.currentTimeMillis();;
		taskList = new LinkedList();
		gui = new Interface(this);
		gui.addMessage("Initializing Sutters Mill");
//		db = new JDBC("jdbc:odbc:JamesSuttersMill");
		db = new JDBC("jdbc:odbc:db_PredictionEngine", false);
		try{
			initConn = db.createConnection();
			mine = new MiningData();

			dbPortal = new DatabasePortal(mine,db);


			gui.addMessage("Initializing Indicies");
			dbPortal.LoadIndicies(initConn);
			for (int i=0; i<kValue;i++)
			{
	//			dbPortal.RemoveOldGroup(initConn,i);
				dbPortal.StoreNewGroup(initConn,i);
			}

			gui.addMessage("Loading User Ratings");
			dbPortal.LoadUserRatings(initConn,0,mine.getUserCount());
			endTime0 = System.currentTimeMillis();;
			kMean = new kMeanSquared(mine);

			System.out.println("Initializing GroupingAlgorithm");
			gui.addMessage("Loading Group Ratings & Reviews");
			dbPortal.LoadGroupByUserMembership(initConn);

			processingDaemon = new ProcessingDaemon(gui);
			processingDaemon.start();
			groupingDaemon = new GroupingDaemon(gui);
			groupingDaemon.start();
			lazyWriter = new LazyWriter(gui);
			lazyWriter.start();
			listener = new TaskListener(gui);
			listener.start();

			gui.setActive(true);

			initConn.close();
			System.out.println("Initilization complete");
			System.out.println("Waiting for users...");
			gui.addMessage("Initilization complete");
			active=true;
		}
		catch(SQLException ex)
		{
			System.err.println("Init SQLException: " + ex.getMessage());
			System.err.println("Failed to create database connection");
		}
	}



    public static void main(String[] args)
    {
		GroupingEngine g = new GroupingEngine();

		g.Interface();
		System.exit(0);
    }


    public void showGroupErrorStats()
    {
		gui.addMessage(kMean.showGroupErrorStats());
	}
	public void showGroupDistanceStats()
	{
		gui.addMessage(kMean.showGroupDistanceStats());
	}
	public boolean haltProcessing()
	{
		halt =true;
		return (active);
	}

    public void Interface()
    {
		byte b[],c=0;
		int count;

		b = new byte[1];
		count=0;
		System.out.println("\n(h)alt processing\n(s)ave groups\ngroup (d)istance stats\ngroup (e)rror stats:");
		do
		{
			c=0;
			count++;
			if (count == 10*1800)
			{
				System.out.println("\n(h)alt processing\n(s)ave groups\ngroup (d)istance stats\ngroup (e)rror stats:");
				count=0;
			}
			try
			{
				while (System.in.available() > 0)
				{
					System.in.read(b);
					c=b[0];
					System.in.read(b);
					System.in.read(b);
				}
			} catch(IOException ex){ System.err.println("IOException: " + ex.getMessage());}
			if (c > 0) System.out.print((char) c);
			if (c == 115) StoreGroups();
			if (c == 101) showGroupErrorStats();
			if (c == 100) showGroupDistanceStats();

			try
			{
				Thread.sleep(100);
			} catch(InterruptedException ex){ System.err.println("InterruptException: " + ex.getMessage());}
		} while (c != 104 && !halt);
		ShutDown();
	}

	public void ShutDown()
	{
		int i=0;

		gui.addMessage("Shutting Down All Daemons");

		groupingDaemon.setActiveState(false);
		groupingDaemon.interrupt();
		listener.setActiveState(false);
		listener.interrupt();
		System.out.println("Shutting Down Task Listener & Grouping Daemon-");
		while (groupingDaemon.isAlive() || listener.isAlive())
		{
			if ((i++ % 50) == 0) System.out.print("*");
		}
		gui.addMessage("Task Listener Daemon Stopped");
		gui.addMessage("Grouping Daemon Stopped");

		processingDaemon.setActiveState(false);
		processingDaemon.interrupt();
		System.out.println("Shutting Down Processing  Daemon-");
		while (processingDaemon.isAlive())
		{
			if ((i++ % 50) == 0) System.out.print("*");
		}
		gui.addMessage("Processing Daemon Stopped");

		lazyWriter.setActiveState(false);
		lazyWriter.interrupt();
		System.out.println("Shutting Down Lazy Writer Daemon-");
		while (processingDaemon.isAlive() || groupingDaemon.isAlive() || lazyWriter.isAlive() || listener.isAlive())
		{
			if ((i++ % 50) == 0) System.out.print("*");
		}
		gui.addMessage("Lazy Writer Daemon Stopped");

		gui.dispose();
		System.out.println("...................");
		System.out.println("   Sutters Mill    ");
		System.out.println("Shut Down Completed");
		System.out.println("...................");
	}

	public void refresh()
	{
		float h = 0;
		totalHappiness = kMean.compileHappiness();
	}

	public void setGroupingIterations(int n)
	{
		grouping_iterations = n;
	}
	public void startGroupingEngine()
	{
		groupingDaemon.interrupt();
	}
	public void setGroupingIntermission(int seconds)
	{
		GroupingDaemon_timeout = seconds;
	}

	public void incrementProcessRequests(int count) { processRequests += count; }
	public void resetProcessRequests(int count) { processRequests = 0; }
	public int getProcessRequests() { return processRequests; }

    class ProcessingDaemon extends Thread
    {
		boolean 	active=true;
		Connection	processingConn;
		Interface 	gui;

		public ProcessingDaemon(Interface _gui)
		{
			gui = _gui;
			System.out.println("Starting Processing Daemon ");
			try
			{
				processingConn = db.createConnection();
			}
			catch(SQLException ex)
			{	System.err.println("TaskListener SQLException: " + ex.getMessage()); active=false;}
		}
		public void setActiveState(boolean _active)	{	active=_active;	}

        public void run()
        {
			try
			{
			do {
				if (taskList.size()>0) ProcessTaskList();
				try
				{
					sleep(1000*ProcessingDaemon_timeout);
				}
				catch(InterruptedException ex){ System.err.println("ProcessingDaemon Awakened");}
			} while (active);
			System.out.println("Shutting Down Processing Daemon");
			}
			catch(Exception ex){ System.err.println("ProcessingDaemon Exception: " + ex.getMessage());ex.printStackTrace(System.out);}
        }

		public void ProcessTaskList()
		{
			int id,index;

			while ((taskList.size()) > 0)
			{
				id = ((Integer) taskList.removeFirst()).intValue();
				if (!mine.validUserID(id))
				{
					index = mine.addUser(id);
//					userIndex.addID(id);
//					index = mine.getUserIndex(id);
					System.out.println("Adding new User "+id+" ("+index+"/"+mine.getUserIndexSize()+")");
//					mine.addUser(index,id);
				}
				else
					index = mine.getUserIndex(id);
				try
				{
					System.out.print("Processing user "+id+" (g="+mine.users[index].groupIndex+")");
					if (kMean.isolateUser(index))								//Must Remove User from Old Group first
					{
						dbPortal.LoadIndividualRatings(processingConn,index);				//Load User's new ratings, which have changed
						System.out.print(" -- ");
						if (mine.users[index].ratings.length >= MIN_RATING_COUNT)
						{
							kMean.inspectUser(index);									//Locate the best group the user with the new ratings
							dbPortal.StoreUserMembership(processingConn,index);
						}
						System.out.println("Completed (newG="+mine.users[index].groupIndex+")");
					}
					else System.err.println("- ProcessTaskList failed to isolate user "+id);
				}
				catch(Exception ex){ System.err.println("ProcessTaskList Exception on user "+id+"::" + ex.getMessage());ex.printStackTrace(System.out);}
			}
		}

    }

    class GroupingDaemon extends Thread
    {
		boolean 	active=true;
		Interface 	gui;

		public GroupingDaemon(Interface _gui)
		{
			gui = _gui;
			System.out.println("Starting Grouping Daemon Daemon");
		}
		public void setActiveState(boolean _active)	{	active=_active;	}

        public void run()
        {

			try
			{
			do {
//				gui.addMessage("Forced Garbage Clean Up");
//				System.gc();
				if (getProcessRequests() == 0)
				{
					SiftUsers(0.05);
					gui.addMessage("Idle Process Requests.  Sifting Users through filter");
				}
				else
					gui.addMessage(getProcessRequests()+" Process Requests. No Sifting.");

				gui.addMessage("Scanning Users and Groups");
				ScanUsers();
				gui.addMessage("Done Scanning Users and Groups");
				StoreUsers();
				StoreGroups();
				try
				{
					sleep(1000*GroupingDaemon_timeout);
				}
				catch(InterruptedException ex){ System.err.println("GroupingDaemon Awakened");}
			} while (active);
			System.out.println("Shutting Down Grouping Daemon");
			}
			catch(Exception ex){ System.err.println("GroupingDaemon Exception: " + ex.getMessage());ex.printStackTrace(System.out);}
        }

		private void SiftUsers(double filter)
		{
			int		brk=30,dx,dg;
			float	dhdt=-1,dh,vdhdt=0;

			for (int i=0; i<mine.getUserCount() && active; i++)
				if (Math.random() < filter)
					kMean.isolateUser(i);
		}


		private void ScanUsers()
		{
			int		brk,dx,dg;
			float	dhdt=-1,dh,vdhdt=0;

			brk = grouping_iterations;
			gui.addMessage("Reactivating Group Processing ("+brk+" Rounds)");
			try
			{
				System.out.println("Scanning Users("+mine.getUserCount()+") Timeout in "+brk);
				while (dhdt != 0 && active && brk > 0)
				{
					dx=0;
					dh=0;
					dhdt=0;
					vdhdt=0;
					for (int i=0; i<mine.getUserCount() && active; i++)
						if (mine.users[i] != null)
						if (mine.users[i].ratings.length >= MIN_RATING_COUNT)
						{
			//				System.out.println("Inspecting User-"+i);
							dh = kMean.inspectUser(i);
							if (dh != 0)
							{
								dhdt+=Math.abs(dh);
								vdhdt+=dh;
								dx++;
							}
						}
					System.out.println("Final: DH/DT="+vdhdt+" |dh/dt|="+dhdt+" ("+(brk--)+") dx/dt="+dx+" -----------");
				}
				for (int i=0; i<mine.getGroupCount() && active; i++)
					mine.groups[i].refresh();
			}
			catch(Exception ex){ System.err.println("ScanUsers Exception: " + ex.getMessage());ex.printStackTrace(System.out);}

		}

    }

	public void StoreUsers()	{	lazyWriter.task[1] = true;System.out.println("Prepare to Store Users"); }
	public void StoreGroups()	{	lazyWriter.task[0] = true;System.out.println("Prepare to Store Groups"); }

    class LazyWriter extends Thread
    {
		boolean 	active=true,task[];
		Connection	lazyWriterConn;
		Interface 	gui;

		public LazyWriter(Interface _gui)
		{
			gui = _gui;
			task = new boolean[2];
			task[0] = false;
			task[1] = false;
			try
			{
				lazyWriterConn = db.createConnection();
			}
			catch(SQLException ex)
			{	System.err.println("TaskListener SQLException: " + ex.getMessage()); active=false;}
			System.out.println("Starting Lazy Writer Daemon");
		}
		public void setActiveState(boolean _active)	{	active=_active;	}
        public void run()
        {
			int i;
			try
			{
			do {
//				System.out.println("LazyWriter looking for work.");
				for (i=0;i<task.length;i++)
					if (task[i]) ExecuteTask(i);
				try	{ sleep(1000*LazyWriter_timeout);	}
				catch(InterruptedException ex)
				{
					System.err.println("LazyWriter Awakened");
				}
			} while (active);
			System.out.println("LazyWriter Writing Remaining Data");

			for (i=0;i<task.length;i++) ExecuteTask(i);	//Do a final write to the database

			System.out.println("Shutting Down LazyWriter");
			}
			catch(Exception ex){ System.err.println("LazyWriter Exception: " + ex.getMessage());ex.printStackTrace(System.out);}
        }
		private void ExecuteTask(int taskID)
		{
			try
			{
				System.out.println("LazyWriter Executing Task "+taskID);
				switch (taskID)
				{
					case 0 :	{
									gui.addMessage("Start Writing to Database - User Memberships");
									dbPortal.UpdateUserInfo(lazyWriterConn);
									gui.addMessage("Completed Writing to Database - User Memberships");
								} break;
					case 1 : 	{
									gui.addMessage("Start DTS - Group Ratings & Reviews");
									int groupCount = mine.getGroupCount();
									for (int i=0;i<groupCount;i++)
										kMean.refreshStats(i);
									dbPortal.StoreGroupRatings(lazyWriterConn);
									gui.addMessage("Completed DTS - Group Ratings & Reviews");
								} break;
				}
			}
			catch(Exception ex){ System.err.println("LazyWriter ExecuteTask Exception task ("+taskID+") : " + ex.getMessage());ex.printStackTrace(System.out);return;}
			task[taskID] = false;
		}
    }


    class TaskListener extends Thread
    {
		boolean 			active=true;
		Connection			taskListenerConn;
		PreparedStatement	ps;
		Interface			gui;

		public TaskListener(Interface _gui)
		{
			System.out.println("Starting Listener Thread");
			gui = _gui;
			try
			{
				taskListenerConn = db.createConnection();
				ps = db.createPreparedStatement(taskListenerConn,"exec usp_DM_User_Process_Request");
			}
			catch(SQLException ex)
			{	System.err.println("TaskListener SQLException: " + ex.getMessage()); active=false;}
		}

		public void setActiveState(boolean _active)	{	active=_active;	}

        public void run()
        {
			while (active)
			{
				BuildTaskList();
				try
				{
					sleep(1000*TaskListener_timeout);
				}
				catch(InterruptedException ex){ System.err.println("TaskListener Awakened");}
			}
			System.out.println("Shutting Down Listener Thread");
        }

		private int BuildTaskList()
		{
			int count=0;
			int userID,uIndex;
			String s;
			try
			{

	//			System.out.print("Reading Task List----------------");
				ResultSet rs = db.executePreparedStatement(ps);
				s="Request to Process :";
				while (rs.next())
				{
					count++;
					userID = rs.getInt("userID");
					s+=userID+";";
					taskList.add((Object) new Integer(userID));
				}
				if (count > 0)
				{
					System.out.println(s);
					gui.addMessage(s);
				}
				rs.close();

			}
			catch(Exception ex)
			{	System.err.println("ReadTaskList SQLException: " + ex.getMessage());	return 0;	}
			if (count>0) processingDaemon.interrupt();
			incrementProcessRequests(count);
			return (count);
		}

    }


}





