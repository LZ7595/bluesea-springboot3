package com.example.backend.Utils;

import com.example.backend.Model.Entity.User;

public class UserHolder {
    private static final ThreadLocal<User> userHolder = new ThreadLocal<>();

    private static void saveUser(User user) {
        userHolder.set(user);
    }
    public static User getUser() {
        return userHolder.get();
    }
    public static void removeUser() {
        userHolder.remove();
    }
}
