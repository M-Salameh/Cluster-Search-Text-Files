package org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch;

public interface OnElectionCallback {

    void onElectedToBeLeader(String IP);

    void onWorker(String IP);
}
