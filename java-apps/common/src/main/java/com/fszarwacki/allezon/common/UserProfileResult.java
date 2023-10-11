package com.fszarwacki.allezon.common;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class UserProfileResult {

    private String cookie;
    private List<UserTagEvent> views = new ArrayList<>();
    private List<UserTagEvent> buys = new ArrayList<>();

    public UserProfileResult() {
    }

    public UserProfileResult(String cookie, List<UserTagEvent> views, List<UserTagEvent> buys) {
        this.cookie = cookie;
        this.views = views;
        this.buys = buys;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public List<UserTagEvent> getViews() {
        return views;
    }

    public void setViews(List<UserTagEvent> views) {
        this.views = views;
    }

    public List<UserTagEvent> getBuys() {
        return buys;
    }

    public void setBuys(List<UserTagEvent> buys) {
        this.buys = buys;
    }

    @Override
    public String toString() {
        return "UserProfileResult [cookie=" + cookie + ", views=" + views + ", buys=" + buys + "]";
    }
}