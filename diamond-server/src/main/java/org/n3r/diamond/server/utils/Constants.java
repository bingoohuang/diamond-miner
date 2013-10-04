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

    /*
     * 批量操作时, 单条数据的状态码
     */
    // 发生异常
    int BATCH_OP_ERROR = -1;
    // 查询成功, 数据存在
    int BATCH_QUERY_EXISTS = 1;
    // 查询成功, 数据不存在
    int BATCH_QUERY_NONEXISTS = 2;
    // 新增成功
    int BATCH_ADD_SUCCESS = 3;
    // 更新成功
    int BATCH_UPDATE_SUCCESS = 4;

}
