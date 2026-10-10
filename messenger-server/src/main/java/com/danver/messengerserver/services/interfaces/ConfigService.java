package com.danver.messengerserver.services.interfaces;

import java.util.Map;

public interface ConfigService {

    /**
     * Returns app configuration information
     */
    Map<String, Object> getConfigInfo(String version);
}
