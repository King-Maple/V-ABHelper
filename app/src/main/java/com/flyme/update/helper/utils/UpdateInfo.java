package com.flyme.update.helper.utils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class UpdateInfo implements Parcelable {
    private String url;
    private long offset;
    private long size;
    private String[] headerKeyValuePairs;
    private String displayid;
    private String maskid;
    private int type;
    private String buildInfo;
    private String flymeid;

    public UpdateInfo() {
    }

    protected UpdateInfo(Parcel in) {
        url = in.readString();
        offset = in.readLong();
        size = in.readLong();
        headerKeyValuePairs = in.createStringArray();
        displayid = in.readString();
        maskid = in.readString();
        type = in.readInt();
        buildInfo = in.readString();
        flymeid=  in.readString();
    }

    public static final Creator<UpdateInfo> CREATOR = new Creator<UpdateInfo>() {
        @Override
        public UpdateInfo createFromParcel(Parcel in) {
            return new UpdateInfo(in);
        }

        @Override
        public UpdateInfo[] newArray(int size) {
            return new UpdateInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeLong(offset);
        dest.writeLong(size);
        dest.writeStringArray(headerKeyValuePairs);
        dest.writeString(displayid);
        dest.writeString(maskid);
        dest.writeInt(type);
        dest.writeString(buildInfo);
        dest.writeString(flymeid);
    }

    public int getType() {
        return type;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    public String getDisplayid() {
        return displayid;
    }

    public String getMaskid() {
        return maskid;
    }

    public String getUrl() {
        return url;
    }

    public String[] getHeaderKeyValuePairs() {
        return headerKeyValuePairs;
    }


    public void setDisplayid(String displayid) {
        this.displayid = displayid;
    }

    public void setHeaderKeyValuePairs(String[] headerKeyValuePairs) {
        this.headerKeyValuePairs = headerKeyValuePairs;
    }

    public void setMaskid(String maskid) {
        this.maskid = maskid;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBuildInfo() {
        return buildInfo;
    }

    public void setBuildInfo(String buildInfo) {
        this.buildInfo = buildInfo;
    }

    public void setFlymeid(String flymeid) {
        this.flymeid = flymeid;
    }

    public String getFlymeid() {
        return this.flymeid;
    }

    @NonNull
    @Override
    public String toString() {
        return "UpdateInfo {" +
                "url=" + url +
                ", offset=" + offset +
                ", size=" + size +
                ", headerKeyValuePairs=" + headerKeyValuePairs.length  +
                ", displayid=" + displayid +
                ", maskid=" + maskid +
                ", type=" + type +
                '}';
    }
}
