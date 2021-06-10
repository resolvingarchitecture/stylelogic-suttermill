public class UserInfo{
	float			happiness;
	int				groupIndex, userIndex, userID;
	itemRatings[]	ratings;
	boolean			modified;

	public UserInfo(int _userIndex, int _userID)
	{
		userIndex = _userIndex;
		userID = _userID;
		setGroup(-1,0);
		ratings = new itemRatings[0];
		modified = false;
	}
	public UserInfo(int _userIndex, int _userID, int[] _itemIndex, byte[] _ratings, int length)
	{
		userIndex = _userIndex;
		userID = _userID;
		setGroup(-1,0);
		setRatings(_itemIndex, _ratings, length);
		modified = false;
	}
	public void setGroup(int _groupIndex, float _happiness)
	{
		groupIndex = _groupIndex;
		happiness = _happiness;
		modified = true;
	}
	public void setRatings(int[] _itemIndex, byte[] _ratings, int length)
	{
		try{
			ratings = new itemRatings[length];
			for (int i=0; i<length; i++)
				ratings[i] = new itemRatings(_itemIndex[i],_ratings[i]);
		} catch (Exception e){System.out.println("Error in User::SetRatings :"+e.getMessage());e.printStackTrace(System.out);}
	}
}

class itemRatings
{
	byte	rating;
	int		itemIndex;

	public itemRatings(int _itemIndex, byte _rating)
	{
		rating = _rating;
		itemIndex = _itemIndex;
	}
}
