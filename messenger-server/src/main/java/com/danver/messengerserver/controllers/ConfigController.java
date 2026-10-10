package com.danver.messengerserver.controllers;

import com.danver.messengerserver.services.interfaces.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    final ConfigService configService;

    @Autowired
    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getConfigInfo() {
        return new ResponseEntity<>(configService.getConfigInfo(null), HttpStatus.OK);
    }
}
