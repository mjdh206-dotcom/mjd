package com.mjd.acremote.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local storage for profiles and settings. */
public final class Store {
    private static final String PREFS = "ac_remote_pro";
    private final SharedPreferences prefs;

    public Store(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Profile> profiles() {
        List<Profile> list = new ArrayList<>();
        String raw = prefs.getString("profiles", "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Profile p = new Profile();
                p.id = o.optString("id", UUID.randomUUID().toString());
                p.name = o.optString("name", "مكيف");
                p.protocolId = o.optString("protocol", "gree");
                p.variant = o.optInt("variant", 0);
                p.temp = o.optInt("temp", 24);
                p.mode = o.optInt("mode", 1);
                p.fan = o.optInt("fan", 0);
                p.power = o.optBoolean("power", false);
                p.swingV = o.optBoolean("swingV", false);
                p.swingH = o.optBoolean("swingH", false);
                p.turbo = o.optBoolean("turbo", false);
                p.sleep = o.optBoolean("sleep", false);
                p.light = o.optBoolean("light", true);
                p.econo = o.optBoolean("econo", false);
                list.add(p);
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    public void save(List<Profile> profiles) {
        JSONArray arr = new JSONArray();
        for (Profile p : profiles) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", p.id);
                o.put("name", p.name);
                o.put("protocol", p.protocolId);
                o.put("variant", p.variant);
                o.put("temp", p.temp);
                o.put("mode", p.mode);
                o.put("fan", p.fan);
                o.put("power", p.power);
                o.put("swingV", p.swingV);
                o.put("swingH", p.swingH);
                o.put("turbo", p.turbo);
                o.put("sleep", p.sleep);
                o.put("light", p.light);
                o.put("econo", p.econo);
                arr.put(o);
            } catch (Throwable ignored) {
            }
        }
        prefs.edit().putString("profiles", arr.toString()).apply();
    }

    public void upsert(Profile profile) {
        List<Profile> list = profiles();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(profile.id)) {
                list.set(i, profile);
                found = true;
                break;
            }
        }
        if (!found) list.add(profile);
        save(list);
        setActive(profile.id);
    }

    public void delete(String id) {
        List<Profile> list = profiles();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                break;
            }
        }
        save(list);
        if (id.equals(getActiveId())) {
            prefs.edit().remove("active").apply();
        }
    }

    public String getActiveId() {
        return prefs.getString("active", null);
    }

    public void setActive(String id) {
        prefs.edit().putString("active", id).apply();
    }

    public Profile active() {
        String id = getActiveId();
        for (Profile p : profiles()) if (p.id.equals(id)) return p;
        List<Profile> all = profiles();
        return all.isEmpty() ? null : all.get(0);
    }

    public boolean onboarded() {
        return prefs.getBoolean("onboarded", false);
    }

    public void setOnboarded() {
        prefs.edit().putBoolean("onboarded", true).apply();
    }

    public void setLastOffMinutes(int minutes) {
        prefs.edit().putInt("timerMinutes", minutes).apply();
    }

    public int lastOffMinutes() {
        return prefs.getInt("timerMinutes", 0);
    }

    public long timerAt() {
        return prefs.getLong("timerAt", 0);
    }

    public void setTimerAt(long at) {
        prefs.edit().putLong("timerAt", at).apply();
    }
}
