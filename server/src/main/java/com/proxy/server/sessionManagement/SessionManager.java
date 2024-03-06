package com.proxy.server.sessionManagement;

import java.util.*;

public class SessionManager {
    //1 hour = 3600000 ms
    private static final int sessionTime = 3600000;
    private static SessionManager instance;
    private final Set<String> sessions;
    private final Timer timer;

    private SessionManager(){
        sessions = new HashSet<>();
        timer = new Timer();
    }

    public static SessionManager getInstance() {
        if (instance == null){
            instance = new SessionManager();
        }
        return instance;
    }

    //deletes Session after certain time period
    private void createSessionTimer(String key){
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        sessions.remove(key);
                        cancel();
                    }
                },
                sessionTime
        );
    }

    public void addSession(String id){
        sessions.add(id);
        createSessionTimer(id);
    }

    public boolean valid(String id){
        return sessions.contains(id);
    }




}
