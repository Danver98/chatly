package com.danver.messengerserver.controllers;

import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.UserRequestDTO;
import com.danver.messengerserver.services.interfaces.ChatService;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.utils.FileStorageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final StorageService storageService;
    private final ChatService chatService;

    @Autowired
    public UserController(UserService userService, @Qualifier("s3Storage") StorageService storageService,
                          ChatService chatService) {
        this.userService = userService;
        this.storageService = storageService;
        this.chatService = chatService;
    }

    @GetMapping("/{id}")
    ResponseEntity<User> getUser(@PathVariable long id) {
        User user = this.userService.getUser(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/")
    ResponseEntity<List<User>> list(@RequestBody UserRequestDTO dto) {
        List<User> found = this.userService.list(dto);
        return new ResponseEntity<>(found, HttpStatus.OK);
        //return new ResponseEntity<>(found, !found.isEmpty() ? HttpStatus.OK : HttpStatus.NO_CONTENT);
    }

    @PostMapping
    ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = this.userService.createUser(user);
        if (createdUser == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        long [] users = {createdUser.getId()};
        // Add new user to common chat
        this.chatService.addParticipants(this.chatService.getAllUsersChat().getId(), users);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    @PatchMapping
    ResponseEntity<?> updateUser(@RequestBody User user) {
        this.userService.updateUser(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/avatar")
    ResponseEntity<String> updateAvatar(@RequestParam Long userId,
                                   @RequestParam MultipartFile file) {

        // Check authority + set destination
        try {
            // Path relative to root of the storage! Without leading slash
            String path = "users/" + userId + "/avatars";
            FileStorageOptions options = FileStorageOptions
                    .builder()
                    .owner(userId)
                    .path(path)
                    .build();
            String url = storageService.store(file, options);
            return new ResponseEntity<>(url, HttpStatus.OK);
        } catch (StorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
