package com.info;

import com.info.service.UserService;
import com.info.util.GetConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Scanner sc = new Scanner(System.in);
    public static void main( String[] args )
    {
     while(true){
         System.out.println("Enter Your Choice:");
         System.out.println("1 Create User");
         System.out.println("2 Fetch User By Id");
         System.out.println("3 Delete User By Id");
         System.out.println("4 Update User By Id");
         System.out.println("5 Fetch All Users");
         System.out.println("0 Exit");

         switch (sc.nextInt())
         {
             case 1:
                 UserService.addUser();
                 break;
             case 2:
                  UserService.fetchUserById();
                  break;
             case 3:
                 UserService.deleteUser();
                 break;
             case 4:
                 UserService.updateProcess();
                 break;
             case 5:
                 UserService.getAllUsers();
                 break;
             case 0:
                 System.exit(0);
         }
     }
    }
}
