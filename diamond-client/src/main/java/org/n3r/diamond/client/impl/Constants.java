package org.n3r.diamond.client.impl;

public interface Constants {
    String SERVER_ADDRESS = "DiamondServer.address";
    String NAME_SERVER_ADDRESS = "NameServer.address";

    String DATA_DIR = "config-data"; // local disaster recovery dir
    String SNAPSHOT_DIR = "snapshot"; // snapshot of last successfully gotten config

    int SC_OK = 200;
    int SC_NOT_MODIFIED = 304;
    int SC_NOT_FOUND = 404;
    int SC_SERVICE_UNAVAILABLE = 503;

    String DEFAULT_GROUP = "DEFAULT_GROUP";

    String DEF_DOMAINNAME = "a.b.c";

    int DEF_NAMESERVER_PORT = 17001;
    int DEFAULT_DIAMOND_SERVER_PORT = 17002;

    String NULL = "";

    String DATAID = "dataId";

    String GROUP = "group";

    String LAST_MODIFIED = "Last-Modified";

    String ACCEPT_ENCODING = "Accept-Encoding";

    String CONTENT_ENCODING = "Content-Encoding";

    String PROBE_MODIFY_REQUEST = "Probe-Modify-Request";

    String CONTENT_MD5 = "Content-MD5";

    String IF_MODIFIED_SINCE = "If-Modified-Since";

    String SPACING_INTERVAL = "client-spacing-interval";

    int POLLING_INTERVAL = 15; // seconds

    int ONCE_TIMEOUT = 2000; // milli seconds

    int CONN_TIMEOUT = 2000; // milli seconds

    int RECV_WAIT_TIMEOUT = ONCE_TIMEOUT * 5; // milli seconds

    String HTTP_URI_FILE = "/diamond-server/content";

    String DIAMOND_HTTP_URI = "/diamond-server/nameserver";

    String ENCODING = "UTF-8";

    String LINE_SEPARATOR = Character.toString((char) 1);
    String WORD_SEPARATOR = Character.toString((char) 2);

    String DIAMOND_STONE_EXT = ".diamond";
    String DIAMOND_CACHE_EXT = ".cache";
}
