class idIndex {

	private	int[] ID;
	private	int[] Index;
	private int start;
	private int indexCount;
	private double growthFactor=1.25;

	public idIndex()
	{
	}

	public idIndex(int[] List, double padding)
	{
		int	maxID=0;
		int	minID=32667;
		int		i=0;


		indexCount=0;
		growthFactor = 1 + padding;
		ID = new int[(int)(List.length*growthFactor)];

		for (i=0; i<List.length; i++)
		{
			ID[i] = List[i];
			if (List[i] > maxID) maxID = List[i];
			if (List[i] < minID) minID = List[i];
		}
		start = minID;
		Index = new int[(int)((maxID-start + 1)*growthFactor)];
		for (i=0; i<List.length; i++)
			Index[ List[i]-start ] = i;
		indexCount = List.length;

//		System.out.println(getIndex(List[1])+" = 1??");
//		System.out.println(getID((int)1)+" = "+List[1]+"??");

	}

	public boolean validID(int newID)
	{
		if (newID-start >= Index.length) return false;
		if (ID[Index[newID-start]] != newID) return false;
		return true;
	}


	public int addID(int newID)
	{
		while (newID-start >= Index.length)
			expandIndex();
		if (indexCount >= ID.length)
		{
			System.out.println(indexCount+" >= "+ID.length+" ("+newID+")");
			expandID();
		}

		Index[newID-start] = indexCount;
		ID[ indexCount ] = newID;

		return (indexCount++);
	}

	private void expandID()
	{
		int[]	newID;

		System.out.println("Expanding ID array from "+ID.length+" to "+(int)(ID.length*growthFactor));
		newID = new int[(int)(ID.length*growthFactor)];
		for (int i=0; i<ID.length; i++)
			newID[i] = ID[i];
		ID = newID;
	}
	private void expandIndex()
	{
		int[]	newIndex;

		System.out.println("Expanding Index array from"+Index.length+" to "+(int)(Index.length*growthFactor));
		newIndex = new int[(int)(Index.length*growthFactor)];
		for (int i=0; i<Index.length; i++)
			newIndex[i] = Index[i];
		Index = newIndex;
	}

	public int getIndex( int id )
	{
		int index=-1;

		try{
			if (id-start >= Index.length)
				addID(id);
			index = Index[id-start];
		}
		catch(Exception ex){ System.err.println("getIndex Exception (" +id+") :"+ ex.getMessage());ex.printStackTrace(System.out);}

		return (index);
	}

	public int getID( int index )
	{
		return (ID[index]);
	}

	public int size()
	{
		return (ID.length);
	}

	public int length()
	{
		return (indexCount);
	}
}
