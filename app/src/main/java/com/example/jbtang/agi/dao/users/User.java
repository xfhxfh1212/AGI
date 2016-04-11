package com.example.jbtang.agi.dao.users;

/**
 * user info class
 * Created by jbtang on 10/2/2015.
 */
public class User {
    public final String name;
    public final String password;
    public final String count;

    private User() {
        this.name = "";
        this.password = "";
        this.count = "";
    }

    public User(String name, String password, String count) {
        this.name = name;
        this.password = password;
        this.count = count;
    }
}
