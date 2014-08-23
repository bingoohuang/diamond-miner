package org.n3r.diamond.server.service;

import org.apache.commons.io.IOUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.n3r.diamond.server.utils.Encrypt;
import org.n3r.diamond.server.utils.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class AdminService {
    public static final String ADMIN_GROUP = "_";
    public static final String USERS = "users";

    private Logger log = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private DiamondService diamondService;

    private Properties properties;

    public AdminService() {
    }

    @PostConstruct
    public void loadUsers() {
        Properties tempProperties = new Properties();
        DiamondStone info = diamondService.findConfigInfo(USERS, ADMIN_GROUP);

        try {
            if (info != null) {
                tempProperties.load(IOUtils.toInputStream(info.getContent()));
            } else {
                tempProperties.put("admin", "admin");
            }
        } catch (IOException e) {
            log.error("load users failed", e);
        }
        this.properties = Encrypt.tryDecrypt(tempProperties);
    }


    public synchronized boolean login(String userName, String password) {
        String passwordInFile = this.properties.getProperty(userName);
        if (passwordInFile != null) return passwordInFile.equals(password);

        return false;
    }


    public synchronized Map<String, String> getAllUsers() {
        Map<String, String> result = new HashMap<String, String>();
        Enumeration<?> enu = this.properties.keys();
        while (enu.hasMoreElements()) {
            String userName = (String) enu.nextElement();
            String password = this.properties.getProperty(userName);
            result.put(userName, Encrypt.encryptValueWithKey(password, userName));
        }

        return result;
    }


    public boolean addUser(String userName, String password) {
        properties.put(userName, password);
        saveProperties();
        return true;
    }

    public boolean removeUser(String userName) {
        properties.remove(userName);
        if (properties.isEmpty()) properties.put("admin", "admin");

        saveProperties();
        return true;
    }

    public boolean updatePassword(String userName, String password) {
        properties.put(userName, password);
        saveProperties();
        return true;
    }

    private void saveProperties() {
        DiamondStone info = diamondService.findConfigInfo(USERS, ADMIN_GROUP);
        String content = Props.getPropertyAsString(Encrypt.encrypt(properties));
        if (info != null) {
            diamondService.updateConfigInfo(USERS, ADMIN_GROUP, content, "admin", true);
        } else {
            diamondService.addConfigInfo(USERS, ADMIN_GROUP, content, "admin", true);
        }
    }
}
