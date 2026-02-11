package com.info.dao;

import com.info.exception.UserNotFoundException;
import com.info.modal.User;
import com.info.util.GetConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class UserDAO {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String PHONE_REGEX = "^[0-9]{10}$";

    //Creating User
    public static void createUser(User user) {
        if (!isValid(user.getEmail(), EMAIL_REGEX)) {
            System.out.println("Invalid Email Format!");
            return;
        }

        Connection conn = null;
        try {
            conn = GetConnection.getConnection();
            conn.setAutoCommit(false);

            String checkEmail = "SELECT id FROM users WHERE email = ?";
            PreparedStatement psCheck = conn.prepareStatement(checkEmail);
            psCheck.setString(1, user.getEmail());
            if (psCheck.executeQuery().next()) {
                System.out.println("User already exists with this email.");
                conn.rollback();
                return;
            }

            String userSql = "INSERT INTO users (name, email) VALUES (?, ?)";
            PreparedStatement psUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, user.getName());
            psUser.setString(2, user.getEmail());
            psUser.executeUpdate();

            ResultSet rs = psUser.getGeneratedKeys();
            int generatedId = 0;
            if (rs.next()) {
                generatedId = rs.getInt(1);
            }

            String phoneSql = "INSERT INTO user_phones (user_id, phone_number) VALUES (?, ?)";
            PreparedStatement psPhone = conn.prepareStatement(phoneSql);

            for (String phone : user.getPhoneList()) {
                if (isValid(phone, PHONE_REGEX)) {
                    psPhone.setInt(1, generatedId);
                    psPhone.setString(2, phone);
                    psPhone.addBatch();

                } else {
                    System.out.println("Skipping invalid phone: " + phone);
                }
            }
            psPhone.executeBatch();

            conn.commit();
            System.out.println("User and phone numbers created successfully!");

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
                System.out.println("Transaction rolled back due to error: " + e.getMessage());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private static boolean isValid(String data, String regex) {
        return data != null && Pattern.compile(regex).matcher(data).matches();
    }
//Fetch User By id
    public static User fetchUserById(int id) throws UserNotFoundException, SQLException {
        Connection conn = GetConnection.getConnection();

        String userSql = "SELECT * FROM users WHERE id = ?";
        PreparedStatement psUser = conn.prepareStatement(userSql);
        psUser.setInt(1, id);
        ResultSet rsUser = psUser.executeQuery();

        if (!rsUser.next()) {
            throw new UserNotFoundException("ALERT: User with ID " + id + " does not exist!");
        }

        String name = rsUser.getString("name");
        String email = rsUser.getString("email");

        String phoneSql = "SELECT phone_number FROM user_phones WHERE user_id = ?";
        PreparedStatement psPhone = conn.prepareStatement(phoneSql);
        psPhone.setInt(1, id);
        ResultSet rsPhone = psPhone.executeQuery();

        List<String> phones = new ArrayList<>();
        while (rsPhone.next()) {
            phones.add(rsPhone.getString("phone_number"));
        }

        return new User(id, name, email, phones);
    }

    //delete user by id
    public static void deleteUserById(int userId) throws UserNotFoundException, SQLException {
        User user = fetchUserById(userId);

        Connection conn = null;
        String sql = "DELETE FROM users WHERE id = ?";

        try {
            conn = GetConnection.getConnection();

            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    conn.commit();
                    System.out.println("SUCCESS: User '" + user.getName() + "' and all associated data deleted.");
                } else {
                    conn.rollback();
                    System.out.println("FAILURE: No rows affected. Transaction rolled back.");
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
                System.err.println("TRANSACTION ERROR: Deletion failed. Changes rolled back.");
            }
            throw e;
        } finally {
            if (conn != null) {
                // 5. Restore default state
                conn.setAutoCommit(true);
            }
        }
    }

    //delete phone by id
    public static void deletePhoneRecord(int phoneId) throws UserNotFoundException, SQLException {
        String checkSql = "SELECT id FROM user_phones WHERE id = ?";

        try (Connection conn = GetConnection.getConnection()) {
            PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setInt(1, phoneId);
            ResultSet rs = checkPstmt.executeQuery();

            if (!rs.next()) {
                throw new UserNotFoundException("PhoneRecordNotFound: Phone ID " + phoneId + " does not exist.");
            }

            String deleteSql = "DELETE FROM user_phones WHERE id = ?";
            PreparedStatement delPstmt = conn.prepareStatement(deleteSql);
            delPstmt.setInt(1, phoneId);
            delPstmt.executeUpdate();
            System.out.println("SUCCESS: Phone record " + phoneId + " "+rs.next()+" "  + " deleted.");
        }
    }

    //Update User Info
    public static void updateBasicInfo(int id, String name, String email) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = GetConnection.getConnection();

            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, email);
                pstmt.setInt(3, id);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    conn.commit();
                    System.out.println("SUCCESS: User basic info updated and committed.");
                } else {
                    conn.rollback();
                    System.out.println("NOTICE: No user found with ID " + id + ". Transaction aborted.");
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
                System.err.println("TRANSACTION FAILED: Rolling back changes due to error.");
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    //Update Phones
    public static void updatePhoneById(int phoneId, String newNumber) throws SQLException {
        String sql = "UPDATE user_phones SET phone_number = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = GetConnection.getConnection();
            // Start Transaction
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newNumber);
                pstmt.setInt(2, phoneId);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    conn.commit();
                    System.out.println("SUCCESS: Phone number updated and transaction committed.");
                } else {
                    // If ID doesn't exist, we don't need to commit anything
                    System.out.println("WARNING: No record found with Phone ID " + phoneId);
                    conn.rollback();
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
                System.err.println("TRANSACTION ERROR: Update failed, changes rolled back.");
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void showUserPhones(int userId) throws SQLException {
        String sql = "SELECT id, phone_number FROM user_phones WHERE user_id = ?";
        try (Connection conn = GetConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("--- Associated Phone Numbers ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | Number: " + rs.getString("phone_number"));
            }
            System.out.println("--------------------------------");
        }
    }
//Delete User
    public static void deleteUser(int userId) throws UserNotFoundException, SQLException {
        User user = fetchUserById(userId);

        System.out.println("\n--- Record to be Deleted ---");
        System.out.println("Name : " + user.getName());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Phones: " + user.getPhoneList());
        System.out.println("----------------------------");

        Connection conn = null;
        try {
            conn = GetConnection.getConnection();
            conn.setAutoCommit(false);

            String deletePhonesSql = "DELETE FROM user_phones WHERE user_id = ?";
            PreparedStatement psPhones = conn.prepareStatement(deletePhonesSql);
            psPhones.setInt(1, userId);
            psPhones.executeUpdate();

            String deleteUserSql = "DELETE FROM users WHERE id = ?";
            PreparedStatement psUser = conn.prepareStatement(deleteUserSql);
            psUser.setInt(1, userId);
            psUser.executeUpdate();

            conn.commit();
            System.out.println("SUCCESS: User and associated records deleted successfully.");

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    //fetch all
    public static List<User> fetchAllUsers() throws SQLException {
        List<User> userList = new ArrayList<>();
        String userSql = "SELECT * FROM users";
        String phoneSql = "SELECT phone_number FROM user_phones WHERE user_id = ?";

        try (Connection conn = GetConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rsUser = stmt.executeQuery(userSql)) {

            while (rsUser.next()) {
                int id = rsUser.getInt("id");
                String name = rsUser.getString("name");
                String email = rsUser.getString("email");

                List<String> phones = new ArrayList<>();
                try (PreparedStatement psPhone = conn.prepareStatement(phoneSql)) {
                    psPhone.setInt(1, id);
                    try (ResultSet rsPhone = psPhone.executeQuery()) {
                        while (rsPhone.next()) {
                            phones.add(rsPhone.getString("phone_number"));
                        }
                    }
                }
                userList.add(new User(id, name, email, phones));
            }
        }
        return userList;
    }
}