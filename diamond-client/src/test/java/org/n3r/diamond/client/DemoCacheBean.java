package org.n3r.diamond.client;

public class DemoCacheBean {
    private String content;


    public DemoCacheBean() {

    }
    public DemoCacheBean(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "DemoCacheBean{" +
                "content='" + content + '\'' +
                '}';
    }
}
