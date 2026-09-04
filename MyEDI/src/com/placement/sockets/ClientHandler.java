package com.placement.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.placement.models.LoginResult;
import com.placement.models.Student;
import com.placement.services.AuthService;
import com.placement.services.RegistrationService;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        try (Socket socket = clientSocket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            String command = reader.readLine();

            if (command == null) return;

            if (command.equals("LOGIN")) {
                handleLogin(reader, writer);
            } else if (command.equals("REGISTER_STUDENT")) {
                handleStudentRegistration(reader, writer);
            } else {
                writer.println("FAILED|Unknown command.");
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Client sent invalid numeric data.");
        }
    }

    private void handleLogin(BufferedReader reader, PrintWriter writer) throws IOException {

        String role = reader.readLine();
        String email = reader.readLine();
        String password = reader.readLine();

        AuthService authService = new AuthService();
        LoginResult result = authService.login(role, email, password);

        if (result.isSuccess()) {
            writer.println("SUCCESS|" + result.getDisplayName());
        } else {
            writer.println("FAILED|" + result.getMessage());
        }

        System.out.println("Login attempt [" + role + "/" + email + "]: "
                + (result.isSuccess() ? "SUCCESS" : "FAILED - " + result.getMessage()));
    }

    private void handleStudentRegistration(BufferedReader reader, PrintWriter writer) throws IOException {

        Student student = new Student();
        student.setName(reader.readLine());
        student.setEmail(reader.readLine());
        String password = reader.readLine();
        student.setDepartment(reader.readLine());
        student.setCgpa(Double.parseDouble(reader.readLine()));
        student.setPassingYear(Integer.parseInt(reader.readLine()));
        student.setBacklogs(Integer.parseInt(reader.readLine()));
        student.setSemester(Integer.parseInt(reader.readLine()));
        student.setPhone(reader.readLine());
        student.setSkills(reader.readLine());

        RegistrationService registrationService = new RegistrationService();
        String result = registrationService.registerStudent(student, password);

        writer.println(result);
    }
}