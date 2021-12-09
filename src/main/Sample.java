package main;

import ipog.*;

import java.util.List;

public class Sample {
    public static void main(String[] args) {
        List<Parameter<?>> parameters = List.of(
                new Parameter<>("JRE", 7, 8, 11, 17),
                new Parameter<>("OS", OS.LINUX, OS.MAC, OS.WINDOWS),
                new Parameter<>("JDK Distribution", "Java SE Development Kit",
                        "OpenJDK", "Corretto", "GraalVM"),
                new Parameter<>("Build Tool", "Ant", "Maven", "Gradle"),
                new Parameter<>("IDE", "Intellij", "Eclipse",
                        "VSC", "NetBeans"),
                new Parameter<>("JUnit", "3.1.3", "4.3.1", "5.8.2"),
                new Parameter<>("Dev Mode", true, false));

        RunConfiguration runConfiguration =
                RunConfiguration.builder(parameters, 3, BaseAlgorithm.IPOG_F)
                        .enhanceHorizontal(true)
                        .adaptVertical()
                        .build();

        long startTime = System.currentTimeMillis();
        CoveringArray testSuite = new IpogRunner(runConfiguration).generate();
        long endTime = System.currentTimeMillis();

        System.out.println("runtime = " + (endTime - startTime) + " ms");
        System.out.println("testSuite.numberOfRows() = " + testSuite.numberOfRows());
        System.out.println("CoveringArrayUtils.isStrengthCovered(testSuite, strength): "
                + (CoveringArrayUtils.isStrengthCovered(testSuite, runConfiguration.getStrength())));
        System.out.println();
        System.out.println(testSuite.toCsv(true));
    }

    private enum OS {
        LINUX, MAC, WINDOWS
    }
}
