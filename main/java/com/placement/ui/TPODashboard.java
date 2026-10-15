package com.placement.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class TPODashboard extends JFrame {
    private static final Color PRIMARY_GREEN=new Color(27,117,61);
    private static final Color DARK_GREEN=new Color(20,92,48);
    private static final Color BG=new Color(247,248,245);
    private static final Color LIGHT=new Color(232,245,236);
    private static final Color TEXT=new Color(38,50,43);
    private static final Color MUTED=new Color(105,115,108);
    private static final Color BORDER=new Color(214,220,215);
    private JPanel content;
    private JLabel title;
    private JButton selected;
    private SolidMenuButton dashboardButton,postingsButton,eligibleApplicationsButton,notificationsButton,reportsButton;

    public TPODashboard(){
        setTitle("Placement Eligibility Portal - TPO Dashboard");
        setSize(1280,760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        JPanel main=new JPanel(new BorderLayout());
        main.add(sidebar(),BorderLayout.WEST);
        main.add(mainPanel(),BorderLayout.CENTER);
        add(main);
        dashboard();
    }

    private JPanel sidebar(){
        JPanel p=new JPanel();
        p.setPreferredSize(new Dimension(245,760));
        p.setBackground(DARK_GREEN);
        p.setBorder(new EmptyBorder(30,18,20,18));
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

        JLabel logo=new JLabel("PLACEMENT");
        logo.setFont(new Font("SansSerif",Font.BOLD,23));
        logo.setForeground(Color.WHITE);
        p.add(logo);

        JLabel portal=new JLabel("ELIGIBILITY PORTAL");
        portal.setFont(new Font("SansSerif",Font.BOLD,11));
        portal.setForeground(new Color(220,245,225));
        p.add(portal);
        p.add(Box.createVerticalStrut(35));

        dashboardButton=menu(p,"Dashboard",e->dashboard());
        postingsButton=menu(p,"Job & Internship Postings",e->postings());
        eligibleApplicationsButton=menu(p,"Eligible Students & Applications",e->eligibleApplications());
        notificationsButton=menu(p,"Deadlines & Notifications",e->notifications());
        reportsButton=menu(p,"Feedback & Reports",e->reports());

        p.add(Box.createVerticalGlue());
        JSeparator s=new JSeparator();
        s.setForeground(new Color(78,135,91));
        p.add(s);
        p.add(Box.createVerticalStrut(15));

        JLabel user=new JLabel("<html><b>TPO</b><br>Training & Placement Officer<br>TPO001</html>");
        user.setForeground(Color.WHITE);
        user.setFont(new Font("SansSerif",Font.PLAIN,12));
        p.add(user);
        p.add(Box.createVerticalStrut(15));

        SolidMenuButton logout=new SolidMenuButton("Logout");
        logout.setPreferredSize(new Dimension(209,38));
        logout.setMinimumSize(new Dimension(209,38));
        logout.setMaximumSize(new Dimension(209,38));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.setBorderColor(new Color(100,175,120));
        logout.addActionListener(e->{dispose();new LoginFrame().setVisible(true);});
        p.add(logout);
        return p;
    }

    private SolidMenuButton menu(JPanel p,String text,java.awt.event.ActionListener a){
        SolidMenuButton b=new SolidMenuButton(text);
        b.setPreferredSize(new Dimension(209,43));
        b.setMinimumSize(new Dimension(209,43));
        b.setMaximumSize(new Dimension(209,43));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e->{select(b);a.actionPerformed(e);});
        p.add(b);
        p.add(Box.createVerticalStrut(4));
        return b;
    }

    private void select(JButton b){
        if(selected instanceof SolidMenuButton) ((SolidMenuButton)selected).setSelectedVisual(false);
        selected=b;
        if(selected instanceof SolidMenuButton) ((SolidMenuButton)selected).setSelectedVisual(true);
    }

    private static class SolidMenuButton extends JButton{
        private boolean selectedVisual;
        private Color borderColor=DARK_GREEN;
        SolidMenuButton(String text){
            super(text);
            setFont(new Font("SansSerif",Font.BOLD,12));
            setForeground(Color.WHITE);
            setBackground(DARK_GREEN);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(false);
            setMargin(new Insets(0,14,0,8));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
        void setSelectedVisual(boolean value){selectedVisual=value;repaint();}
        void setBorderColor(Color color){borderColor=color;repaint();}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(DARK_GREEN);
            g2.fillRect(0,0,getWidth(),getHeight());
            if(selectedVisual){
                g2.setColor(DARK_GREEN);
                g2.fillRect(0,0,getWidth(),getHeight());
            }
            if(borderColor!=DARK_GREEN || selectedVisual){
                g2.setColor(selectedVisual?PRIMARY_GREEN:borderColor);
                g2.drawRect(0,0,getWidth()-1,getHeight()-1);
            }
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm=g2.getFontMetrics();
            int x=14;
            int y=(getHeight()-fm.getHeight())/2+fm.getAscent();
            g2.drawString(getText(),x,y);
            g2.dispose();
        }
    }

    private JPanel mainPanel(){
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(BG);
        JPanel h=new JPanel(new BorderLayout());
        h.setBackground(Color.WHITE);
        h.setBorder(new EmptyBorder(20,25,18,25));

        JPanel t=new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));
        title=new JLabel("TPO Dashboard");
        title.setFont(new Font("SansSerif",Font.BOLD,24));
        title.setForeground(TEXT);
        JLabel sub=new JLabel("Training & Placement Officer");
        sub.setForeground(MUTED);
        sub.setFont(new Font("SansSerif",Font.PLAIN,12));
        t.add(title);t.add(Box.createVerticalStrut(3));t.add(sub);

        h.add(t,BorderLayout.WEST);
        h.add(new JLabel("<html><b>TPO Admin</b><br><font color='#69736C'>TPO001</font></html>"),BorderLayout.EAST);
        p.add(h,BorderLayout.NORTH);

        content=new JPanel(new BorderLayout());
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(20,25,25,25));
        p.add(content,BorderLayout.CENTER);
        return p;
    }

    private void dashboard(){
        select(dashboardButton);
        title.setText("TPO Dashboard");
        JPanel p=new JPanel(new BorderLayout(0,20));p.setBackground(BG);
        JPanel stats=new JPanel(new GridLayout(1,4,15,0));stats.setOpaque(false);
        stats.add(card("Total Students","842"));stats.add(card("Eligible Students","526"));
        stats.add(card("Active Postings","18"));stats.add(card("Applications","1,284"));

        JPanel bottom=new JPanel(new GridLayout(1,2,20,0));bottom.setOpaque(false);
        bottom.add(section("Pending Job Approvals",new String[][]{
            {"Infosys","Software Engineer","Waiting"},{"Deloitte","Analyst","Waiting"},
            {"TCS","Graduate Engineer","Approved"},{"Accenture","Associate","Waiting"}},new String[]{"Organization","Position","Status"}));
        JPanel d=section("Upcoming Deadlines",new String[][]{
            {"Infosys","Registration","12 Sep 2026","2 days"},
            {"TCS","Application","15 Sep 2026","5 days"},
            {"Deloitte","Registration","18 Sep 2026","8 days"},
            {"Amazon","Application","20 Sep 2026","10 days"}},new String[]{"Organization","Type","Deadline","Remaining"});
        bottom.add(d);
        p.add(stats,BorderLayout.NORTH);p.add(bottom,BorderLayout.CENTER);show(p);
    }

    private JPanel card(String name,String value){
        JPanel p=new JPanel(new BorderLayout());p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(18,20,18,20)));
        JLabel n=new JLabel(name);n.setForeground(MUTED);n.setFont(new Font("SansSerif",Font.PLAIN,12));
        JLabel v=new JLabel(value);v.setForeground(PRIMARY_GREEN);v.setFont(new Font("SansSerif",Font.BOLD,27));
        p.add(n,BorderLayout.NORTH);p.add(v,BorderLayout.CENTER);return p;
    }

    private JPanel section(String name,String[][] data,String[] cols){
        JPanel p=new JPanel(new BorderLayout(0,10));p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(16,16,16,16)));
        JLabel h=new JLabel(name);h.setFont(new Font("SansSerif",Font.BOLD,16));h.setForeground(TEXT);
        p.add(h,BorderLayout.NORTH);p.add(new JScrollPane(table(data,cols)),BorderLayout.CENTER);return p;
    }

    private void postings(){
        select(postingsButton);
        title.setText("Job & Internship Postings");
        JPanel p=new JPanel(new BorderLayout(0,15));p.setBackground(BG);
        JButton add=new JButton("+ Add Posting");primary(add);
        add.addActionListener(e->JOptionPane.showMessageDialog(this,"Posting form will be connected to the company/TCP server later."));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);
        top.add(new JLabel("Common records shared by TPO, TPC, Director and Dean"),BorderLayout.WEST);top.add(add,BorderLayout.EAST);
        String[][] d={{"Placement","Infosys","Open","Computer, IT","6.5 LPA","86","42","GD + Interview","Approved"},
        {"Internship","TCS","Open","AI&DS, IT","25K/month","74","31","Project + Interview","Approved"},
        {"Placement","Deloitte","Open","Computer, IT, E&TC","7.2 LPA","112","58","GD + Interview","Waiting"},
        {"Placement","Accenture","Closed","Computer, IT","6.0 LPA","94","71","Interview","Approved"},
        {"Internship","Amazon","Open","Computer, AI&DS","35K/month","61","29","Project + Interview","Waiting"}};
        p.add(top,BorderLayout.NORTH);p.add(new JScrollPane(table(d,new String[]{"Event","Organization","Registration","Department","Package","Eligible","Applied","Process","Job Status"})),BorderLayout.CENTER);show(p);
    }

    private void eligibleApplications(){
        select(eligibleApplicationsButton);
        title.setText("Eligible Students & Applications");
        JPanel p=new JPanel(new BorderLayout(0,15));p.setBackground(BG);
        p.add(new JLabel("Eligible students and their placement/internship application status"),BorderLayout.NORTH);
        String[][] d={{"STU001","Aarav Sharma","Computer","8.72","Infosys","Eligible","Applied","GD Pending"},
        {"STU002","Priya Patil","IT","8.41","Deloitte","Eligible","Applied","Interview"},
        {"STU003","Rohan Kulkarni","AI&DS","9.02","TCS","Eligible","Not Applied","-"},
        {"STU004","Ananya Joshi","E&TC","8.15","Accenture","Eligible","Applied","Selected"},
        {"STU005","Rahul Deshmukh","Mechanical","7.82","TCS","Eligible","Not Applied","-"},
        {"STU006","Sneha More","Electrical","8.64","Infosys","Eligible","Applied","GD Pending"}};
        JTable t=table(d,new String[]{"Student ID","Name","Department","CGPA","Posting","Eligibility","Application Status","Current Stage"});
        p.add(new JScrollPane(t),BorderLayout.CENTER);
        JPanel b=new JPanel(new FlowLayout(FlowLayout.RIGHT));b.setOpaque(false);
        JButton v=new JButton("View Student"),u=new JButton("Update Status");
        secondary(v);primary(u);
        b.add(v);b.add(u);p.add(b,BorderLayout.SOUTH);show(p);
    }

    private void notifications(){
        select(notificationsButton);
        title.setText("Deadlines & Notifications");
        JPanel p=new JPanel(new BorderLayout(0,15));p.setBackground(BG);
        JPanel stats=new JPanel(new GridLayout(1,3,15,0));stats.setOpaque(false);
        stats.add(card("Upcoming Deadlines","07"));stats.add(card("Due This Week","03"));stats.add(card("Expired","01"));
        JPanel box=new JPanel(new BorderLayout(0,10));box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(12,12,12,12)));
        String[][] d={{"Infosys","Registration","12 Sep 2026","2 days","High"},{"TCS","Application","15 Sep 2026","5 days","Medium"},
        {"Deloitte","Registration","18 Sep 2026","8 days","Medium"},{"Amazon","Application","20 Sep 2026","10 days","Low"},
        {"Accenture","Document Submission","09 Sep 2026","Expired","High"}};
        JTable t=table(d,new String[]{"Organization","Deadline Type","Deadline","Remaining","Priority"});
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.setOpaque(false);
        JButton manage=new JButton("Manage Deadline"),send=new JButton("Send Notification");secondary(manage);primary(send);
        manage.addActionListener(e->JOptionPane.showMessageDialog(this,"Deadline form will be connected to the server later."));
        send.addActionListener(e->sendNotification());
        buttons.add(manage);buttons.add(send);
        box.add(new JLabel("Select a deadline and send a reminder to students or staff."),BorderLayout.NORTH);
        box.add(new JScrollPane(t),BorderLayout.CENTER);box.add(buttons,BorderLayout.SOUTH);
        p.add(stats,BorderLayout.NORTH);p.add(box,BorderLayout.CENTER);show(p);
    }

    private void sendNotification(){
        JTextField subject=new JTextField();
        JComboBox<String> recipient=new JComboBox<>(new String[]{"All Eligible Students","Students Applied","TPC","Director","Dean","Selected Department"});
        JComboBox<String> posting=new JComboBox<>(new String[]{"Infosys - Registration deadline","TCS - Application deadline","Deloitte - Registration deadline","Amazon - Application deadline","Accenture - Document submission"});
        JTextArea message=new JTextArea("Reminder: The deadline is approaching. Please complete the required action before the deadline.",5,32);
        message.setLineWrap(true);message.setWrapStyleWord(true);
        JPanel f=new JPanel(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(5,5,5,5);g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;
        field(f,g,0,"Recipient:",recipient);field(f,g,1,"Posting:",posting);field(f,g,2,"Subject:",subject);
        g.gridx=0;g.gridy=3;g.weightx=0;f.add(new JLabel("Message:"),g);g.gridx=1;g.weightx=1;g.fill=GridBagConstraints.BOTH;f.add(new JScrollPane(message),g);
        int r=JOptionPane.showConfirmDialog(this,f,"Send Deadline Notification",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(r==JOptionPane.OK_OPTION){
            if(subject.getText().trim().isEmpty()){JOptionPane.showMessageDialog(this,"Enter a notification subject.");return;}
            JOptionPane.showMessageDialog(this,"Notification sent successfully.\n\nRecipient: "+recipient.getSelectedItem()+"\nPosting: "+posting.getSelectedItem(),"Notification Sent",JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void field(JPanel p,GridBagConstraints g,int row,String label,JComponent c){
        g.gridy=row;g.gridx=0;g.weightx=0;p.add(new JLabel(label),g);g.gridx=1;g.weightx=1;p.add(c,g);
    }

    private void reports(){
        select(reportsButton);
        title.setText("Feedback & Reports");
        JPanel p=new JPanel(new BorderLayout(0,20));p.setBackground(BG);
        JPanel s=new JPanel(new GridLayout(1,4,15,0));s.setOpaque(false);
        s.add(card("Placement Rate","78%"));s.add(card("Total Offers","426"));s.add(card("Avg. Package","7.4 LPA"));s.add(card("Student Feedback","4.3/5"));
        String[][] d={{"Computer","312","264","84.6%","8.1 LPA"},{"IT","198","154","77.8%","7.6 LPA"},{"AI&DS","126","98","77.7%","8.4 LPA"},{"E&TC","102","61","59.8%","6.7 LPA"},{"Electrical","68","42","61.7%","6.2 LPA"},{"Mechanical","74","31","41.9%","5.8 LPA"}};
        p.add(s,BorderLayout.NORTH);p.add(new JScrollPane(table(d,new String[]{"Department","Eligible","Placed","Placement Rate","Average Package"})),BorderLayout.CENTER);show(p);
    }

    private JTable table(String[][] data,String[] cols){
        DefaultTableModel m=new DefaultTableModel(data,cols){public boolean isCellEditable(int r,int c){return false;}};
        JTable t=new JTable(m);t.setRowHeight(32);t.setFont(new Font("SansSerif",Font.PLAIN,12));t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));t.getTableHeader().setBackground(LIGHT);t.getTableHeader().setForeground(TEXT);t.setGridColor(BORDER);t.setSelectionBackground(LIGHT);t.setSelectionForeground(TEXT);return t;
    }

    private void primary(JButton b){
        b.setFont(new Font("SansSerif",Font.BOLD,12));b.setForeground(Color.WHITE);b.setBackground(PRIMARY_GREEN);b.setFocusPainted(false);b.setBorderPainted(false);b.setPreferredSize(new Dimension(145,38));
    }

    private void secondary(JButton b){
        b.setFont(new Font("SansSerif",Font.BOLD,12));b.setForeground(PRIMARY_GREEN);b.setBackground(Color.WHITE);b.setFocusPainted(false);b.setBorder(BorderFactory.createLineBorder(PRIMARY_GREEN));b.setPreferredSize(new Dimension(145,38));
    }

    private void show(JPanel p){content.removeAll();content.add(p);content.revalidate();content.repaint();}

    public static void main(String[] args){
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
        SwingUtilities.invokeLater(()->new TPODashboard().setVisible(true));
    }
}
