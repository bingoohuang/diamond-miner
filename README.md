# diamond-miner

a java config system base on taobao diamond, main changes included:

+ All files charset changed from GBK to UTF-8.
+ Logging system changed from commons logging to slf4j.
+ All code were cleaned and refactored.
+ Description and valid fields were added for convenience.
+ An extension was added to local config file.
+ Diamond server disk files are rooted as use home directory.
+ A cache was added to client based on diamond and guava cache.
+ API is simplified to be more friendly.

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
    create table `diamond_stones` (
        `id` bigint(64) unsigned not null auto_increment,
        `data_id` varchar(255) not null default ' ',
        `group_id` varchar(128) not null default ' ',
        `content` longtext not null,
        `md5` varchar(32) not null default ' ',
        `description` varchar(256) default null,
        `valid` tinyint(1) not null default 1,
        `gmt_create` datetime not null default '2010-05-05 00:00:00',
        `gmt_modified` datetime not null default '2010-05-05 00:00:00',
        primary key (`id`),
        unique key `uk_diamond_datagroup` (`data_id`,`group_id`)
    ) default charset=utf8;
```

+ create user

```sql
    create user 'diamond'@'%' identified by 'diamond';
    grant all privileges on diamond.* to diamond@'%'  identified by 'diamond';
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
    * Create DiamondServer.address on the classpath root.
    * Add one line to DiamondServer.address
```
DiamondServer.address
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


# Rantionale

+ The client and server local store tree:

```
    ~/.diamond-client
    |____config-data
    |____DiamondServer.address
    |____snapshot
    | |____DEFAULT_GROUP
    | | |____foo.cache
    | | |____foo.diamond
    | | |____SOLR_URL.diamond
    
    ~/.diamond-server
    |____config-dump
    | |____admin
    | | |____users.diamond
    | |____DEFAULT_GROUP
    | | |____a1.diamond
    | | |____bar.diamond
    | | |____c1.diamond
    | | |____d1.diamond
    | | |____foo.diamond
    | | |____SOLR_URL.diamond

```

+ The client api will lookup config in privileged by order:
    * The local disaster recovery dir(~/.diamond-client/config-data)
    * Httpclient to diamond-server if local cache missed
    * Local snapshot

>So if the diamond-server died, the client can still work by snapshot. And the snapshot is not available, the local disaster recovery dir can be used to temporarily.

+ The client has a standalone thread to update configs to diamond-server by comparing those md5 periodically per 15 seconds.
+ The server will dump all the config from database periodically per 10 minutes.
+ More links:
    * [diamond专题（一）- 简介和快速使用](http://rdc.taobao.com/team/jm/archives/1588)
    * [diamond专题（二）- 核心原理介绍](http://rdc.taobao.com/team/jm/archives/1592)
    * [diamond专题（三）- diamond架构](http://rdc.taobao.com/team/jm/archives/1606)
    * [diamond专题（四）- 容灾机制](http://rdc.taobao.com/team/jm/archives/1617)
    * [ZooKeeper和Diamond有什么不同](http://rdc.taobao.com/team/jm/archives/2561)

