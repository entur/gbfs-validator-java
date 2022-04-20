package org.entur.gbfs.validation.versions;

public class VersionFactory {
    public static Version createVersion(String version) {
        switch (version) {
            case "1.0":
                return new Version1_0();
            case "1.1":
                return new Version1_1();
            case "2.0":
                return new Version2_0();
            case "2.1":
                return new Version2_1();
            case "2.2":
                return new Version2_2();
            case "2.3":
                return new Version2_3();
            default:
                throw new UnsupportedOperationException("Version not implemented");
        }
    }
}
