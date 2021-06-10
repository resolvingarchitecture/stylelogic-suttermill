public class MiningData{


	int			userCount=0,groupCount=0,itemCount=0;
	idIndex		itemIndex,userIndex,groupIndex;
	UserInfo	users[];
	GroupInfo	groups[];


	final static double	userIndex_padding = 0.25;
	final static double	groupIndex_padding = 1.00;
	final static double	itemIndex_padding = 0.10;

	public MiningData()
	{
	}

	public void prepareUserIndex(int[] List)
	{
		userIndex = new idIndex(List, userIndex_padding);
		userCount = userIndex.length();
		users = new UserInfo[userIndex.size()];
		for (int index=0;index<userCount;index++)
			users[index] = new UserInfo(index,userIndex.getID(index));
	}

	public int addUser(int id)
	{
		int index = userIndex.addID(id);
		if (index >= users.length ) expandUsers();
		userCount = userIndex.length();
		users[index] = new UserInfo(index,id);
		return index;
	}

	private void expandUsers()
	{
		UserInfo	newUsers[] = new UserInfo[ (int) (users.length * (userIndex_padding+1.0)) ];

		for (int i=0; i<users.length; i++)
			newUsers[i] = users[i];
		users = newUsers;
	}



	public void prepareItemIndex(int[] List)
	{
		itemIndex = new idIndex(List, itemIndex_padding);
		itemCount = itemIndex.length();
	}

	public int addItem(int id)
	{
		int index = itemIndex.addID(id);
		itemCount = itemIndex.length();
		return index;
	}


	public void prepareGroupIndex(int[] List)
	{
		groupIndex = new idIndex(List, groupIndex_padding);
		groupCount = groupIndex.length();
		groups = new GroupInfo[groupIndex.size()];
		for (int index=0;index<groupCount;index++)
			groups[index] = new GroupInfo(index,itemCount,userCount,itemIndex_padding,userIndex_padding);
	}

	public int addGroup(int id)
	{
		int index = groupIndex.addID(id);
		groupCount = groupIndex.length();
		if (index >= groups.length ) expandGroups();
		groups[index] = new GroupInfo(index,itemCount,userCount,itemIndex_padding,userIndex_padding);
		return index;
	}

	private void expandGroups()
	{
		GroupInfo	newGroups[] = new GroupInfo[ (int) (groups.length * (groupIndex_padding+1.0)) ];

		for (int i=0; i<groups.length; i++)
			newGroups[i] = groups[i];
		groups = newGroups;
	}


	public int getUserIndex(int id)
	{
		return userIndex.getIndex(id);
	}
	public int getItemIndex(int id)
	{
		return itemIndex.getIndex(id);
	}
	public int getGroupIndex(int id)
	{
		return groupIndex.getIndex(id);
	}


	public boolean validUserID(int id)
	{
		return userIndex.validID(id);
	}
	public boolean validItemID(int id)
	{
		return itemIndex.validID(id);
	}
	public boolean validGroupID(int id)
	{
		return groupIndex.validID(id);
	}

	public int getUserID(int index)
	{
		return userIndex.getIndex(index);
	}
	public int getItemID(int index)
	{
		return itemIndex.getIndex(index);
	}
	public int getGroupID(int index)
	{
		return groupIndex.getIndex(index);
	}

	public int getUserCount() {		return userCount;	}
	public int getItemCount() {		return itemCount;	}
	public int getGroupCount() {	return groupCount;	}
	public int getUserIndexSize() {		return userIndex.size();	}
	public int getItemIndexSize() {		return itemIndex.size();	}
	public int getGroupIndexSize() {	return groupIndex.size();	}



}


