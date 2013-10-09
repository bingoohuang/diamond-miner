package org.n3r.diamond.server.service;

import org.apache.commons.io.IOUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class AdminService {
    private Logger log = LoggerFactory.getLogger(AdminService.class);


    @Autowired
    private DiamondService diamondService;

    private Properties properties = new Properties();

    public AdminService() {
    }

    @PostConstruct
    public void loadUsers() {
        Properties tempProperties = new Properties();
        DiamondStone info = diamondService.findConfigInfo("users", "admin");

        try {
            if (info != null) {
                tempProperties.load(IOUtils.toInputStream(info.getContent()));
            } else {
                tempProperties.put("admin", "admin");
            }

        } catch (IOException e) {
            log.error("load users failed", e);
        }
        this.properties = tempProperties;
    }


    public synchronized boolean login(String userName, String password) {
        String passwordInFile = this.properties.getProperty(userName);
        if (passwordInFile != null)
            return passwordInFile.equals(password);

        return false;
    }


    public synchronized Map<String, String> getAllUsers() {
        Map<String, String> result = new HashMap<String, String>();
        Enumeration<?> enu = this.properties.keys();
        while (enu.hasMoreElements()) {
            String address = (String) enu.nextElement();
            String group = this.properties.getProperty(address);
            result.put(address, group);
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
        DiamondStone info = diamondService.findConfigInfo("users", "admin");
        String content = getPropertyAsString(properties);
        if (info != null) {
            diamondService.updateConfigInfo("users", "admin", content, "admin", true);
        } else {
            diamondService.addConfigInfo("users", "admin", content, "admin", true);
        }
    }

    public String getPropertyAsString(Properties prop) {
        StringWriter writer = new StringWriter();
        try {
            prop.store(writer, "");
        } catch (IOException e) {
            log.error("save user pass fail", e);
        }
        return writer.getBuffer().toString();
    }

}
