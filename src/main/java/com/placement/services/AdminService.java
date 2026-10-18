package com.placement.services;
import com.placement.database.FacultyDAO;
import com.placement.models.Faculty;
import com.placement.security.PasswordHasher;
import java.util.List;
public class AdminService {
    private final FacultyDAO dao=new FacultyDAO();
    public List<Faculty> getFaculty()throws Exception{return dao.findAll();}
    public int addFaculty(String name,String email,String password,String role,String department,String phone)throws Exception{
        if(!(role.equals("ADMIN")||role.equals("TPO")||role.equals("TPC")||role.equals("DIRECTOR")||role.equals("DEAN")))throw new IllegalArgumentException("Invalid faculty role.");
        Faculty f=new Faculty();f.setName(name);f.setEmail(email);f.setPassword(PasswordHasher.hashPassword(password));f.setRole(role);f.setDepartment(department);f.setPhone(phone);return dao.insert(f);
    }
}
