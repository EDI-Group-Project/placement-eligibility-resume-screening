package com.placement.services;
import com.placement.database.FacultyDAO;
import com.placement.models.Faculty;
import com.placement.security.PasswordHasher;
import java.util.List;
public class AdminService {
    private final FacultyDAO dao=new FacultyDAO();
    public List<Faculty> getFaculty()throws Exception{return dao.findAll();}
    public int addFaculty(String name,String email,String password,String role,String department,String phone)throws Exception{
        String normalizedRole=role==null?"":role.trim().toUpperCase();
        if(!(normalizedRole.equals("ADMIN")||normalizedRole.equals("TPO")||normalizedRole.equals("TPC")||normalizedRole.equals("DIRECTOR")||normalizedRole.equals("DEAN")))throw new IllegalArgumentException("Invalid faculty role.");
        if(name==null||name.isBlank())throw new IllegalArgumentException("Name is required.");
        if(email==null||email.isBlank())throw new IllegalArgumentException("Email is required.");
        if(password==null||password.isBlank())throw new IllegalArgumentException("Password is required.");
        if(dao.findByEmail(email.trim())!=null)throw new IllegalArgumentException("An account with this email already exists.");
        Faculty f=new Faculty();f.setName(name.trim());f.setEmail(email.trim());f.setPassword(PasswordHasher.hashPassword(password));f.setRole(normalizedRole);f.setDepartment(department==null||department.isBlank()?null:department.trim());f.setPhone(phone==null?"":phone.trim());return dao.insert(f);
    }
}
