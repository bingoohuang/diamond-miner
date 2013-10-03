package org.n3r.diamond.client;


import static org.apache.commons.lang3.StringUtils.*;
import static org.n3r.diamond.client.impl.Constants.DEFAULT_GROUP;

public class DiamondStone {
    private DiamondAxis diamondAxis;
    private String content;


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DiamondAxis getDiamondAxis() {
        return diamondAxis;
    }

    public void setDiamondAxis(DiamondAxis diamondAxis) {
        this.diamondAxis = diamondAxis;
    }

    @Override
    public String toString() {
        return "DiamondStone{" +
                "diamondAxis=" + diamondAxis +
                ", content=" + content +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiamondStone diamondStone = (DiamondStone) o;

        if (diamondAxis != null ? !diamondAxis.equals(diamondStone.diamondAxis) : diamondStone.diamondAxis != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return diamondAxis != null ? diamondAxis.hashCode() : 0;
    }

    public static class DiamondAxis {
        public String dataId;
        public String group;

        public static DiamondAxis makeAxis(String group, String dataId) {
            return new DiamondAxis(group, dataId);
        }

        public static DiamondAxis makeAxis(String dataId) {
            return new DiamondAxis(dataId);
        }

        public DiamondAxis(String dataId) {
            this(null, dataId);
        }

        public DiamondAxis(String group, String dataId) {
            if (isBlank(dataId)) throw new IllegalArgumentException("blank dataId");

            this.group = defaultIfEmpty(group, DEFAULT_GROUP);
            this.dataId = dataId;
        }

        public String getDataId() {
            return dataId;
        }

        public String getGroup() {
            return group;
        }

        @Override
        public String toString() {
            return "DiamondAxis{" +
                    "dataId=" + dataId +
                    ", group=" + group +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DiamondAxis that = (DiamondAxis) o;

            if (!dataId.equals(that.dataId)) return false;
            if (!group.equals(that.group)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = dataId.hashCode();
            result = 31 * result + group.hashCode();
            return result;
        }
    }
}
