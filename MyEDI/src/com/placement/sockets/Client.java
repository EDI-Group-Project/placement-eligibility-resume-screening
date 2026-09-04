package com.placement.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket("localhost", 5000);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader( 
                     new InputStreamReader(socket.getInputStream()))) {
        	writer.println("REGISTER_STUDENT");

            System.out.print("Name: ");
            writer.println(scanner.nextLine());

            System.out.print("Email: ");
            writer.println(scanner.nextLine());

            System.out.print("Password: ");
            writer.println(scanner.nextLine());

            System.out.print("Department: ");
            writer.println(scanner.nextLine());

            System.out.print("CGPA: ");
            writer.println(scanner.nextLine());

            System.out.print("Passing Year: ");
            writer.println(scanner.nextLine());

            System.out.print("Backlogs: ");
            writer.println(scanner.nextLine());

            System.out.print("Semester: ");
            writer.println(scanner.nextLine());

            System.out.print("Phone: ");
            writer.println(scanner.nextLine());

            System.out.print("Skills: ");
            writer.println(scanner.nextLine());

            String response = reader.readLine();
            System.out.println("Server response: " + response);

        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }

        scanner.close();
    }
}