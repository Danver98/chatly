package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.services.interfaces.ConfigService;
import com.danver.messengerserver.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final Environment env;
    private final Map<String, Object> configInfo;

    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class.getName());

    @Autowired
    public ConfigServiceImpl(Environment env) {
        this.env = env;
        this.configInfo = new HashMap<>();
        this.setConfigInfo();
    }

    @Override
    public Map<String, Object> getConfigInfo(String version) {
        return this.configInfo;
    }

    private void setConfigInfo() {
        logger.info("Setting config info...");
        // Get context-path
        //TODO: init config from DB before Start
        this.configInfo.put("server.servlet.context-path", env.getProperty("server.servlet.context-path"));
        this.configInfo.put("websocket.message-service.stomp-endpoint", Constants.MESSAGE_BROKER_DESTINATION_PREFIX);
        this.configInfo.put("websocket.message-service.application-destination-prefix", Constants.MESSAGE_BROKER_APPLICATION_DESTINATION_PREFIX);
        this.configInfo.put("websocket.message-service.broker-prefix", new ArrayList<>(List.of(Constants.MESSAGE_BROKER_TOPIC_PREFIX)));
        this.configInfo.put("websocket.message-service.public-chat-queue-name", Constants.MESSAGE_BROKER_CHAT_QUEUE_NAME);
        this.configInfo.put("websocket.message-service.private-chat-queue-name", Constants.MESSAGE_BROKER_PRIVATE_CHAT_QUEUE_NAME);
    }
}
