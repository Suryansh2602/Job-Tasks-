package com.info.modal;

import java.util.List;

public class User {
    private int id;
    private String name;
    private String email;
    private List<String> phoneList;

    public List<String> getPhoneList() {
        return phoneList;
    }

    public void setPhoneList(List<String> phoneList) {
        this.phoneList = phoneList;
    }

    public User() {
        super();
    }

    public User(String name, String email, List<String> phone) {
        this.name = name;
        this.email = email;
        this.phoneList = phone;
    }



    public User(int id, String name, String email, List<String> phones) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phoneList = phones;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }




}