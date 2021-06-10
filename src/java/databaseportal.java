import java.io.InputStream;
import java.io.IOException;
import java.io.*;
import java.sql.*;

import java.util.*;

public class DatabasePortal
{
	MiningData	mine;
	JDBC		db;
	private int	kValue = 80;
	final static int	MIN_RATINGES_PER_PREDICTON = 1;

	public DatabasePortal(MiningData _mine, JDBC _db)
	{
		mine = _mine;
		db = _db;
	}

	public void LoadIndicies(Connection con)
	{
		int[]	itemIDs;
		int	i,g;
		int		count,Index;

		try
		{

			System.out.println("Quering Item Indicies-------------------");
			ResultSet rs = db.Query(con,"exec usp_DM_Index_item_count");
			rs.next();count = rs.getInt(1);rs.close();
			itemIDs = new int[count];
			rs = db.Query(con,"exec usp_DM_Index_item_select");
			i=0;
	        System.out.println("Initializing Item Indicies-------------------");
			while (rs.next())
			{
				itemIDs[i++] = rs.getInt(1);
			}
			rs.close();
			mine.prepareItemIndex(itemIDs);
	        System.out.println("Completed-------------------");

	        System.out.println("Quering User Indicies-------------------");
			rs = db.Query(con,"exec usp_DM_Index_user_count");
			rs.next();count = rs.getInt(1);rs.close();
			itemIDs = new int[count];
			rs = db.Query(con,"exec usp_DM_Index_user_select");
			i=0;
	        System.out.println("Initializing User Indicies-------------------");
			while (rs.next())
				itemIDs[i++] = rs.getInt(1);

			mine.prepareUserIndex(itemIDs);
	        System.out.println("Completed-------------------");

	        System.out.println("Quering Group Indicies-------------------");
			rs = db.Query(con,"exec usp_DM_Index_group_count");
			rs.next();count = rs.getInt(1);rs.close();
			if (count>kValue) kValue=count;
			mine.groupCount = kValue;

			rs = db.Query(con,"exec usp_DM_Index_group_select");
			itemIDs = new int[count];
			i=0;
			while (rs.next())
				itemIDs[i++] = rs.getInt(1);
			System.out.println("Initializing Group Indicies-------------------");
			mine.prepareGroupIndex(itemIDs);

			System.out.println("Initializing Group ResultIDs overall----------");
			rs = db.Query(con,"exec usp_DM_Index_group_FARGID_1_select");
			while (rs.next())
			{
				i = rs.getInt(1);
				g = rs.getInt(2);
//				groups[groupIndex.getIndex(g)].value_fargID = i;
				mine.groups[g].value_fargID = i;
			}
			System.out.println("Initializing Group ResultIDs histogram--------");
			rs = db.Query(con,"exec usp_DM_Index_group_FARGID_33_select");
			while (rs.next())
			{
				i = rs.getInt(1);
				g = rs.getInt(2);
				mine.groups[g].hist_fargID = i;
			}
			rs.close();
	        System.out.println("Completed-------------------");

		}
		catch(SQLException ex){ System.err.println("LoadIndicies SQLException: " + ex.getMessage());}
	}


	public boolean LoadGroupRatings(Connection con)
	{
		int[]	groupIDs;
		int 	count,iIndex,gIndex,uIndex,g;
		int		rr,id,i;
		short	ratings[],w;
		float	r;

		i=0;
		try
		{
	        System.out.println("Quering Group Reviews-------------------");
			ResultSet rs = db.Query(con,"exec usp_DM_group_Reviews_select");
	        System.out.println("Loading Group Reviews-------------------");
			while  ( rs.next() )
			{
				i++;
				gIndex = mine.getGroupIndex( rs.getInt("groupID") );
				iIndex = mine.getItemIndex( rs.getInt("itemID") );
				r = rs.getFloat("rating");
				w = rs.getShort("weight");

				mine.groups[gIndex].ratings[iIndex].rating = (long)(r*((long)1<<32));
				mine.groups[gIndex].ratings[iIndex].weight = w;

				if (i%100 == 0) System.out.print(".");
			}
	        System.out.println("Completed-------------------");

	        System.out.println("Quering Group Ratings-------------------");
			rs = db.Query(con,"exec usp_DM_group_Ratings_select");
	        System.out.println("Loading Group Ratings-------------------");

		int k = 0;
			i=-1;
			gIndex=0;iIndex=0;
			ratings = new short[7];
			while  ( rs.next() )
			{
				k++;
				if (k % 100 == 0) System.out.print(".");
				gIndex = mine.getGroupIndex( rs.getInt("groupID") );
				id = rs.getInt("itemID");
				iIndex = mine.getItemIndex(id);
				rr = rs.getInt("rating");
				w = rs.getShort("weight");
				if (id != i)
				{
					if (i>=0) mine.groups[gIndex].ratings[iIndex].ratings = ratings;
					i = id;
					ratings = new short[7];
					for (count=0;count<7;count++) ratings[count]=0;
				}
				ratings[rr] = w;
			}
			if (i>=0) mine.groups[gIndex].ratings[iIndex].ratings = ratings;
	        System.out.println("Completed-------------------");

	        ReadGroupUserMembership(con);

			for (i=0; i<mine.getGroupCount() ; i++)
			{
				mine.groups[i].dx = GroupInfo.DIRTY_DX+1;
				mine.groups[i].refresh();
			}

	        System.out.println("Completed-------------------");
			i=1;
		}
		catch(SQLException ex){ System.err.println("LoadGroupRatings SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}

		return (i>0);
	}


	public void ReadGroupUserMembership(Connection con)
	{
		int 	uIndex,gIndex;
		double 	h;
		try
		{
			System.out.println("Quering Group User Xref-------------------");
			ResultSet rs = db.Query(con,"exec usp_DM_user_GroupMembership_select");
			System.out.println("Loading Group User Xref-------------------");
			while  ( rs.next() )
			{
				uIndex = mine.getUserIndex( rs.getInt(1) );
				gIndex = mine.getGroupIndex( rs.getInt(2) );

				mine.groups[gIndex].userList[uIndex/8] ^= 1<<uIndex%8;
				mine.groups[gIndex].memberCount++;
				if (mine.users[uIndex] != null)
				{
					mine.users[uIndex].groupIndex = gIndex;
					mine.users[uIndex].happiness = rs.getInt(3)/100;
				}
			}
			rs.close();
		}
		catch(SQLException ex){ System.err.println("LoadGroupUserMembership SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}
	public boolean LoadGroupByUserMembership(Connection con)
	{
		int 	uIndex,gIndex,c=0;
		double 	h;

		try
		{
			System.out.println("Quering Group User Xref-------------------");
			ResultSet rs = db.Query(con,"exec usp_DM_user_GroupMembership_select");
			System.out.println("Loading Group User Xref:");
			while  ( rs.next() )
			{
				if (c++ % 100==0) System.out.print(".");
				uIndex = mine.getUserIndex( rs.getInt(1) );
				gIndex = mine.getGroupIndex( rs.getInt(2) );
				h = rs.getInt(3)/100;

				if (mine.users[uIndex] != null)
				{
					mine.groups[gIndex].addUser(uIndex,mine.users[uIndex].ratings,(float) h);
					mine.users[uIndex].setGroup(gIndex, (float) h);
				}
			}
			rs.close();
		}
		catch(SQLException ex){ System.err.println("LoadGroupUserMembership SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
		return (c>0);
	}


	public void LoadUserRatings(Connection con, int start, int length)

	{
		int		i,index,item;
		int		u,newu;
		int[]	tempItems;
		byte[]	tempRatings,r;
		int		count;
		long	startTime,totalTime,endTime;
		boolean moreDB;
		try
		{
			tempItems = new int[mine.getItemCount()];		//init for later use with User Ratings
			tempRatings = new byte[mine.getItemCount()];	//init for later use with User Ratings

	        System.out.println("Querying User Ratings ("+mine.getUserID((int) start)+" - "+mine.getUserID((int) (start+length-1))+")--------");
			ResultSet rs = db.Query(con,"exec usp_DM_user_Rating_Select_Range "+mine.getUserID((int) start)+", "+mine.getUserID((int) (start+length-1)));
	        System.out.println("Loading User Ratings-------------------");
			endTime = System.currentTimeMillis();
			totalTime = endTime;
	        count=0;
	        moreDB=rs.next();
			r = new byte[4];
			newu=-1;
			while (moreDB)
			{
				if (count == 0) newu = rs.getInt(1);
				i=0;
				u = newu;
		        if (count % 100 == 0) System.out.print(".");
				while (newu==u && moreDB)
				{
					item = mine.getItemIndex( rs.getInt(2) );
					r = rs.getBytes(3);
					if (r[0] > (byte) 0)
					{
						tempItems[i] = item;
						tempRatings[i++] = (byte) (r[0] & 7);
					}
					moreDB=rs.next();
					if (moreDB) newu = rs.getInt(1);
				}
				index = mine.getUserIndex( u );
				mine.users[index] = new UserInfo( index+start, (int) u, tempItems, tempRatings, i);
//System.out.print(index);
				count++;
			}
			System.out.println("---->loaded "+count);

			endTime = System.currentTimeMillis();
			System.out.println("Total Load Time ="+(endTime-totalTime));
			rs.close();
	        System.out.println("\nCompleted-------------------");

		}
		catch(SQLException ex){ System.err.println("OOPS SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}

	public void LoadIndividualRatings(Connection con, int index)
	{
		int	i,id;
		int	u;
		int[]	tempItems;
		byte[]	tempRatings,r;
		int		count;
		try
		{
			tempItems = new int[mine.getItemCount()];		//init for later use with User Ratings
			tempRatings = new byte[mine.getItemCount()];	//init for later use with User Ratings

	        id = mine.getUserID( (int) index );

//	        System.out.println("Loading User "+id+"'s Ratings-------------------");
			ResultSet rs = db.Query(con,"exec usp_DM_user_Rating_Select_Indiv "+id);

			i=0;u=0;
			r = new byte[4];
			while (rs.next())
			{
				u = rs.getInt(1);
				id = rs.getInt(2);
				if (!mine.validItemID(id))
				{
					mine.addItem(id);
					System.out.println("Adding new Item "+id+" ("+mine.getItemIndex(id)+"/"+mine.getItemIndexSize()+")");
					for (int g=0;g<mine.getGroupCount();g++)
						mine.groups[g].addItem(mine.getItemIndex(id));
				}
				tempItems[i] = mine.getItemIndex( id );
				r = rs.getBytes(3);
				tempRatings[i++] = (byte) (r[0] & 7);

			}
			if (i>0) mine.users[index].setRatings(tempItems, tempRatings, i);
			rs.close();
		}
		catch(SQLException ex){ System.err.println("LoadIndividualRatings SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}












	public void UpdateUserInfo(Connection con)
	{
		int	i;
		try
		{
			for (i=0;i<mine.getUserCount();i++)
				if (mine.users[i] != null)
					if (mine.users[i].modified)
						StoreUserMembership(con,i);
		}
		catch(Exception ex){ System.err.println("UpdateUserInfo Exception: " + ex.getMessage());ex.printStackTrace(System.out);}
	}
	public void StoreUserMembership(Connection con, int userIndex)
	{
		try
		{
			db.postQuery(con,"exec usp_DM_user_GroupMembership_update "+mine.users[userIndex].userID+", "+mine.users[userIndex].groupIndex+", "+(int)(mine.users[userIndex].happiness*100));
			mine.users[userIndex].modified = false;
		}
		catch(SQLException ex){ System.err.println("StoreUserMembership SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}

	public void StoreGroupStats(Connection con, int i)
	{
		float			groupHappiness;
		double			Member_Item_Rating_SD,Item_Rating_SD,Item_Rating_Mean,Predictive_Error;
		int				Item_Rating_N;

		try
		{
//			System.out.print("Storing Group Stats "+i);
//			System.out.println(" "+i+","+								mine.groups[i].memberCount+","+								mine.groups[i].Member_Item_Rating_SD+","+								mine.groups[i].Item_Rating_N+","+								mine.groups[i].Item_Rating_SD+","+								mine.groups[i].Item_Rating_Mean+","+								mine.groups[i].Predictive_Error+","+								mine.groups[i].groupHappiness);
			db.postQuery(con,"Exec usp_DM_Prediction_Group_Update " +mine.getGroupID(i)+","+	mine.groups[i].memberCount+","+mine.groups[i].Member_Item_Rating_SD+","+mine.groups[i].Item_Rating_N+","+mine.groups[i].Item_Rating_SD+","+mine.groups[i].Item_Rating_Mean+","+mine.groups[i].Predictive_Error+","+mine.groups[i].groupHappiness);
		}
		catch(SQLException ex){ System.err.println("StoreGroupStats SQLException: " + ex.getMessage());}
	}

	public void StoreOverallGroupStats(Connection con)
	{
		float			groupHappiness;
		double			Member_Item_Rating_SD,Item_Rating_SD,Item_Rating_Mean,Predictive_Error;
		int				Item_Rating_N,memberCount;

		memberCount=0;
		groupHappiness =0;
		Member_Item_Rating_SD =0;
		Item_Rating_SD = 0;
		Item_Rating_Mean = 0;
		Predictive_Error = 0;
		Item_Rating_N = 0;

		int groupCount = mine.getGroupCount();
		for (int i=0;i<groupCount;i++)
		{
			memberCount += mine.groups[i].memberCount;
			groupHappiness += mine.groups[i].groupHappiness;
			Member_Item_Rating_SD += mine.groups[i].Member_Item_Rating_SD;
			Item_Rating_SD += mine.groups[i].Item_Rating_SD;
			Item_Rating_Mean += mine.groups[i].Item_Rating_Mean;
			Predictive_Error += mine.groups[i].Predictive_Error;
			Item_Rating_N += mine.groups[i].Item_Rating_N;
		}
		groupHappiness /= (double) groupCount;
		Member_Item_Rating_SD /= (double) groupCount;
		Item_Rating_SD /= (double) groupCount;
		Item_Rating_Mean /= (double) groupCount;
		Predictive_Error /= (double) groupCount;
		Item_Rating_N /= groupCount;

		try
		{
			db.postQuery(con,"Exec usp_DM_Prediction_Group_Update -1,"+memberCount+","+Member_Item_Rating_SD+","+Item_Rating_N+","+Item_Rating_SD+","+Item_Rating_Mean+","+Predictive_Error+","+groupHappiness);
		}
		catch(SQLException ex){ System.err.println("StoreOverallGroupStats SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}
	}

	public void StoreGroupRatings(Connection con)
	{
		int	i,index;
		int	u,newu;
		int[]	tempItems;
		byte[]	tempRatings;
		int		count,votes;
		byte	b;
		float	avg;

		System.out.println("Storing Group Ratings-------------------");

		try {
		FileWriter foutValue = new FileWriter("tbl_Temp_Field_Analysis_Result_Group__Item_XrefValue.txt");
		FileWriter foutHistogram = new FileWriter("tbl_Temp_Field_Analysis_Result_Group__Item_XrefHistogram.txt");

		for (i=0;i<mine.getGroupCount();i++)
			if (mine.groups[i].modified)
			{
				StoreGroupStats(con,i);
				try
				{
					for (int j=0;j<mine.groups[i].ratingCount;j++)
						if (mine.groups[i].ratings[j].modified)
						{
							if  (mine.groups[i].ratings[j].weight >= MIN_RATINGES_PER_PREDICTON)
							{
								foutValue.write(mine.groups[i].value_fargID+"|"+mine.getItemID(j)+"|"+(((float)mine.groups[i].ratings[j].rating/((long)1<<32)))+"|"+mine.groups[i].ratings[j].weight+";");
								for (int k=1; k<7; k++)
								{
									foutHistogram.write(mine.groups[i].hist_fargID+"|"+mine.getItemID(j)+"|"+k+"|"+mine.groups[i].ratings[j].ratings[k]+";");
								}
							}
							else if  (mine.groups[i].ratings[j].weight == MIN_RATINGES_PER_PREDICTON-1)

							{
								foutValue.write(mine.groups[i].value_fargID+"|"+mine.getItemID(j)+"|0.0|0;");
								for (int k=1; k<7; k++)
								{
									foutHistogram.write(mine.groups[i].hist_fargID+"|"+mine.getItemID(j)+"|"+k+"|0;");
								}
							}
							mine.groups[i].ratings[j].modified = false;
						}

					mine.groups[i].modified=false;
				}
				catch(Exception ex){ System.err.println("StoreGroupRatings SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}

			}
			foutValue.close();
			foutHistogram.close();
		}
		catch(IOException ex){ System.err.println("StoreGroupRatings IOException: " + ex.getMessage());}
		try
		{
			db.postQuery(con,"Exec usp_DM_GroupPackage_Execute");
		}
		catch(SQLException ex){ System.err.println("StoreGroupRatings SQLException: " + ex.getMessage());ex.printStackTrace(System.out);}

		StoreOverallGroupStats(con);

        System.out.println("Completed-------------------");
	}

	public void StoreNewGroup(Connection con,int i)
	{
		try
		{
			ResultSet rs = db.Query(con,"select count(*) from tbl_Prediction_Group where id = "+mine.getGroupID(i));
			rs.next();
			int count = rs.getInt(1);
			if (count == 0)
			{
		        System.out.println("Storing Group "+i+" in db");
 				rs = db.Query(con,"Exec usp_DM_Prediction_Group_Insert "+mine.getGroupID(i));

				rs.next();
				mine.groups[i].value_fargID = rs.getInt(1);
				mine.groups[i].hist_fargID = rs.getInt(2);

			}
			rs.close();
		}
		catch(Exception ex){ System.err.println("StoreNewGroup SQLException: " + ex.getMessage());}
	}

	private void RemoveOldGroup(Connection con,int i)
	{
		try
		{
	        System.out.println("Removing Group "+i+" in db");
			db.postQuery(con,"Exec usp_DM_Prediction_Group_Delete "+mine.getGroupID(i));
		}
		catch(Exception ex){ System.err.println("RemoveOldGroup SQLException: " + ex.getMessage());}
	}


}