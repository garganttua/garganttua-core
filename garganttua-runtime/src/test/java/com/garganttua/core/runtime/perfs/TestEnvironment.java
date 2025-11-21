package com.garganttua.core.runtime.perfs;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Properties;

public record TestEnvironment(
        String osName,
        String osVersion,
        int cpuCores,
        String arch,
        long totalMemory,
        long maxJvmMemory,
        long totalJvmMemory,
        String javaVersion,
        String javaVendor,
        String jvmName,
        String jvmVersion) {
    public static TestEnvironment capture() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        Properties props = System.getProperties();
        com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
                .getOperatingSystemMXBean();

        return new TestEnvironment(
                props.getProperty("os.name"),
                props.getProperty("os.version"),
                os.getAvailableProcessors(),
                os.getArch(),
                bean.getTotalMemorySize(),
                runtime.maxMemory(),
                runtime.totalMemory(),
                props.getProperty("java.version"),
                props.getProperty("java.vendor"),
                props.getProperty("java.vm.name"),
                props.getProperty("java.vm.version"));
    }

    @Override
    public final String toString() {

        return "OS Name : " + osName +
                "\nOS Version : " + osVersion +
                "\nCPU Cores : " + cpuCores +
                "\nTotal Memory : " + (totalMemory / 1024 / 1024 / 1024) + " Go" +
                "\nMax JVM Memory : " + (maxJvmMemory / 1024 / 1024 / 1024) + " Go" +
                "\nTotal JVM Memory : " + (totalJvmMemory / 1024 / 1024) + " Mo" +
                "\nJava Version : " + javaVersion +
                "\nJava Vendor : " + javaVendor +
                "\nJVM Name : " + jvmName +
                "\nJVM Version : " + jvmVersion;
    }
}