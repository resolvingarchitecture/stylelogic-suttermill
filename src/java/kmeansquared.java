import java.lang.*;

public class kMeanSquared implements GroupingAlgorithm
{
	MiningData	mine;
	float		dh;

	public kMeanSquared(MiningData _mine)
	{
		this.mine = _mine;
	}

	public void init(int kValue)
	{
		float	maxd = 0, d=0;
		int		userList[],round,newUser,initUser,i,j;
		boolean	pass;
		userList = new int[kValue];

		for (round=0;round<kValue; round++)
		{
			System.out.println("Starting Round "+round);
			maxd=0;d=0;initUser=-1;newUser=-1;
			for (i=0;i<mine.userCount;i++)
			if (mine.users[i].ratings.length >= 30)
			{
				if (i%100==0) System.out.print("|");
				pass=true;
				for (j=0; j<round; j++)
					if (i == userList[j]) pass=false;
				if (pass)
					if (round==0)
					{
						for (j=i+1;j<mine.userCount && maxd<5.0;j++)
						{
							if (j%100==0) System.out.print(".");
							if (mine.users[j].ratings.length >= 30)
							{
								if ((d = evalUserUser(i,j)) > maxd)
								{
									newUser=i;
									initUser=j;
									System.out.println("Users "+i+","+j+" :: d="+d);
									maxd=d;
								}
							}
						}
					}
					else
					{
						if ((d = evalUserList(i,userList,round)) > maxd)
						{
							newUser=i;
							maxd=d;
							System.out.println("User "+i+":List["+round+"] :: d="+d);
						}
					}
			}
			System.out.println("Completed...Max d="+maxd);
			if (round==0 && initUser>=0) userList[round++] = initUser;
			if (newUser>=0) userList[round] = newUser;
			if (newUser>=0) System.out.println("Added User"+newUser);
		}

		for (i=0;i<userList.length;i++)
		{
			mine.groups[i].addUser( mine.users[userList[i]].userIndex, mine.users[userList[i]].ratings, 0);
			mine.users[userList[i]].setGroup(i, 0);
		}
	}

	private float evalUserUser( int user1, int user2 )
	{
		int		d=0,index,left,right,itemIndex;
		int		weight=0;

		for (int i=0; i<mine.users[user1].ratings.length; i++)
		{
			itemIndex = mine.users[user1].ratings[i].itemIndex;
//			System.out.println("-------------Looking for Item "+itemIndex);
			left=0;right=mine.users[user2].ratings.length-1;
			index = right;
			while (itemIndex != mine.users[user2].ratings[index].itemIndex && left+1<right)
			{
//				System.out.println("Left:"+left+" Right:"+right+" Index:"+index);
				index = left+(right-left)/2;
				if (itemIndex > mine.users[user2].ratings[index].itemIndex)
					left=index;
				else
					right=index;
			}
			if (itemIndex == mine.users[user2].ratings[index].itemIndex)
			{
				d += abs( mine.users[user1].ratings[i].rating - mine.users[user2].ratings[index].rating);
				weight++;
			}
		}
		return ( (weight>0)?(float)d/weight:0);
	}

	private float evalUserList( int user, int userList[], int round )
	{
		float d=0;

		for (int j=0; j<round; j++)
			d+=evalUserUser(user,userList[j]);
		return ( d );
	}



	public void start()
	{
		int		brk=100;
		float	dhdt=0;
		do {
			dhdt = 0;
			System.out.println("Scanning Users Timeout-"+brk);
			for (int i=0; i<mine.userCount; i++)
			{
//				System.out.println("Inspecting User-"+i);
				dhdt+=inspectUser(i);
			}
			System.out.println("Final DH/DT-"+dhdt+"------------------------------"+brk);
		} while (dhdt!=0 && brk-->0);
	}

	public boolean isolateUser(int userIndex)
	{
		boolean state=false;
		UserInfo user = mine.users[userIndex];
		if (user != null)
			try
			{
				if (user.groupIndex >=0 )
					if (state = mine.groups[user.groupIndex].removeUser(user.userIndex,user.ratings,user.happiness))
						user.setGroup(-1, 0);
					else System.out.println("Failed to remove User - "+userIndex+" from group "+user.groupIndex);

				else
				{
					System.out.println("No need to Isolate User! No groupIndex for User "+userIndex+"("+user.userID+")");
					state=true;
				}
			}
			catch(Exception ex){ System.err.println("isolateUser Exception on " + userIndex+" : "+ex.getMessage());ex.printStackTrace(System.out);}
		else System.out.println("User - "+userIndex+" does not exist");
		return state;
	}

	public float inspectUser(int userIndex)
	{
		float	temp,h=0,dhdt=0;
		int		bestGroup=-1;
		boolean pass;
		UserInfo user = mine.users[userIndex];

		int userID = user.userID;

//		if (groups[user.groupIndex].memberCount>1)
//			System.out.println("Inspect User "+userIndex+"("+userID+")");
			for (int i=0; i<mine.groupCount; i++)
			{
				if ( (temp=evaluateUserGroup(mine.users[userIndex], mine.groups[i])) > h)
				{
					bestGroup = i;
					h = temp;
				}
//			System.out.println("Group "+i+" H="+temp+" <> "+h+"(best)");

			}
//			System.out.println("Best Group = "+bestGroup+" ("+h+") changed from "+user.groupIndex);
			if (bestGroup != user.groupIndex && bestGroup>=0)
			{
//				System.out.println("User "+userIndex+" Best Group = "+bestGroup+" ("+h+") changed from "+user.groupIndex);
				pass=true;
				if (user.groupIndex>=0)
					pass=mine.groups[user.groupIndex].removeUser(user.userIndex,user.ratings,user.happiness);		//Remove User from old Group.  Subtract old ratings and old Happiness
				if (pass)
				{
					dhdt = h-user.happiness;
					user.happiness = h;
					mine.groups[bestGroup].addUser(user.userIndex,user.ratings,h);					//Add User to new Group. Add new ratings and happiness score to the new group
					user.setGroup(bestGroup, h);
				}
			}
		return dhdt;

	}

private long abs(long a)
{
	return ((a<0)?-a:a);
}

	private float evaluateUserGroup( UserInfo user, GroupInfo group )
	{
		long	distance=0,d;
		int		weight=0;
		if (user.ratings != null)
			for (int i=0; i<user.ratings.length; i++)
			{
				d = gaugeDistance( ((long) user.ratings[i].rating) << 32, group.ratings[user.ratings[i].itemIndex].rating);
				if (d>=0)
				{
	//				System.out.println("d="+d);
					distance += d;
					weight++;
				}
			}
		return ( scoreHappiness(distance,weight) );
	}


	private float evaluateGroupGroup(GroupInfo group1, GroupInfo group2 )
	{
		long	distance=0,d;
		int		weight=0;

		for (int i=0; i<group1.ratingCount; i++)
		{
			d = gaugeDistance( group1.ratings[i].rating, group2.ratings[i].rating);
			if (d>=0)
			{
//				System.out.println("d="+d);
				distance += d;
				weight++;
			}
		}
		return ( scoreHappiness(distance,weight) );
	}

	private long gaugeDistance(long userRating, long groupRating)
	{
		return ( ((groupRating > 0) && (userRating > 0))? ((long)5<<32)-abs(userRating-groupRating):-1 );
	}
	private float scoreHappiness(long distance, int weight)
	{
		return ((weight>0)?((float) distance/weight)/((long) 1<<32):0);
	}
	public void halt()
	{
	}

	public String showGroupErrorStats()
	{
		float d, local_min,local_max,min,max, avg, local_avg,ov_max,ov_min;

		avg=0;
		max = 0;
		min = Float.MAX_VALUE;
		ov_max = 0;
		ov_min = Float.MAX_VALUE;
		d=0;
		System.out.println(" - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		System.out.println("Group Membership Average Errors");
		for (int i=0; i<mine.groupCount; i++)
		{
			local_avg=0;
			local_max = 0;
			local_min = Float.MAX_VALUE;
			for (int j=0;j<mine.groups[i].userList.length;j++)
				for (int k=0; k<8; k++)
					if ((mine.groups[i].userList[j] & (1<<k)) != 0)
					{
						d = (5-evaluateUserGroup( mine.users[j*8+k] , mine.groups[i]));
						local_avg+=d;
						if (d>local_max) local_max = d;
						if (d<local_min) local_min = d;
					}
			mine.groups[i].groupHappiness = d;
			local_avg /= mine.groups[i].memberCount;
			System.out.println("Group "+i+": mem="+mine.groups[i].memberCount+" AvgErr="+local_avg+" MaxErr="+local_max+" MinErr="+local_min);
			if (local_max>max) max = local_max;
			if (local_min<min) min = local_min;
			if (local_avg>ov_max) ov_max = local_avg;
			if (local_avg<ov_min) ov_min = local_avg;
			avg += local_avg;
		}
		System.out.println("............................................................");
		System.out.println("Overall Group Avg Error="+avg/mine.groupCount+" Indv Error Max:"+max+" Min="+min);
		System.out.println("Max Group Error="+ov_max+" Min Group Error="+ov_min);
		System.out.println("............................................................");

		return ("Overall Group Avg Error="+avg/mine.groupCount+" Indv Error Max:"+max+" Min="+min);
	}

	public String showGroupDistanceStats()
	{
		float d, local_min,local_max,min,max, avg, local_avg;

		System.out.println(" - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		System.out.println("Group Distances");

		avg=0;
		max = 0;
		min = Float.MAX_VALUE;
		for (int i=0; i<mine.groupCount; i++)
		{
			local_max = 0;
			local_min = Float.MAX_VALUE;
			local_avg=0;
			for (int j=0;j<mine.groupCount;j++)
				if (j!=i)
				{
					d = 5-evaluateGroupGroup(mine.groups[i],mine.groups[j]);
					local_avg+=d;
					if (d>local_max)
					{
						local_max = d;
						if (d>max) max=d;
					}
					if (d<local_min)
					{
						local_min = d;
						if (d<min) min=d;
					}

				}
			avg+=local_avg;
			System.out.println("Mean Dist="+local_avg/mine.groupCount+" Max Dist="+local_max+" Min Dist="+local_min);
		}
		System.out.println("............................................................");
		System.out.println("Overall Mean="+avg/mine.groupCount/mine.groupCount+" Overall Max="+max+" Overall Min="+min);
		return ("Overall Mean="+avg/mine.groupCount/mine.groupCount+" Overall Max="+max+" Overall Min="+min);
	}

	public float compileHappiness()
	{
		float h=0;

		for (int i=0; i<mine.groupCount; i++)
			h+= mine.groups[i].groupHappiness;

		return h;
	}

	public void refreshStats(int g)
	{
		int		i,j,count=0,votes, n, ratingN[],r,uIndex,iIndex;
		double	memberSD, pe, mean, sd;
		double	ratingSD[];

		long 	SHIFT_32 = (long)1<<32;
		double 	SHIFT_64 = (double)SHIFT_32 * (double)SHIFT_32;

		n=0;
		mean=0;
		ratingSD = new double[mine.groups[g].ratingCount];
		ratingN  = new int[mine.groups[g].ratingCount];

		for (i=0; i <mine.groups[g].ratingCount; i++)
		{
			ratingN[i] = 0;
			ratingSD[i] = (double) 0;
			if (mine.groups[g].ratings[i].weight > 0)
			{
				n++;
				mean += mine.groups[g].ratings[i].rating;
			}
		}
		mean /= (double) n;
		sd=0;
		for (i=0; i <mine.groups[g].ratingCount; i++)
			if (mine.groups[g].ratings[i].weight > 0)
				sd += ((mine.groups[g].ratings[i].rating-mean) * (mine.groups[g].ratings[i].rating-mean));

		sd = Math.sqrt( sd/SHIFT_64/(double) (n-1) );
		mean /= SHIFT_32;

		pe =0;
		i=0;j=0;
		try
		{
		for (i=0; i<mine.groups[g].userList.length; i++)
			for (j=0; j<8; j++)
				if ((mine.groups[g].userList[i] & (1<<j)) > (byte) 0)
				{
					pe += 5-mine.users[i*8+j].happiness;
				}
}catch(Exception e) {System.out.println("Failed to access user "+(i*8+j)+" for group "+g);}
		pe = pe/mine.groups[g].memberCount;

		for (i=0; i<mine.groups[g].userList.length; i++)
			for (j=0; j<8; j++)
				if ((mine.groups[g].userList[i] & (1<<j)) > (byte) 0)
				{
					uIndex = i*8+j;
					for (int k=0; k<mine.users[uIndex].ratings.length; k++)
						if (mine.users[uIndex].ratings[k].rating > 0)
						{
							iIndex = mine.users[uIndex].ratings[k].itemIndex;
							ratingN[ iIndex ]++;
							ratingSD[ iIndex ] += (double)	(mine.groups[g].ratings[iIndex].rating-(long)mine.users[uIndex].ratings[k].rating*((long)1<<32))*
															(mine.groups[g].ratings[iIndex].rating-(long)mine.users[uIndex].ratings[k].rating*((long)1<<32));
						}

				}
		memberSD= (double) 0.0;
		count=0;
		for (i=0; i <mine.groups[g].ratingCount; i++)
			if (ratingN[i] > 1)
			{
				memberSD += Math.sqrt( (double) ratingSD[i]/SHIFT_64/(ratingN[i]-1) );
				count++;
			}
		if (count>1)
			 mine.groups[g].Member_Item_Rating_SD =  memberSD/(double)(count);
		else mine.groups[g].Member_Item_Rating_SD = -1;


		mine.groups[g].Predictive_Error = pe;
		mine.groups[g].Item_Rating_N = n;
		mine.groups[g].Item_Rating_Mean = mean;
		if (n>1)
			 mine.groups[g].Item_Rating_SD = sd;
		else mine.groups[g].Item_Rating_SD = -1;

		if (mine.groups[g].Member_Item_Rating_SD > 0)
			mine.groups[g].groupHappiness = mine.groups[g].Predictive_Error / mine.groups[g].Member_Item_Rating_SD;
		else
			mine.groups[g].groupHappiness = mine.groups[g].Predictive_Error * 25;


	}




}

