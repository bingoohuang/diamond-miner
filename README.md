# diamond-miner

a java config system base on taobao diamond, main changes included:

+ API is simplified to be more friendly.
+ All files charset changed from GBK to UTF-8.
+ Logging system changed from commons logging to slf4j.
+ All code were cleaned and refactored.
+ Description and valid fields were added for convenience.
+ Diamond server disk files are rooted as use home directory.
+ A cache was added to client based on diamond and guava cache.
+ Some other changes.


# Usage

+ Setup mysql database and tables
+ Startup diamond-server
+ Try to use diamond-client

## Setup mysql database and tables

+ create database diamond

```sql
    create database diamond;
```
+ create table

```sql
create table diamond_stones (
    id varchar(255) not null,
    data_id varchar(255) not null,
    group_id varchar(128) not null,
    content longtext not null,
    description longtext default null,
    valid tinyint(1) not null default 1,
    gmt_create datetime not null,
    gmt_modified datetime not null,
    primary key (id),
    unique key uk_diamond_datagroup (data_id,group_id)
) default charset=utf8;
```

```sql
create table diamond_stones (
    id varchar2(400) not null primary key,
    data_id varchar2(255) not null,
    group_id varchar2(128) not null,
    content long not null,
    description varchar2(1024),
    valid number(1,0) default 1,
    gmt_create date not null,
    gmt_modified date not null
);

create index idx_diamond_stones on diamond_stones(data_id, group_id);
```

+ create user

```sql
    create user 'diamond'@'%' identified by 'diamond';
    grant all privileges on diamond.* to diamond@'localhost' identified by 'diamond';
```

## Setup diamond-server

There are two ways to setup diamond-servers. One is download the diamond-server code and then run mvn jetty:run. The other is download the war package diamond-server-0.0.1.war and then run java -jar diamond-server-0.0.1.war.

+ source code way
    1. download the diamond-server code from github
    2. run the command: 
```
mvn jetty:run
```
or
```
mvn package && cd target && java -jar dimaond-server-0.0.1.war
```


+ war package way
    1. download [diamond-server-0.0.1.war](https://github.com/bingoohuang/diamond-miner/releases/download/v0.0.1/diamond-server-0.0.1.war)
    2. run the command:
```
java -jar diamond-server-0.0.1.war
```

+ Cluster example
    1. java -Dport=18001 -jar diamond-server-0.0.1.war
    2. java -Dport=18002 -jar diamond-server-0.0.1.war
    3. Add config: dataId=nameservers, group=admin, content=localhost:18001 localhost:18002

The default port of diamond-server is 17002, you can change it by java -Dport=8080 -jar diamond-server-0.0.1.war or mvn -Dport=8080 jetty:run。

The default mysql connection is:

```
db.driver=com.mysql.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/diamond?useUnicode=true&&characterEncoding=UTF-8&connectTimeout=1000&autoReconnect=true
db.user=diamond
db.password=diamond
db.initialSize=10
db.maxActive=10
db.maxIdle=5
db.maxWait=5
db.poolPreparedStatements=true
```

If you have the different mysql ip, user or passoword, you can have two way to change your connection info:

+ place a diamond-server.properties in your current directory(same directory with diamond-server-0.0.1.war)
+ update WEB-INF/classes/diamond-server.properties in diamond-server-0.0.1.war.

After you setup diamond-server successfully, you can open [http://localhost:17002/diamond-server](http://localhost:17002/diamond-server) to login in diamond-server console. The default username and password is admin/admin.

## Try to use diamond-client
+ Setup connection info for client
    * Create diamond-client.properties on the classpath root.
    * Add one line to diamond-client.properties

```
DiamondServer.address=localhost:17002

or cluster way

DiamondServer.address=localhost:18001 localhost:18002
```


+ Simple use examples

```java
String foo = DiamondMiner.getString("foo"); // foofoo
String bar = DiamondMiner.getStone("my_group", "bar"); // barbar

int defaultTimeout = 10;
int timeout = DiamondMiner.getInt("timeout", defaultTimeout);
```

+ Add a listener to repsonse config changing.

```java
DiamondManager diamondManager = new DiamondManager("foo");
diamondManager.addDiamondListener(new DiamondListener() {
    @Override
    public Executor getExecutor() {
        return null;
    }

    @Override
    public void accept(DiamondStone diamondStone) {
        System.out.println("DiamondListener:" + diamondStone.getContent());
    }
});

String diamond = diamondManager.getDiamond();
```

+ Use substitution (Supported by DiamondMiner, and N/A in DiamondManager)

```java
String foobar = DiamondMiner.getString("foobar");
// ${foo}-${bar} will substituted to foofoo-barbar

```

+ Use diamond cache
    1. Create a cache updater class, and it should implement Callable interface.

            package org.n3r.diamond.client;
            
            import org.n3r.diamond.client.cache.ParamsAppliable;
            import java.util.Arrays;
            import java.util.Date;
            import java.util.concurrent.Callable;
                
            public class DemoUpdater implements Callable<String>, ParamsAppliable {
                private String param;
                
                @Override
                public String call() {
                    return param + new Date();
                }
                
                @Override
                public void applyParams(String[] params) {
                    this.param = Arrays.toString(params);
                }
            }
    2. Add a config like:

            dataId=foo, group=DEFAULT_GROUP,
            content=@org.n3r.diamond.client.DemoUpdater("Hello world") @TimestampMark("2013-10-09 10:37:08.123")
    3. Get the cache:

            String cache = DiamondMiner.getCache("foo");
    4. Update the cache manually, any change of config will cause cache updated.

            content=@org.n3r.diamond.client.DemoUpdater("Hello world") @TimestampMark("2013-10-09 10:37:10.234")
            or
            content=@org.n3r.diamond.client.DemoUpdater("你好，中国") @TimestampMark("2013-10-09 10:37:10.234")


# Notes

+ garbled characters on TOMCAT console: add `-Dfile.encoding=UTF-8` to VM options
+ check charset in MYSQL : 

```mysql
show create database diamond;
show create table diamond_stones;

set names utf8;
ALTER DATABASE `diamond` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE `config_info` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

```

# Rantionale

+ The client and server local store tree:

```
    ~/.diamond-client              ~/.diamond-server
    |____config-data               |____config-dump
    |____DiamondServer.address     | |____admin
    |____snapshot                  | | |____users.diamond
    | |____DEFAULT_GROUP           | |____DEFAULT_GROUP
    | | |____foo.cache             | | |____bar.diamond
    | | |____foo.diamond           | | |____foo.diamond
    | | |____SOLR_URL.diamond      | | |____SOLR_URL.diamond
```

+ The client api will lookup config in privileged by order:
    * The local disaster recovery dir(~/.diamond-client/config-data)
    * Httpclient to diamond-server if local cache missed
    * Local snapshot

>So if the diamond-server died, the client can still work by snapshot. And even when the snapshot is not available, the local disaster recovery dir can be used temporarily.

+ The client has a standalone thread to update configs to diamond-server by comparing those md5 periodically per 15 seconds.
+ The server will dump all the config from database periodically per 10 minutes.
+ More links:
    * [diamond专题（一）- 简介和快速使用](http://jm-blog.aliapp.com/?p=1588)
    * [diamond专题（二）- 核心原理介绍](http://jm-blog.aliapp.com/?p=1592)
    * [diamond专题（三）- diamond架构](http://jm-blog.aliapp.com/?p=1606)
    * [diamond专题（四）- 容灾机制](http://jm-blog.aliapp.com/?p=1617)
    * [ZooKeeper和Diamond有什么不同](http://jm-blog.aliapp.com/?p=2561)

