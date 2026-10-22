package com.placement.ui;
import com.placement.sockets.SocketClient;
import javax.swing.*;import javax.swing.table.DefaultTableModel;import java.awt.*;import java.util.List;import java.io.File;import java.io.FileWriter;
public class FunctionalDashboard extends JFrame{
 private final String role,name,token;private final boolean edit,query,admin;private final SocketClient client=new SocketClient();private final CardLayout cards=new CardLayout();private final JPanel content=new JPanel(cards);
 private final DefaultTableModel jobs=model(new String[]{"Job ID","Company","Role","Package","Min CGPA","Branches","Backlogs","Year","Required Skills","Deadline"}),students=model(new String[]{"PRN","Name","Email","Department","CGPA","Year","Backlogs","Semester","Phone","Skills"}),notes=model(new String[]{"ID","Job ID","Subject","Message","Group","Recipients","Created"}),faculty=model(new String[]{"ID","Name","Email","Role","Department","Phone"});
 private final JTable jobsT=new JTable(jobs),studentsT=new JTable(students),notesT=new JTable(notes),facultyT=new JTable(faculty);private JTextField search;private JComboBox<String> dept,year,cgpa,back,sem,sort;private JLabel stats; private JPanel dashboardCards; private JPanel recentJobsList; private EligibilityPieChart eligibilityChart; private int eligibleStudentCount=-1; private JPanel reportsPanel; private JTable branchReport;
 public FunctionalDashboard(String role,String name,String token){String normalizedRole=role==null?"":role.trim().toUpperCase(java.util.Locale.ROOT);this.role=normalizedRole;this.name=name==null||name.isBlank()?normalizedRole:name;this.token=token;edit=this.role.equals("ADMIN")||this.role.equals("TPO")||this.role.equals("TPC");query=this.role.equals("ADMIN")||this.role.equals("TPC")||this.role.equals("TPO");admin=this.role.equals("ADMIN");setTitle("Placement Eligibility Portal - "+role+" Dashboard");setSize(1250,760);setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setLocationRelativeTo(null);build();refresh();}
 private void build(){JPanel root=new JPanel(new BorderLayout());root.add(side(),BorderLayout.WEST);JPanel main=new JPanel(new BorderLayout());main.add(header(),BorderLayout.NORTH);content.setBackground(BG());content.add(dashboard(),"D");content.add(jobsPage(),"J");content.add(studentsPage(),"S");content.add(notesPage(),"N");reportsPanel=reportPage();content.add(reportsPanel,"R");if(edit)content.add(new ResumeToolsPanel(role,token),"RT");if(admin)content.add(adminPage(),"A");main.add(content);root.add(main);setContentPane(root);}
 private JPanel side(){JPanel p=new JPanel();p.setPreferredSize(new Dimension(220,760));p.setBackground(DARK());p.setBorder(BorderFactory.createEmptyBorder(28,16,18,16));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.add(lbl("PLACEMENT",23,true,Color.WHITE));p.add(lbl("ELIGIBILITY PORTAL",11,false,new Color(220,245,225)));p.add(Box.createVerticalStrut(25));nav(p,"Dashboard","D");nav(p,"Job Postings","J");nav(p,"Students","S");nav(p,"Notifications","N");nav(p,"Reports","R");if(edit)nav(p,"Resume Tools","RT");if(admin)nav(p,"Manage Users","A");p.add(Box.createVerticalGlue());nav(p,"Logout",null);return p;}
 private void nav(JPanel p,String text,String card){JButton b=new JButton(text);b.setMaximumSize(new Dimension(188,40));b.setAlignmentX(Component.LEFT_ALIGNMENT);b.setHorizontalAlignment(SwingConstants.LEFT);b.setForeground(Color.WHITE);b.setBackground(DARK());b.setBorderPainted(false);b.setFocusPainted(false);if(card==null)b.addActionListener(e->logout());else b.addActionListener(e->{cards.show(content,card);if(card.equals("D")){loadStats();loadJobs();}if(card.equals("R"))refreshReportsView();if(card.equals("J"))loadJobs();if(card.equals("S"))loadStudents();if(card.equals("N"))loadNotes();if(card.equals("A"))loadFaculty();});p.add(b);p.add(Box.createVerticalStrut(4));}
 private JPanel header(){JPanel p=new JPanel(new BorderLayout());p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createEmptyBorder(18,24,18,24));p.add(lbl(role+" Dashboard",22,true,TEXT()),BorderLayout.WEST);p.add(lbl(name,13,false,MUTED()),BorderLayout.EAST);return p;}
 private JPanel dashboard(){JPanel p=page();p.setLayout(new BorderLayout(16,16));JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);JPanel title=new JPanel();title.setOpaque(false);title.setLayout(new BoxLayout(title,BoxLayout.Y_AXIS));title.add(lbl("Dashboard",25,true,TEXT()));title.add(Box.createVerticalStrut(4));title.add(lbl("Placement readiness and current opportunities",13,false,MUTED()));JButton refresh=btn("Refresh");refresh.addActionListener(e->{loadStats();loadJobs();loadStudents();loadNotes();});top.add(title,BorderLayout.WEST);top.add(refresh,BorderLayout.EAST);p.add(top,BorderLayout.NORTH);dashboardCards=new JPanel(new GridLayout(1,4,14,0));dashboardCards.setOpaque(false);if(role.equals("TPO")){dashboardCards.add(metricCard("Registered Students","—"));dashboardCards.add(metricCard("Active Jobs","—"));dashboardCards.add(metricCard("Eligible Students","—"));dashboardCards.add(metricCard("Notifications","—"));}else{dashboardCards.add(metricCard("Active Jobs","—"));dashboardCards.add(metricCard("Pending Eligible Matches","—"));dashboardCards.add(metricCard("Notifications","—"));dashboardCards.add(metricCard("Registered Students","—"));}JPanel center=new JPanel(new BorderLayout(0,14));center.setOpaque(false);center.add(dashboardCards,BorderLayout.NORTH);if(role.equals("TPO")){JPanel lower=new JPanel(new GridLayout(1,2,14,0));lower.setOpaque(false);JPanel chartCard=whiteCard();chartCard.setLayout(new BorderLayout(8,8));chartCard.add(lbl("Eligible & Not Eligible Students",16,true,TEXT()),BorderLayout.NORTH);eligibilityChart=new EligibilityPieChart();chartCard.add(eligibilityChart,BorderLayout.CENTER);lower.add(chartCard);JPanel recent=whiteCard();recent.setLayout(new BorderLayout(8,8));recent.add(lbl("Recent Placement Opportunities",16,true,TEXT()),BorderLayout.NORTH);recentJobsList=new JPanel();recentJobsList.setBackground(Color.WHITE);recentJobsList.setLayout(new BoxLayout(recentJobsList,BoxLayout.Y_AXIS));recent.add(new JScrollPane(recentJobsList),BorderLayout.CENTER);lower.add(recent);center.add(lower,BorderLayout.CENTER);}else{JPanel recent=whiteCard();recent.setLayout(new BorderLayout(8,8));recent.add(lbl("Recent Placement Opportunities",16,true,TEXT()),BorderLayout.NORTH);JTable recentJobs=new JTable(jobs);style(recentJobs);recent.add(new JScrollPane(recentJobs),BorderLayout.CENTER);center.add(recent,BorderLayout.CENTER);}p.add(center,BorderLayout.CENTER);stats=lbl("Loading live statistics…",12,false,MUTED());p.add(stats,BorderLayout.SOUTH);return p;}
 private JPanel metricCard(String title,String value){JPanel c=whiteCard();c.setLayout(new BorderLayout(6,6));JLabel v=lbl(value,26,true,TEXT());c.add(v,BorderLayout.CENTER);c.add(lbl(title,12,true,MUTED()),BorderLayout.SOUTH);return c;}
 private JPanel whiteCard(){JPanel c=new JPanel(new BorderLayout());c.setBackground(Color.WHITE);c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220,225,220)),BorderFactory.createEmptyBorder(14,14,14,14)));return c;}
 private JPanel jobsPage(){JPanel p=page();JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));top.setOpaque(false);JButton ref=btn("Refresh");ref.addActionListener(e->loadJobs());top.add(ref);if(edit){JButton add=btn("Add Job");add.addActionListener(e->addJob());JButton el=btn("Eligible Students");el.addActionListener(e->eligible());JButton no=btn("Send Notification");no.addActionListener(e->notifyAllStudents());JButton screen=btn("Screen Resume");screen.addActionListener(e->screenResume());JButton rank=btn("Rank Candidates");rank.addActionListener(e->rankCandidates());top.add(add);top.add(el);top.add(no);top.add(screen);top.add(rank);}p.add(top,BorderLayout.NORTH);style(jobsT);p.add(new JScrollPane(jobsT));return p;}
 private JPanel studentsPage(){JPanel p=page();if(query){JPanel f=new JPanel(new FlowLayout(FlowLayout.LEFT));f.setOpaque(false);search=new JTextField(12);dept=box("ALL","CSE","IT","ECE","MECH","CIVIL");year=box("ALL","2026","2027","2028","2029");cgpa=box("ALL","6","7","8","9");back=box("ALL","0","1","2","3");sem=box("ALL","1","2","3","4","5","6","7","8");sort=box("CGPA DESC","CGPA ASC","NAME ASC","NAME DESC","YEAR ASC","BACKLOGS ASC");f.add(new JLabel("Search"));f.add(search);f.add(new JLabel("Dept"));f.add(dept);f.add(new JLabel("Year"));f.add(year);f.add(new JLabel("Min CGPA"));f.add(cgpa);f.add(new JLabel("Max Backlogs"));f.add(back);f.add(new JLabel("Semester"));f.add(sem);f.add(new JLabel("Sort"));f.add(sort);JButton a=btn("Apply");a.addActionListener(e->loadStudents());JButton c=btn("Clear");c.addActionListener(e->{search.setText("");dept.setSelectedIndex(0);year.setSelectedIndex(0);cgpa.setSelectedIndex(0);back.setSelectedIndex(0);sem.setSelectedIndex(0);sort.setSelectedIndex(0);loadStudents();});f.add(a);f.add(c);if(admin){JButton addStudent=btn("Add Student");addStudent.addActionListener(e->addStudent());f.add(addStudent);}if(edit){JButton download=btn("Download Resume by Student ID");download.addActionListener(e->downloadStudentResume());f.add(download);JButton screenResume=btn("Screen Resume");screenResume.addActionListener(e->screenResume());f.add(screenResume);}p.add(f,BorderLayout.NORTH);}else p.add(lbl("Student Roster (view only)",18,true,TEXT()),BorderLayout.NORTH);style(studentsT);p.add(new JScrollPane(studentsT),BorderLayout.CENTER);return p;}
 private JPanel notesPage(){JPanel p=page();JButton r=btn("Refresh");r.addActionListener(e->loadNotes());p.add(r,BorderLayout.NORTH);style(notesT);p.add(new JScrollPane(notesT),BorderLayout.CENTER);return p;}
 private JPanel reportPage(){JPanel p=page();p.setLayout(new BorderLayout(14,14));JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);JPanel titles=new JPanel();titles.setOpaque(false);titles.setLayout(new BoxLayout(titles,BoxLayout.Y_AXIS));titles.add(lbl("Reports & Analytics Hub",24,true,TEXT()));titles.add(lbl("Live database-backed placement reports",12,false,MUTED()));JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.setOpaque(false);JButton refresh=btn("Refresh Data"),export=btn("Export Report (TXT)");refresh.addActionListener(e->refreshReportsView());export.addActionListener(e->exportReport());actions.add(refresh);actions.add(export);head.add(titles,BorderLayout.WEST);head.add(actions,BorderLayout.EAST);p.add(head,BorderLayout.NORTH);JTabbedPane tabs=new JTabbedPane();tabs.addTab("Student Eligibility",new JScrollPane(copyTable(studentsT)));tabs.addTab("Job Opportunities",new JScrollPane(copyTable(jobsT)));tabs.addTab("Notifications",new JScrollPane(copyTable(notesT)));branchReport=branchReportTable();tabs.addTab("Department Analysis",new JScrollPane(branchReport));if(admin){tabs.addTab("Users",new JScrollPane(copyTable(facultyT)));}style(studentsT);style(jobsT);style(notesT);style(facultyT);p.add(tabs,BorderLayout.CENTER);return p;}
 private JTable copyTable(JTable source){DefaultTableModel copy=model(java.util.stream.IntStream.range(0,source.getColumnCount()).mapToObj(source::getColumnName).toArray(String[]::new));for(int r=0;r<source.getRowCount();r++){Object[] row=new Object[source.getColumnCount()];for(int c=0;c<row.length;c++)row[c]=source.getValueAt(r,c);copy.addRow(row);}JTable table=new JTable(copy);style(table);return table;}
 private void refreshReportsView(){loadStats();loadJobs();loadStudents();loadNotes();if(admin)loadFaculty();if(reportsPanel!=null)content.remove(reportsPanel);reportsPanel=reportPage();content.add(reportsPanel,"R");content.revalidate();content.repaint();cards.show(content,"R");}
 private JTable branchReportTable(){DefaultTableModel model=model(new String[]{"Department","Student Count","Average CGPA"});java.util.Map<String,int[]> counts=new java.util.TreeMap<>();java.util.Map<String,Double> sums=new java.util.TreeMap<>();for(int r=0;r<students.getRowCount();r++){String dep=String.valueOf(students.getValueAt(r,3));int[] v=counts.computeIfAbsent(dep,k->new int[1]);v[0]++;try{sums.put(dep,sums.getOrDefault(dep,0.0)+Double.parseDouble(String.valueOf(students.getValueAt(r,4))));}catch(Exception ignored){}}for(String dep:counts.keySet()){int n=counts.get(dep)[0];model.addRow(new Object[]{dep,n,String.format(java.util.Locale.ROOT,"%.2f",sums.getOrDefault(dep,0.0)/n)});}JTable t=new JTable(model);style(t);return t;}
 private void exportReport(){JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("placement-report.txt"));if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;try(FileWriter out=new FileWriter(chooser.getSelectedFile())){out.write("PLACEMENT PORTAL REPORT - "+role+"\\nGenerated: "+java.time.LocalDateTime.now()+"\\n\\n");writeTable(out,"STUDENTS",studentsT);writeTable(out,"JOBS",jobsT);writeTable(out,"NOTIFICATIONS",notesT);if(admin)writeTable(out,"USERS",facultyT);JOptionPane.showMessageDialog(this,"Report exported successfully.");}catch(Exception ex){JOptionPane.showMessageDialog(this,"Unable to export report: "+ex.getMessage(),"Export error",JOptionPane.ERROR_MESSAGE);}}
 private void writeTable(FileWriter out,String title,JTable table)throws java.io.IOException{out.write("== "+title+" ==\\n");for(int c=0;c<table.getColumnCount();c++)out.write((c==0?"":" | ")+table.getColumnName(c));out.write("\\n");for(int r=0;r<table.getRowCount();r++){for(int c=0;c<table.getColumnCount();c++)out.write((c==0?"":" | ")+String.valueOf(table.getValueAt(r,c)));out.write("\\n");}out.write("\\n");}
 private JPanel adminPage(){JPanel p=page();JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));top.setOpaque(false);JButton add=btn("Add User");add.addActionListener(e->addFaculty());JButton ref=btn("Refresh");ref.addActionListener(e->loadFaculty());top.add(add);top.add(ref);p.add(top,BorderLayout.NORTH);style(facultyT);p.add(new JScrollPane(facultyT),BorderLayout.CENTER);return p;}
 private void refresh(){loadStats();loadJobs();loadStudents();loadNotes();if(admin)loadFaculty();}
 private void loadStats(){try{SocketClient.Response r=client.sendRequest("GET_DASHBOARD_STATS",token);if(r.success){String[] x=r.parts();stats.setText("Live data loaded from the placement database.");if(dashboardCards!=null&&x.length>=4){dashboardCards.removeAll();if(role.equals("TPO")){dashboardCards.add(metricCard("Registered Students",x[3]));dashboardCards.add(metricCard("Active Jobs",x[0]));dashboardCards.add(metricCard("Eligible Students",eligibleStudentCount<0?"…":String.valueOf(eligibleStudentCount)));dashboardCards.add(metricCard("Notifications",x[2]));}else{dashboardCards.add(metricCard("Active Jobs",x[0]));dashboardCards.add(metricCard("Pending Eligible Matches",x[1]));dashboardCards.add(metricCard("Notifications",x[2]));dashboardCards.add(metricCard("Registered Students",x[3]));}dashboardCards.revalidate();dashboardCards.repaint();}}}catch(Exception e){stats.setText("Unable to load live statistics.");}}
 private void loadJobs(){jobs.setRowCount(0);try{SocketClient.ListResponse r=client.sendListRequest("GET_JOBS",token);if(!r.success)throw new Exception(r.errorMessage);for(String l:r.lines){String[] x=l.split("\\|",-1);if(x.length>=10)jobs.addRow(x);}}catch(Exception ignored){}if(role.equals("TPO")){renderRecentJobs();loadEligibilityChartData();}}
 private void renderRecentJobs(){if(recentJobsList==null)return;recentJobsList.removeAll();int shown=0;for(int i=jobs.getRowCount()-1;i>=0&&shown<5;i--){final int row=i;JPanel item=new JPanel();item.setOpaque(false);item.setLayout(new BoxLayout(item,BoxLayout.Y_AXIS));String company=String.valueOf(jobs.getValueAt(i,1)),job=String.valueOf(jobs.getValueAt(i,2));item.add(lbl(company+"  —  "+job,13,true,TEXT()));item.add(lbl(String.valueOf(jobs.getValueAt(i,3))+" LPA   •   Min CGPA "+jobs.getValueAt(i,4)+"   •   Deadline "+jobs.getValueAt(i,8),11,false,MUTED()));item.setBorder(BorderFactory.createEmptyBorder(8,2,8,2));item.setAlignmentX(Component.LEFT_ALIGNMENT);recentJobsList.add(item);recentJobsList.add(new JSeparator());shown++;}if(shown==0)recentJobsList.add(lbl("No placement opportunities found.",12,false,MUTED()));recentJobsList.revalidate();recentJobsList.repaint();}
 private void loadEligibilityChartData(){if(eligibilityChart==null)return;java.util.Set<String> eligibleIds=new java.util.HashSet<>();try{for(int i=0;i<jobs.getRowCount();i++){String jobId=String.valueOf(jobs.getValueAt(i,0));SocketClient.ListResponse er=client.sendListRequest("GET_ELIGIBLE_STUDENTS",token,jobId);if(!er.success)continue;for(String line:er.lines){String[] cols=line.split("\\|",-1);if(cols.length>0&&!cols[0].isBlank())eligibleIds.add(cols[0]);}}eligibleStudentCount=eligibleIds.size();int total=students.getRowCount();int eligible=Math.min(total,eligibleStudentCount);eligibilityChart.setCounts(eligible,Math.max(0,total-eligible));if(dashboardCards!=null){loadStats();}}catch(Exception ignored){eligibilityChart.setCounts(-1,-1);}}
 private void loadStudents(){students.setRowCount(0);try{SocketClient.ListResponse r;if(query){String q=search==null?"":search.getText();String d=(String)dept.getSelectedItem();int y=num(year),m=num(back),s=num(sem);double c=decimal(cgpa,-1);String so=(String)sort.getSelectedItem();String[] z=so.split(" ");boolean asc=z.length>1&&z[1].equals("ASC");r=client.sendListRequest("SEARCH_SORT_FILTER_STUDENTS",token,q,d,String.valueOf(y),String.valueOf(c),String.valueOf(m),String.valueOf(s),z[0],String.valueOf(asc));}else r=client.sendListRequest("GET_STUDENTS",token);if(!r.success)throw new Exception(r.errorMessage);for(String l:r.lines){String[] x=l.split("\\|",-1);if(x.length>=10)students.addRow(x);}}catch(Exception e){JOptionPane.showMessageDialog(this,"Unable to load students: "+e.getMessage(),"Server error",JOptionPane.ERROR_MESSAGE);}if(role.equals("TPO"))loadEligibilityChartData();}
 private void loadNotes(){notes.setRowCount(0);try{SocketClient.ListResponse r=client.sendListRequest("GET_NOTIFICATIONS",token);if(!r.success)return;for(String l:r.lines){String[] x=l.split("\\|",-1);if(x.length>=7)notes.addRow(x);}}catch(Exception ignored){}}
 private void loadFaculty(){faculty.setRowCount(0);try{SocketClient.ListResponse r=client.sendListRequest("GET_FACULTY",token);if(!r.success)throw new Exception(r.errorMessage);for(String l:r.lines){String[] x=l.split("\\|",-1);if(x.length>=6)faculty.addRow(x);}}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage());}}
 
private void screenResume() {
    if (!edit) {
        JOptionPane.showMessageDialog(
            this, "You do not have permission to screen resumes.");
        return;
    }

    if (jobsT.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "No jobs are available.");
        return;
    }

    String job = (String) JOptionPane.showInputDialog(
        this,
        "Select the job for screening:",
        "Resume Screening",
        JOptionPane.QUESTION_MESSAGE,
        null,
        java.util.stream.IntStream.range(0, jobsT.getRowCount())
            .mapToObj(i -> String.valueOf(jobsT.getValueAt(i, 0)))
            .toArray(String[]::new),
        String.valueOf(jobsT.getValueAt(0, 0))
    );

    if (job == null) return;

    String sid = JOptionPane.showInputDialog(
        this, "Enter Student ID, PRN, or email:");

    if (sid == null || sid.isBlank()) return;

    try {
        SocketClient.ListResponse r =
            client.sendListRequest("SCREEN_STUDENT", token, job, sid.trim());

        if (!r.success) {
            throw new Exception(r.errorMessage);
        }

        showScreeningReport(job, sid.trim(), r.lines);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Unable to screen this resume:\n" + ex.getMessage(),
            "Screening Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}

private void showScreeningReport(
        String job, String student, List<String> lines) {

    java.util.Map<String, String> data = new java.util.LinkedHashMap<>();

    if (lines != null) {
        for (String line : lines) {
            if (line == null) continue;

            int separator = line.indexOf('|');
            if (separator > 0) {
                data.put(
                    line.substring(0, separator).trim().toUpperCase(),
                    line.substring(separator + 1).trim()
                );
            }
        }
    }

    String total = data.getOrDefault("TOTAL", "0");
    String ats = data.getOrDefault("ATS", "0");
    String improvement = data.getOrDefault(
        "IMPROVE", "No recommendations were returned.");

    JPanel report = new JPanel(new BorderLayout(12, 12));
    report.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    report.setBackground(Color.WHITE);

    JPanel heading = new JPanel();
    heading.setOpaque(false);
    heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
    heading.add(lbl("Resume Screening Report", 21, true, TEXT()));
    heading.add(Box.createVerticalStrut(6));
    heading.add(lbl("Job: " + job + "    |    Student: " + student,
                    12, false, MUTED()));
    report.add(heading, BorderLayout.NORTH);

    JPanel body = new JPanel();
    body.setOpaque(false);
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

    JPanel summary = new JPanel(new GridLayout(1, 2, 12, 0));
    summary.setOpaque(false);
    summary.add(screeningMetric("Overall Score", total));
    summary.add(screeningMetric("ATS Score", ats));
    body.add(summary);
    body.add(Box.createVerticalStrut(18));

    body.add(lbl("Score Breakdown", 16, true, TEXT()));
    body.add(Box.createVerticalStrut(10));

    addScoreBar(body, "Skills", data.get("SKILL"));
    addScoreBar(body, "Projects", data.get("PROJECTS"));
    addScoreBar(body, "Experience", data.get("EXPERIENCE"));
    addScoreBar(body, "Certifications", data.get("CERTIFICATIONS"));
    addScoreBar(body, "Keywords", data.get("KEYWORDS"));

    body.add(Box.createVerticalStrut(12));
    body.add(lbl("Matched Skills", 15, true, TEXT()));
    body.add(Box.createVerticalStrut(4));
    body.add(wrapText(data.getOrDefault("MATCHED", "None reported")));

    body.add(Box.createVerticalStrut(10));
    body.add(lbl("Missing Skills", 15, true, TEXT()));
    body.add(Box.createVerticalStrut(4));
    body.add(wrapText(data.getOrDefault("MISSING", "None reported")));

    body.add(Box.createVerticalStrut(10));
    body.add(lbl("Recommendations", 15, true, TEXT()));
    body.add(Box.createVerticalStrut(4));
    body.add(wrapText(improvement));

    boolean unreadable =
        improvement.toLowerCase(java.util.Locale.ROOT)
                   .contains("no resume text could be read");

    if (unreadable) {
        body.add(Box.createVerticalStrut(8));
        body.add(wrapText(
            "This report is incomplete because resume text extraction failed. "
            + "The zero scores do not indicate that the student is unqualified."
        ));
    }

    JScrollPane scroll = new JScrollPane(body);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setPreferredSize(new Dimension(590, 490));

    report.add(scroll, BorderLayout.CENTER);

    JOptionPane.showMessageDialog(
        this,
        report,
        "Resume Screening Result",
        JOptionPane.PLAIN_MESSAGE
    );
}

private JPanel screeningMetric(String title, String value) {
    JPanel panel = whiteCard();
    panel.setLayout(new BorderLayout(4, 8));
    panel.add(lbl(title, 12, true, MUTED()), BorderLayout.NORTH);
    panel.add(lbl(value, 27, true, TEXT()), BorderLayout.CENTER);
    return panel;
}

private void addScoreBar(JPanel parent, String title, String rawValue) {
    double score = 0;

    try {
        score = Double.parseDouble(rawValue == null ? "0" : rawValue);
    } catch (NumberFormatException ignored) {
        // Keep the displayed score at zero for invalid numeric data.
    }

    // Accept either fractional scores (0.0–1.0) or percentage scores.
    double percent = score >= 0 && score <= 1 ? score * 100 : score;
    percent = Math.max(0, Math.min(100, percent));

    JPanel row = new JPanel(new BorderLayout(8, 5));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(3, 0, 8, 0));

    JPanel labels = new JPanel(new BorderLayout());
    labels.setOpaque(false);
    labels.add(lbl(title, 12, false, TEXT()), BorderLayout.WEST);
    labels.add(lbl(String.format(java.util.Locale.ROOT, "%.0f%%", percent),
                    12, true, TEXT()), BorderLayout.EAST);

    JProgressBar bar = new JProgressBar(0, 100);
    bar.setValue((int) Math.round(percent));
    bar.setStringPainted(false);
    bar.setForeground(new Color(46, 139, 87));
    bar.setBackground(new Color(232, 237, 232));
    bar.setPreferredSize(new Dimension(100, 10));

    row.add(labels, BorderLayout.NORTH);
    row.add(bar, BorderLayout.CENTER);
    parent.add(row);
}

private JLabel wrapText(String text) {
    String safe = text == null || text.isBlank()
        ? "None reported" : text;

    JLabel label = new JLabel(
        "<html><div style='width:510px;'>" +
        escapeHtml(safe).replace("\n", "<br>") +
        "</div></html>"
    );
    label.setFont(new Font("SansSerif", Font.PLAIN, 12));
    label.setForeground(TEXT());
    return label;
}

private String escapeHtml(String value) {
    return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
}


private void rankCandidates() {
    if (!edit) return;

    if (jobsT.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "No jobs are available.");
        return;
    }

    String job = (String) JOptionPane.showInputDialog(
        this,
        "Select a job to rank candidates:",
        "Rank Candidates",
        JOptionPane.QUESTION_MESSAGE,
        null,
        java.util.stream.IntStream.range(0, jobsT.getRowCount())
            .mapToObj(i -> String.valueOf(jobsT.getValueAt(i, 0)))
            .toArray(String[]::new),
        String.valueOf(jobsT.getValueAt(0, 0))
    );

    if (job == null) return;

    try {
        SocketClient.ListResponse response =
            client.sendListRequest("RANK_CANDIDATES", token, job);

        if (!response.success) {
            throw new Exception(response.errorMessage);
        }

        showRankedCandidates(job, response.lines);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Unable to rank candidates:\n" + ex.getMessage(),
            "Ranking Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}

private void showRankedCandidates(String job, List<String> lines) {
    DefaultTableModel model = new DefaultTableModel(
        new String[]{"Rank", "Candidate", "Score", "Skills", "Projects",
                     "Experience", "Certifications", "ATS Score"},
        0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    if (lines != null) {
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;

            String[] fields = line.split("\\|", -1);

            // Expected backend format:
            // rank|name|total|skill|projects|experience|certifications|ATS
            if (fields.length >= 8) {
                model.addRow(new Object[]{
                    fields[0], fields[1], fields[2], fields[3],
                    fields[4], fields[5], fields[6], fields[7]
                });
            } else {
                // Preserve unexpected backend output instead of silently
                // pretending it is a valid candidate record.
                model.addRow(new Object[]{
                    "—", line, "—", "—", "—", "—", "—", "—"
                });
            }
        }
    }

    JPanel panel = new JPanel(new BorderLayout(12, 12));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    JLabel heading = lbl("Ranked Candidates", 21, true, TEXT());
    JLabel subtitle = lbl(
        "Job ID: " + job + "    •    Candidates ranked by screening results",
        12, false, MUTED()
    );

    JPanel header = new JPanel();
    header.setOpaque(false);
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.add(heading);
    header.add(Box.createVerticalStrut(6));
    header.add(subtitle);
    panel.add(header, BorderLayout.NORTH);

    if (model.getRowCount() == 0) {
        JPanel empty = new JPanel(new BorderLayout(8, 8));
        empty.setBackground(Color.WHITE);
        empty.add(lbl("No ranked candidates were returned.",
                      15, true, TEXT()), BorderLayout.CENTER);
        empty.add(lbl(
            "Check whether students have uploaded resumes and whether "
            + "screening results are available.",
            12, false, MUTED()
        ), BorderLayout.SOUTH);
        panel.add(empty, BorderLayout.CENTER);
    } else {
        JTable table = new JTable(model);
        style(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(
            javax.swing.ListSelectionModel.SINGLE_SELECTION
        );

        int[] widths = {55, 170, 85, 85, 90, 100, 115, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i)
                 .setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(850, 360));
        panel.add(scroll, BorderLayout.CENTER);
    }

    JLabel footer = lbl(
        "Total candidates: " + model.getRowCount(),
        12, true, MUTED()
    );
    panel.add(footer, BorderLayout.SOUTH);

    JOptionPane.showMessageDialog(
        this,
        panel,
        "Ranked Candidates",
        JOptionPane.PLAIN_MESSAGE
    );
}

 private void downloadStudentResume(){if(!edit&&!role.equals("DIRECTOR")&&!role.equals("DEAN"))return;String sid=JOptionPane.showInputDialog(this,"Enter Student ID, PRN, or email:");if(sid==null||sid.isBlank())return;try{SocketClient.Response r=client.sendRequest("DOWNLOAD_RESUME",token,sid.trim());if(!r.success)throw new Exception(r.payload);String[] parts=r.parts();String fileName=new String(java.util.Base64.getDecoder().decode(parts[0]),java.nio.charset.StandardCharsets.UTF_8);byte[] bytes=java.util.Base64.getDecoder().decode(parts[1]);JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File(fileName));if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)java.nio.file.Files.write(chooser.getSelectedFile().toPath(),bytes);}catch(Exception ex){JOptionPane.showMessageDialog(this,"Download failed: "+ex.getMessage());}}
 private void addStudent(){JTextField prn=new JTextField(),n=new JTextField(),em=new JTextField(),pw=new JPasswordField(),dep=new JTextField(),cg=new JTextField("7.0"),py=new JTextField(String.valueOf(java.time.Year.now().getValue()+1)),bl=new JTextField("0"),sm=new JTextField("8"),ph=new JTextField(),sk=new JTextField();JPanel form=new JPanel(new GridLayout(0,2,8,8));add(form,"PRN (optional)",prn);add(form,"Full name",n);add(form,"Email",em);add(form,"Initial password",pw);add(form,"Department",dep);add(form,"CGPA",cg);add(form,"Passing year",py);add(form,"Backlogs",bl);add(form,"Semester",sm);add(form,"Phone (10 digits)",ph);add(form,"Skills",sk);if(JOptionPane.showConfirmDialog(this,form,"Add Student",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{SocketClient.Response r=client.sendRequest("ADD_STUDENT",token,prn.getText(),n.getText(),em.getText(),new String(((JPasswordField)pw).getPassword()),dep.getText(),cg.getText(),py.getText(),bl.getText(),sm.getText(),ph.getText(),sk.getText());if(!r.success)throw new Exception(r.payload);JOptionPane.showMessageDialog(this,r.payload);loadStudents();loadStats();}catch(Exception ex){JOptionPane.showMessageDialog(this,"Unable to add student: "+ex.getMessage(),"Registration error",JOptionPane.ERROR_MESSAGE);}}
 private void addJob(){JTextField c=new JTextField(),ro=new JTextField(),pack=new JTextField(),cg=new JTextField("7"),br=new JTextField("CSE,IT"),ma=new JTextField("0"),skills=new JTextField("Java, SQL, Python"),dl=new JTextField(java.time.LocalDate.now().plusDays(30).toString());JPanel p=new JPanel(new GridLayout(0,2,8,6));add(p,"Company",c);add(p,"Role",ro);add(p,"Package",pack);add(p,"Min CGPA",cg);add(p,"Branches (comma-separated)",br);add(p,"Max Backlogs",ma);add(p,"Required Skills (comma-separated)",skills);add(p,"Application Deadline (YYYY-MM-DD)",dl);if(JOptionPane.showConfirmDialog(this,p,"Add Job",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{SocketClient.Response r=client.sendRequest("ADD_JOB",token,c.getText(),ro.getText(),pack.getText(),cg.getText(),br.getText(),ma.getText(),skills.getText(),dl.getText());if(!r.success)throw new Exception(r.payload);loadJobs();loadStats();}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage());}}
 private void eligible(){int viewRow=jobsT.getSelectedRow();if(viewRow<0){JOptionPane.showMessageDialog(this,"Select a job first.","Eligible Students",JOptionPane.INFORMATION_MESSAGE);return;}int row=jobsT.convertRowIndexToModel(viewRow);String jobId=String.valueOf(jobs.getValueAt(row,0));String company=String.valueOf(jobs.getValueAt(row,1));String jobRole=String.valueOf(jobs.getValueAt(row,2));try{SocketClient.ListResponse r=client.sendListRequest("GET_ELIGIBLE_STUDENTS",token,jobId);if(!r.success)throw new Exception(r.errorMessage);if(role.equals("TPO")){showTpoEligibleStudents(jobId,company,jobRole,r.lines);return;}JTextArea a=new JTextArea(String.join("\n",r.lines));a.setEditable(false);JOptionPane.showMessageDialog(this,new JScrollPane(a),"Eligible Students",JOptionPane.INFORMATION_MESSAGE);}catch(Exception e){JOptionPane.showMessageDialog(this,"Unable to load eligible students: "+e.getMessage(),"Eligible Students",JOptionPane.ERROR_MESSAGE);}}
 private void showTpoEligibleStudents(String jobId,String company,String jobRole,List<String> lines){java.util.List<String[]> records=new java.util.ArrayList<>();if(lines!=null){for(String line:lines){if(line==null||line.isBlank())continue;String[] fields=line.split("\\|",-1);if(fields.length<10)continue;boolean emptyRecord=true;for(String field:fields){if(!field.isBlank()){emptyRecord=false;break;}}if(!emptyRecord)records.add(java.util.Arrays.copyOf(fields,10));}}JPanel body=new JPanel(new BorderLayout(0,12));body.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));body.setBackground(Color.WHITE);JPanel heading=new JPanel();heading.setOpaque(false);heading.setLayout(new BoxLayout(heading,BoxLayout.Y_AXIS));heading.add(lbl(company+" — "+jobRole,17,true,TEXT()));heading.add(Box.createVerticalStrut(4));heading.add(lbl("Job ID: "+jobId+"    •    Eligible students: "+records.size(),12,false,MUTED()));body.add(heading,BorderLayout.NORTH);if(records.isEmpty()){JPanel empty=new JPanel(new BorderLayout(8,8));empty.setBackground(Color.WHITE);empty.setBorder(BorderFactory.createEmptyBorder(28,18,28,18));empty.add(lbl("No eligible students found for this job.",15,true,TEXT()),BorderLayout.CENTER);empty.add(lbl("Try another job posting or check its eligibility criteria.",12,false,MUTED()),BorderLayout.SOUTH);body.add(empty,BorderLayout.CENTER);}else{DefaultTableModel eligibleModel=model(new String[]{"PRN","Name","Email","Department","CGPA","Year","Backlogs","Semester","Phone","Skills"});for(String[] record:records)eligibleModel.addRow(record);JTable table=new JTable(eligibleModel);style(table);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setFillsViewportHeight(true);table.getTableHeader().setReorderingAllowed(false);int[] widths={95,145,190,90,55,65,70,75,115,220};for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);JScrollPane scroll=new JScrollPane(table);scroll.setPreferredSize(new Dimension(950,Math.min(360,Math.max(130,records.size()*29+28))));body.add(scroll,BorderLayout.CENTER);}JOptionPane.showMessageDialog(this,body,"Eligible Students",JOptionPane.INFORMATION_MESSAGE);}
 private void notifyAllStudents(){if(jobsT.getRowCount()==0){JOptionPane.showMessageDialog(this,"Post or refresh a job before sending an eligibility notification.");return;}int selected=jobsT.getSelectedRow();String[] jobChoices=java.util.stream.IntStream.range(0,jobsT.getRowCount()).mapToObj(i->jobs.getValueAt(i,0)+" — "+jobs.getValueAt(i,1)+" — "+jobs.getValueAt(i,2)).toArray(String[]::new);String choice=(String)JOptionPane.showInputDialog(this,"Select the job for the notification:","Send Job Notification",JOptionPane.QUESTION_MESSAGE,null,jobChoices,jobChoices[Math.max(0,selected<0?0:jobsT.convertRowIndexToModel(selected))]);if(choice==null)return;int row=java.util.Arrays.asList(jobChoices).indexOf(choice);if(row<0)return;String jobId=String.valueOf(jobs.getValueAt(row,0));try{SocketClient.ListResponse eligible=client.sendListRequest("GET_ELIGIBLE_STUDENTS",token,jobId);if(!eligible.success)throw new Exception(eligible.errorMessage);java.util.List<String> prns=new java.util.ArrayList<>();for(String line:eligible.lines){String[] fields=line.split("\\|",-1);if(fields.length>0&&!fields[0].isBlank())prns.add(fields[0].trim());}if(prns.isEmpty()){JOptionPane.showMessageDialog(this,"No eligible students are available for this job.");return;}int confirm=JOptionPane.showConfirmDialog(this,"Send a job-specific notification for "+jobId+" to "+prns.size()+" eligible student(s)?","Confirm Notification",JOptionPane.YES_NO_OPTION);if(confirm!=JOptionPane.YES_OPTION)return;SocketClient.Response r=client.sendRequest("SEND_NOTIFICATION",token,jobId,String.join(",",prns));if(!r.success)throw new Exception(r.payload);loadNotes();loadStats();JOptionPane.showMessageDialog(this,"Job notification sent. Job ID: "+jobId+"\nRecipients: "+prns.size());}catch(Exception e){JOptionPane.showMessageDialog(this,"Unable to send job notification: "+e.getMessage());}}
 private void addFaculty(){JTextField n=new JTextField(),e=new JTextField(),p=new JTextField("pass123"),d=new JTextField(),ph=new JTextField();JComboBox<String> ro=box("TPO","TPC","DIRECTOR","DEAN","ADMIN");JPanel x=new JPanel(new GridLayout(0,2));add(x,"Name",n);add(x,"Email",e);add(x,"Password",p);x.add(new JLabel("Role"));x.add(ro);add(x,"Department",d);add(x,"Phone",ph);if(JOptionPane.showConfirmDialog(this,x,"Add User",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{SocketClient.Response r=client.sendRequest("ADD_FACULTY",token,n.getText(),e.getText(),p.getText(),(String)ro.getSelectedItem(),d.getText(),ph.getText());if(!r.success)throw new Exception(r.payload);loadFaculty();}catch(Exception z){JOptionPane.showMessageDialog(this,z.getMessage());}}
 private void logout(){try{client.sendRequest("LOGOUT",token);}catch(Exception ignored){}dispose();new LoginFrame().setVisible(true);}
 private static final class EligibilityPieChart extends JPanel{private int eligible=-1,notEligible=-1;EligibilityPieChart(){setBackground(Color.WHITE);setPreferredSize(new Dimension(300,220));}void setCounts(int e,int n){eligible=e;notEligible=n;repaint();}protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);if(eligible<0||notEligible<0){g2.setColor(MUTED());g2.drawString("Eligibility data unavailable",20,40);g2.dispose();return;}int total=eligible+notEligible;if(total==0){g2.setColor(MUTED());g2.drawString("No student records available",20,40);g2.dispose();return;}int d=Math.min(150,Math.min(getWidth()-40,getHeight()-50));int x=20,y=12;int angle=(int)Math.round(360.0*eligible/total);g2.setColor(new Color(46,139,87));g2.fillArc(x,y,d,d,90,-angle);g2.setColor(new Color(220,90,90));g2.fillArc(x,y,d,d,90-angle,-(360-angle));int ly=y+d+12;g2.setColor(new Color(46,139,87));g2.fillRect(x,ly,10,10);g2.setColor(TEXT());g2.drawString("Eligible: "+eligible,x+16,ly+10);g2.setColor(new Color(220,90,90));g2.fillRect(x+125,ly,10,10);g2.setColor(TEXT());g2.drawString("Not eligible: "+notEligible,x+141,ly+10);g2.dispose();}}
 private static JPanel page(){JPanel p=new JPanel(new BorderLayout(0,10));p.setBackground(BG());p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));return p;}private static Color BG(){return new Color(247,248,245);}private static Color DARK(){return new Color(20,92,48);}private static Color TEXT(){return new Color(38,50,43);}private static Color MUTED(){return new Color(105,115,108);}private static JLabel lbl(String s,int z,boolean b,Color c){JLabel l=new JLabel(s);l.setFont(new Font("SansSerif",b?Font.BOLD:Font.PLAIN,z));l.setForeground(c);return l;}private static JButton btn(String s){JButton b=new JButton(s);b.setFocusPainted(false);return b;}private static DefaultTableModel model(String[] c){return new DefaultTableModel(c,0){public boolean isCellEditable(int r,int c){return false;}};}private static void style(JTable t){t.setRowHeight(28);t.setAutoCreateRowSorter(true);t.setFillsViewportHeight(true);}private static JComboBox<String> box(String...x){return new JComboBox<>(x);}private static int num(JComboBox<String>b){String x=(String)b.getSelectedItem();return x==null||x.equals("ALL")?0:Integer.parseInt(x);}private static double decimal(JComboBox<String>b,double n){String x=(String)b.getSelectedItem();return x==null||x.equals("ALL")?n:Double.parseDouble(x);}private static void add(JPanel p,String n,JComponent c){p.add(new JLabel(n));p.add(c);}
}
