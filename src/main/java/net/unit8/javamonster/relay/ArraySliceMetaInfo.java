package net.unit8.javamonster.relay;

public class ArraySliceMetaInfo {
    private int sliceStart;
    private int arrayLength;

    private ArraySliceMetaInfo() {

    }

    public int getSliceStart() {
        return sliceStart;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    public static class Builder {
        private ArraySliceMetaInfo arraySliceMetaInfo;
        public Builder() {
            arraySliceMetaInfo = new ArraySliceMetaInfo();
        }

        public Builder sliceStart(int sliceStart) {
            arraySliceMetaInfo.sliceStart = sliceStart;
            return this;
        }

        public Builder arrayLength(int arrayLength) {
            arraySliceMetaInfo.arrayLength = arrayLength;
            return this;
        }

        public ArraySliceMetaInfo build() {
            return arraySliceMetaInfo;
        }
    }
}
