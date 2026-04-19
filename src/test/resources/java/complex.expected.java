package com.enterprise.app;

import java.util.List;
import java.util.ArrayList;

public class UserManager {
    private List<String> users;

    public UserManager() {
        this.users = new ArrayList<>();
    }

    public void addUser(String user) {
        if (user != null && !user.isEmpty()) {
            this.users.add(user);
        }
    }

    public boolean removeUser(String user) {
        return this.users.remove(user);
    }

    public int getUserCount() {
        return this.users.size();
    }
}
