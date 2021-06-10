import java.io.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import javax.swing.*;
import java.util.Calendar;
import java.util.Date;

public class Interface extends JFrame implements ActionListener, WindowListener
{
	Image		icon;
	Canvas		c;
	JTextArea 	msgQueue;
	JPanel 		leftPane,rightPane,contentPane;
	JButton		button[];
	JScrollPane scrollPane;
	EngineControls	engine;
	Calendar		postedTime, currentTime;

	String[]	ButtonList = {	"Force Processing",
								"Save Log to Disk",
								"Group Error Stats",
								"Group Dispersion Stats",
								"Shut Down Processing"
								};
	String[]	ButtonActionList = {"process",
									"save",
									"error",
									"group",
									"halt"
								};
	String[]	ButtonTipList = {
								"Force Processing",
								"Store the current log to file",
								"Display error statistics for current group members",
								"Display distribution statistics for current set of groups",
								"Shut Down all processors"
								};


	public Interface(EngineControls _engine)
	{
		engine = _engine;
		icon = 	Toolkit.getDefaultToolkit().getImage("icon.gif");
		setIconImage(icon);
		setTitle("Sutters Mill - ReviewMovies");

		postedTime = Calendar.getInstance();
		currentTime = Calendar.getInstance();

		contentPane = new JPanel();
        BoxLayout box1 = new BoxLayout(contentPane, BoxLayout.X_AXIS);
        contentPane.setLayout(box1);
		contentPane.setSize(400,300);
		contentPane.setBackground(new Color(40,45,60));


		leftPane = new JPanel();
        BoxLayout box2 = new BoxLayout(leftPane, BoxLayout.Y_AXIS);
        leftPane.setLayout(box2);

        button = new JButton[ButtonList.length];
        for (int i=0; i<ButtonList.length; i++)
        {
	        button[i] = new JButton(ButtonList[i]);
		    button[i].setActionCommand(ButtonActionList[i]);
		    button[i].setToolTipText(ButtonTipList[i]);
		    button[i].addActionListener(this);
		    button[i].setEnabled(true);
	        leftPane.add(button[i]);
		}

		rightPane = new JPanel();
		msgQueue = new JTextArea(12,60);
		msgQueue.setBackground(new Color(180,190,220));
		msgQueue.setFont(new Font("Arail", Font.PLAIN, 12));
		msgQueue.setLineWrap(true);
		msgQueue.setWrapStyleWord(true);
		msgQueue.setBounds(40,10,350,275);

		scrollPane = new JScrollPane(msgQueue);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rightPane.add(scrollPane);


        contentPane.add(leftPane);
        contentPane.add(rightPane);
        setContentPane(contentPane);
		addWindowListener(this);

		pack();
		show();
		msgQueue.insert(">>>"+FormatDate(currentTime)+"<<<\n",0);
	}

	public void setActive( boolean _active )
	{
		System.out.println("Activating Buttons");
		for (int i=0; i<button.length; i++)
			button[i].setEnabled(_active);
	}

	public String FormatTime( Calendar c )
	{
		String s="";
		if (c.get(Calendar.HOUR_OF_DAY) < 10)  s = "0";
		s += c.get(Calendar.HOUR_OF_DAY) + ":";
		if (c.get(Calendar.MINUTE) < 10)  s += "0";
		s += c.get(Calendar.MINUTE) + ":";
		if (c.get(Calendar.SECOND) < 10)  s += "0";
		s += c.get(Calendar.SECOND);
		return s;
	}
	public String FormatDate( Calendar c )
	{
		String s="";
		if (c.get(Calendar.MONTH) < 10)  s = "0";
		s += c.get(Calendar.MONTH) + "-";
		if (c.get(Calendar.DAY_OF_MONTH) < 10)  s += "0";
		s += c.get(Calendar.DAY_OF_MONTH) + "-";
		s += c.get(Calendar.YEAR);
		return s;
	}


	public void addMessage( String msg )
	{
		String s;

		currentTime.setTime(new Date(System.currentTimeMillis()));
		if (postedTime.get(Calendar.DAY_OF_MONTH) != currentTime.get(Calendar.DAY_OF_MONTH))
		{
			msgQueue.insert(">>>"+FormatDate(currentTime)+"<<<\n",0);
			postedTime.setTime( currentTime.getTime() );
		}
		msgQueue.insert(FormatTime(currentTime) + "  -  "+msg+"\n",0);
	}

	public void update(Graphics g)
	{
		Dimension  d = getSize();
		g.clearRect(0,0,d.width,d.height);
	    paint(g);
	}

	public void paint(Graphics g)
	{
		g.drawImage(icon,10,25,this);
		leftPane.repaint();
		rightPane.repaint();
	}

	public void saveLogFile()
	{
		try
		{
			FileWriter fout = new FileWriter("Log"+FormatDate(currentTime).replace('-','_')+"__"+FormatTime(currentTime).replace(':','_')+".log");
			fout.write(msgQueue.getText());
			fout.close();
		}
		catch(IOException ex){ System.err.println("StoreGroupRatings IOException: " + ex.getMessage());}
		msgQueue.setText(">>>"+FormatDate(currentTime)+"<<<\n");
		msgQueue.insert(FormatTime(currentTime) + "  -  Log File Saved to Disk\n",0);
	}

	public void actionPerformed(ActionEvent e)
	{
	    String action = e.getActionCommand();
	    if (action.equals("process"))
	    {
			engine.startGroupingEngine();
		}
		else if (action.equals("save"))
		{
			saveLogFile();
		}
		else if (action.equals("error"))
		{
			engine.showGroupErrorStats();
		}
		else if (action.equals("group"))
		{
			engine.showGroupDistanceStats();
		}
		else if (action.equals("halt"))
		{
			engine.haltProcessing();
		}
    }


	public void windowClosing(WindowEvent e)
	{
		if (!engine.haltProcessing())
			System.exit(0);
		setVisible(false);
	}

	public void windowClosed(WindowEvent e)	{	}

	public void windowOpened(WindowEvent e) {	}

	public void windowIconified(WindowEvent e) {	}

	public void windowDeiconified(WindowEvent e) {	}

	public void windowActivated(WindowEvent e) {	}

	public void windowDeactivated(WindowEvent e) {	}



}