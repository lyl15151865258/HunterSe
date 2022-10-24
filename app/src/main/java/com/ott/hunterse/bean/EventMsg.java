package com.ott.hunterse.bean;

/**
 * EventBus传递消息实体类
 * Created at 2022-06-13 0013 16:35:14
 *
 * @author LiYuliang
 * @version 1.0
 */

public class EventMsg {

    private int tag;
    private String msg;

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
