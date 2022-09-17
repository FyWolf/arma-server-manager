package cz.forgottenempire.arma3servergui.system.services;

public interface SystemService {

    long getDiskSpaceLeft();

    long getDiskSpaceTotal();

    long getMemoryLeft();

    long getMemoryTotal();

    double getCpuUsage();
}