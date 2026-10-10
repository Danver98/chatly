package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.UserRequestDTO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    User getUser(long id);

    User getUserByEmail(String email);

    /**
     * @param user user object with raw password
     * @return newly created user or null if error occurred
     */
    User createUser(User user);

    void updateUser(User user);

    void deleteUser(long id);

    /**
     * Search users by name and/or surname
     */
    List<User> list(UserRequestDTO dto);
}
