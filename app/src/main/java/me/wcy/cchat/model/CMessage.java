package me.wcy.cchat.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by hzwangchenyan on 2017/12/26.
 */
public class CMessage implements Serializable {
    @SerializedName("from")
    private String from;
    @SerializedName("to")
    private String to;
    @SerializedName("type")
    private int type;
    @SerializedName("content")
    private String content;

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
