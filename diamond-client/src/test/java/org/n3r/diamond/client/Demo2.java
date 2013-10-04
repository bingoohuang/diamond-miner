package org.n3r.diamond.client;

public class Demo2 {
    public static void main(String[] args) {
        // String solrUrl = DiamondMiner.getString("SOLR_URL");
        String solrUrl = DiamondMiner.getStone("DEFAULT_GROUP", "SOLR_URL");

        System.out.println(solrUrl);
    }
}
