public class GroupInfo{
	double			groupHappiness,user_growthFactor,item_growthFactor;
	double			Member_Item_Rating_SD,Item_Rating_SD,Item_Rating_Mean,Predictive_Error;
	int				Item_Rating_N;
	int				memberCount,groupIndex,value_fargID,hist_fargID,dx,ratingCount=0;
	groupRatings[]	ratings;
	byte[]			userList;
	boolean			modified;

	final static short DIRTY_DX= 10;

	public GroupInfo(int _groupIndex, int itemCount, int userCount, double _item_growthFactor, double _user_growthFactor)
	{
		dx=0;
		value_fargID = -1;
		hist_fargID = -1;
		user_growthFactor = 1.0+_user_growthFactor;
		item_growthFactor = 1.0+_item_growthFactor;
		value_fargID = 82+_groupIndex;
		hist_fargID = 132+_groupIndex;
		groupIndex =_groupIndex;
		memberCount=0;
		userList = new byte[(int)((userCount/8+1)*user_growthFactor)];
		ratings = new groupRatings[(int)(itemCount*item_growthFactor)];
		for (int i=0;i<userList.length;i++)
			userList[i]=0;
		for (int i=0;i<itemCount;i++)
			ratings[i] = new groupRatings(0, (short) 0);
		modified = false;

		ratingCount = itemCount;
		groupHappiness = 0;
		Member_Item_Rating_SD = 0;
		Item_Rating_SD = 0;
		Item_Rating_Mean =0;
		Predictive_Error = 0;
		Item_Rating_N = 0;
	}

	public void refresh()
	{
		int		count=0,votes;
		long	avg;

		if (dx > DIRTY_DX)
		{
//			System.out.println("Cleaning Data for Group "+groupIndex);
			dx=0;
			for (int i=0; i <ratingCount; i++)
			{
				avg=0;votes=0;
				for (int j=1; j<ratings[i].ratings.length; j++)
				{
					avg = avg + j*ratings[i].ratings[j];
					votes += ratings[i].ratings[j];
				}
				if (votes > 0) ratings[i].rating = (avg << 32) / votes;
			}
		}
	}

	public boolean validateItemReview(int i)
	{
		float avg;
		short votes;
		boolean valid;

		avg = (float) 0.0;
		votes = 0;
		for (int j=1; j<ratings[i].ratings.length; j++)
		{
			avg = avg + j*ratings[i].ratings[j];
			votes += ratings[i].ratings[j];
		}
		if (votes > 0)
			avg /= votes;
		valid = (Math.abs( (float) ratings[i].rating/ ((long) 1 << 32)-avg) < 0.001);
if (!valid) 	System.out.print((float) ratings[i].rating/ ((long) 1 << 32)+" V "+avg+" ");
		ratings[i].valid = valid;
		return valid;
	}


	public void addUser(int userIndex, itemRatings[] userRatings, float userHappiness)
	{
		int		itemIndex;
		boolean	valid;

		dx++;
		for (int i=0; i <userRatings.length; i++)
			if (userRatings[i].rating > 0)
			{
				itemIndex = userRatings[i].itemIndex;
				ratings[itemIndex].modified = true;
	//			System.out.println("group "+groupIndex+", item "+itemIndex+", weight "+ratings[itemIndex].weight);
				ratings[itemIndex].rating = (long) (ratings[itemIndex].rating*ratings[itemIndex].weight+((long)userRatings[i].rating<<32))/(ratings[itemIndex].weight+1);
				ratings[itemIndex].weight++;
				ratings[itemIndex].ratings[ userRatings[i].rating ]++;

/*
				if (ratings[i].valid)
					if (!validateItemReview(itemIndex))
					{
						System.out.print("User "+userIndex+"("+userRatings[i].rating+")-->"+groupIndex+"(dx="+dx+") failed; Item "+itemIndex+"::"+( (float)ratings[itemIndex].rating/((long) 1<<32) )+" ("+ratings[itemIndex].weight+")");
						for (int k=1;k<7;k++)
							System.out.print(ratings[itemIndex].ratings[k]+" ");
						System.out.println("");
					}
*/
			}

//		groupHappiness += userHappiness;
		while (userIndex/8 >= userList.length) expandUsers();
		userList[userIndex/8] ^= 1<<userIndex%8;
		memberCount++;
		modified = true;
	}

	public boolean removeUser(int userIndex, itemRatings[] userRatings, float userHappiness)
	{
		int		itemIndex=0,i=0;

		if (memberCount > 1)
		{
			dx++;
			try{
				modified = true;
				for (i=0; i <userRatings.length; i++)
					if (userRatings[i].rating > 0)
					{
						itemIndex = userRatings[i].itemIndex;
						ratings[itemIndex].modified = true;
						if (ratings[itemIndex].weight > 1)
						{
							ratings[itemIndex].rating = (long) (ratings[itemIndex].rating*ratings[itemIndex].weight-((long)userRatings[i].rating<<32))/(ratings[itemIndex].weight-1);
							ratings[itemIndex].weight--;
						}
						else
						{
							ratings[itemIndex].rating=0;
							ratings[itemIndex].weight=0;
						}
						if (ratings[itemIndex].ratings[ userRatings[i].rating ] > 0)
							ratings[itemIndex].ratings[ userRatings[i].rating ]--;

/*
						if (ratings[i].valid)
							if (!validateItemReview(itemIndex))
							{
								System.out.print("User "+userIndex+"("+userRatings[i].rating+")<==="+groupIndex+"(dx="+dx+") failed; Item "+itemIndex+"::"+( (float)ratings[itemIndex].rating/((long) 1<<32) )+" ("+ratings[itemIndex].weight+")");
								for (int k=1;k<7;k++)
									System.out.print(ratings[itemIndex].ratings[k]+" ");
								System.out.println("");
							}
*/

					}
//				groupHappiness -= userHappiness;
				userList[userIndex/8] ^= 1<<userIndex%8;
				memberCount--;
			} catch(Exception ex){
				System.out.println(itemIndex+" rating "+userRatings[i].rating+" weight="+ratings[itemIndex].weight);

				System.err.println("removeUser Exception: " + ex.getMessage());ex.printStackTrace(System.out);}
			return true;
		}
		else return false;
	}

	public long getRating(int itemIndex)
	{	return ((itemIndex<ratingCount)?ratings[itemIndex].rating:-1);}

	public void addItem(int id)
	{
		while (id >= ratings.length) expandItems();
		ratings[id] = new groupRatings(0,(short)0);
		ratingCount++;
	}

	private void expandUsers()
	{
		byte[] newUserList;

		newUserList = new byte[(int)(userList.length*user_growthFactor)];
		System.out.println("Expanding userList array from "+userList.length+" to "+(int)(userList.length*user_growthFactor));
		for (int i=0; i <userList.length; i++)
			newUserList[i] = userList[i];
		for (int i=userList.length; i<newUserList.length; i++)
			newUserList[i] = 0;
		userList = newUserList;
	}

	private void expandItems()
	{
		groupRatings[] newRatings;

		newRatings = new groupRatings[(int)(ratings.length*item_growthFactor)];
		for (int i=0; i <ratings.length; i++)
			newRatings[i] = ratings[i];
		for (int i=ratings.length; i<newRatings.length; i++)
			newRatings[i] = new groupRatings(0,(short) 0);
		ratings = newRatings;
	}
}


class groupRatings
{
	long	rating;
	boolean valid = true;
	boolean modified = false;
	short	weight;
	short[]	ratings = {0,0,0,0,0,0,0};

	public groupRatings()
	{	rating=0; weight=0;}
	public groupRatings(int _rating, short _weight)
	{	rating=_rating; weight=_weight;}
	public groupRatings(int _rating, short _weight, short _ratings[])
	{	rating=_rating; weight=_weight; ratings=_ratings; }
}