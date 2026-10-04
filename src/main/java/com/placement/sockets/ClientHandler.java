package com.placement.sockets;

import com.placement.models.*;
import com.placement.services.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService auth=new AuthService();
    private final RegistrationService registration=new RegistrationService();
    private final JobService jobs=new JobService();
    private final StudentService students=new StudentService();
    private final NotificationService notifications=new NotificationService();
    private final AdminService admin=new AdminService();
    private final ApplicationService applications=new ApplicationService();
    public ClientHandler(Socket socket){this.socket=socket;}
    @Override public void run(){
        try(Socket s=socket;BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream()));PrintWriter w=new PrintWriter(s.getOutputStream(),true)){
            String cmd=r.readLine();if(cmd==null)return;
            switch(cmd){
                case "LOGIN"->login(r,w);case "REGISTER_STUDENT"->register(r,w);case "GET_JOBS"->getJobs(r,w);case "ADD_JOB"->addJob(r,w);
                case "GET_STUDENTS"->getStudents(r,w);case "SEARCH_SORT_FILTER_STUDENTS"->queryStudents(r,w);case "GET_ELIGIBLE_STUDENTS"->eligible(r,w);case "ADD_STUDENT"->addStudent(r,w);
                case "SEND_NOTIFICATION"->sendNotification(r,w);case "SEND_GENERAL_NOTIFICATION"->sendGeneral(r,w);case "GET_NOTIFICATIONS"->getNotifications(r,w);
                case "GET_DASHBOARD_STATS"->stats(r,w);case "ADD_FACULTY"->addFaculty(r,w);case "GET_FACULTY"->getFaculty(r,w);
                case "APPLY_JOB"->apply(r,w);case "GET_MY_APPLICATIONS"->myApplications(r,w);case "LOGOUT"->logout(r,w);
                default->w.println("FAILED|Unknown command: "+cmd);
            }
        }catch(Exception e){System.out.println("Client handler error: "+e.getMessage());}
    }
    private String token(BufferedReader r,PrintWriter w)throws IOException{String t=r.readLine();if(!SessionManager.isValid(t)){w.println("FAILED|Not authenticated. Please log in again.");return null;}return t;}
    private boolean role(String t,PrintWriter w,String... allowed){String a=SessionManager.getRole(t);for(String x:allowed)if(x.equalsIgnoreCase(a))return true;w.println("FAILED|You do not have permission for this operation.");return false;}
    private boolean missing(String v,String n,PrintWriter w){if(v==null||v.isBlank()){w.println("FAILED|"+n+" is required.");return true;}return false;}
    private void login(BufferedReader r,PrintWriter w)throws IOException{String role=r.readLine(),email=r.readLine(),pass=r.readLine();if(missing(role,"Role",w)||missing(email,"Email",w)||missing(pass,"Password",w))return;LoginResult x=auth.login(role,email,pass);if(!x.isSuccess()){w.println("FAILED|"+x.getMessage());return;}String t=SessionManager.createSession(x.getAccountType(),x.getDisplayName(),x.getAccountId());w.println("SUCCESS|"+t+"|"+x.getDisplayName()+"|"+x.getAccountType());}
    private void register(BufferedReader r,PrintWriter w)throws IOException{Student s=new Student();s.setName(r.readLine());s.setEmail(r.readLine());String p=r.readLine();s.setDepartment(r.readLine());s.setCgpa(d(r.readLine()));s.setPassingYear(i(r.readLine()));s.setBacklogs(i(r.readLine()));s.setSemester(i(r.readLine()));s.setPhone(r.readLine());s.setSkills(r.readLine());if(missing(s.getName(),"Name",w)||missing(s.getEmail(),"Email",w)||missing(p,"Password",w))return;w.println(registration.registerStudent(s,p));}
    private void getJobs(BufferedReader r,PrintWriter w){try{String t=r.readLine();if(t!=null&&!SessionManager.isValid(t)){w.println("FAILED|Not authenticated.");return;}write(w,jobs.getAllJobs().stream().map(JobPosting::toProtocolLine).toList());}catch(Exception e){w.println("FAILED|Unable to retrieve jobs: "+e.getMessage());}}
    private void addJob(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC"))return;String c=r.readLine(),ro=r.readLine(),pack=r.readLine();double cg=d(r.readLine());String br=r.readLine();int max=i(r.readLine());String dl=r.readLine();if(missing(c,"Company",w)||missing(ro,"Role",w)||missing(br,"Branches",w))return;JobPosting j=jobs.addJob(c,ro,pack,cg,Arrays.stream(br.split(",")).map(String::trim).filter(x->!x.isBlank()).toList(),max,dl,SessionManager.getAccountId(t));w.println("SUCCESS|"+j.getId());}
    private void getStudents(BufferedReader r,PrintWriter w)throws IOException{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC","DIRECTOR","DEAN"))return;try{write(w,students.getAllStudents().stream().map(Student::toProtocolLine).toList());}catch(Exception e){w.println("FAILED|"+e.getMessage());}}
    private void addStudent(BufferedReader r,PrintWriter w)throws IOException{
        String t=token(r,w);if(t==null||!role(t,w,"ADMIN"))return;
        Student s=new Student();
        s.setPrn(r.readLine());s.setName(r.readLine());s.setEmail(r.readLine());String p=r.readLine();
        s.setDepartment(r.readLine());s.setCgpa(d(r.readLine()));s.setPassingYear(i(r.readLine()));s.setBacklogs(i(r.readLine()));
        s.setSemester(i(r.readLine()));s.setPhone(r.readLine());s.setSkills(r.readLine());
        if(missing(s.getName(),"Name",w)||missing(s.getEmail(),"Email",w)||missing(p,"Password",w))return;
        w.println(registration.registerStudent(s,p));
    }
    private void queryStudents(BufferedReader r,PrintWriter w)throws IOException{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPC"))return;try{String q=r.readLine(),dept=r.readLine();int year=i(r.readLine());double min=d(r.readLine());int max=i(r.readLine());int sem=i(r.readLine());String sort=r.readLine();boolean asc=Boolean.parseBoolean(r.readLine());if(year==0)year=-1;if(max==0)max=-1;if(sem==0)sem=-1;List<Student> out=students.query(q,dept,year,min,max,sem,sort,asc);write(w,out.stream().map(Student::toProtocolLine).toList());}catch(Exception e){w.println("FAILED|Student query failed: "+e.getMessage());}}
    private void eligible(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC","DIRECTOR","DEAN"))return;JobPosting j=jobs.getJob(r.readLine());if(j==null){w.println("FAILED|Job not found.");return;}write(w,students.getEligibleUnnotifiedStudentsSorted(j).stream().map(Student::toProtocolLine).toList());}
    private void sendNotification(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC"))return;JobPosting j=jobs.getJob(r.readLine());String csv=r.readLine();if(j==null){w.println("FAILED|Job not found.");return;}Notification n=notifications.sendJobEligibilityNotification(j,Arrays.asList(csv.split(",")));w.println("SUCCESS|"+n.getId()+"|"+n.getRecipientCount());}
    private void sendGeneral(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC"))return;String sub=r.readLine(),msg=r.readLine(),group=r.readLine();List<String> rec="ALL".equalsIgnoreCase(group)?students.getAllStudents().stream().map(Student::getPrn).toList():List.of();Notification n=notifications.sendGeneralNotification(sub,msg,rec);w.println("SUCCESS|"+n.getId()+"|"+n.getRecipientCount());}
    private void getNotifications(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC","DIRECTOR","DEAN","STUDENT"))return;write(w,notifications.getHistory().stream().map(Notification::toProtocolLine).toList());}
    private void stats(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","TPO","TPC","DIRECTOR","DEAN"))return;List<JobPosting> js=jobs.getAllJobs();List<Student> ss=students.getAllStudents();int pending=0;for(JobPosting j:js)pending+=students.getEligibleUnnotifiedStudents(j).size();w.println("SUCCESS|"+js.size()+"|"+pending+"|"+notifications.getHistory().size()+"|"+ss.size());}
    private void addFaculty(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN"))return;String n=r.readLine(),e=r.readLine(),p=r.readLine(),ro=r.readLine(),d=r.readLine(),ph=r.readLine();if(missing(n,"Name",w)||missing(e,"Email",w)||missing(p,"Password",w))return;w.println("SUCCESS|"+admin.addFaculty(n,e,p,ro,d,ph));}
    private void getFaculty(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"ADMIN","DIRECTOR","DEAN"))return;write(w,admin.getFaculty().stream().map(f->String.join("|",String.valueOf(f.getFacultyId()),safe(f.getName()),safe(f.getEmail()),safe(f.getRole()),safe(f.getDepartment()),safe(f.getPhone()))).toList());}
    private void apply(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"STUDENT"))return;String id=r.readLine();if(jobs.getJob(id)==null){w.println("FAILED|Job not found.");return;}w.println("SUCCESS|"+applications.apply(id,SessionManager.getAccountId(t)));}
    private void myApplications(BufferedReader r,PrintWriter w)throws Exception{String t=token(r,w);if(t==null||!role(t,w,"STUDENT"))return;write(w,applications.getForStudent(SessionManager.getAccountId(t)).stream().map(Application::toProtocolLine).toList());}
    private void logout(BufferedReader r,PrintWriter w) throws IOException {SessionManager.invalidate(r.readLine());w.println("SUCCESS|Logged out.");}
    private void write(PrintWriter w,List<String> lines){w.println("SUCCESS");w.println(lines.size());for(String x:lines)w.println(x);w.println("END");}
    private static String safe(String s){return s==null?"":s.replace("|","/");}private static int i(String s){try{return Integer.parseInt(s);}catch(Exception e){return 0;}}private static double d(String s){try{return Double.parseDouble(s);}catch(Exception e){return -1;}}
}
