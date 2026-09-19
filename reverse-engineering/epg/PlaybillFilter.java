package com.pukka.ydepg.common.http.v6bean.v6node;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class PlaybillFilter implements Serializable {

    @SerializedName("countries")
    private List<String> countries;

    @SerializedName("genres")
    private List<String> genres;

    @SerializedName("initials")
    private List<String> initials;

    @SerializedName("lifetimeIDs")
    private List<String> lifetimeIDs;

    public List<String> getInitials() {
        return this.initials;
    }

    public void setInitials(List<String> list) {
        this.initials = list;
    }

    public List<String> getLifetimeIDs() {
        return this.lifetimeIDs;
    }

    public void setLifetimeIDs(List<String> list) {
        this.lifetimeIDs = list;
    }

    public List<String> getCountries() {
        return this.countries;
    }

    public void setCountries(List<String> list) {
        this.countries = list;
    }

    public List<String> getGenres() {
        return this.genres;
    }

    public void setGenres(List<String> list) {
        this.genres = list;
    }
}
