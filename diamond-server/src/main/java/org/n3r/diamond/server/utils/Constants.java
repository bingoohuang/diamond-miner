package org.n3r.diamond.server.utils;

public interface Constants {
    String DEFAULT_GROUP = "DEFAULT_GROUP";
    
    String BASE_DIR = "config-dump";


    String PROBE_MODIFY_REQUEST = "Probe-Modify-Request";

    String CONTENT_MD5 = "Content-MD5";

    String ENCODING = "UTF-8";

    String LINE_SEPARATOR = Character.toString((char) 1);

    String WORD_SEPARATOR = Character.toString((char) 2);

    int DEF_DIAMOND_NAMESERVER_PORT = 17001;
    String DIAMOND_STONE_EXT = ".diamond";

    int BATCH_OP_ERROR = -1;
    int BATCH_QUERY_EXISTS = 1;
    int BATCH_QUERY_NONEXISTS = 2;
    int BATCH_ADD_SUCCESS = 3;
    int BATCH_UPDATE_SUCCESS = 4;

}
