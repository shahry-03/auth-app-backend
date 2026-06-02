package com.auth_app_backend.helper;

import java.util.UUID;

public class UserHelper {
    public static UUID parseId(String id) {
        return UUID.fromString(id);
    }
}
