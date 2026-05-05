package core;

public enum Platform {
    ANDROID, IOS;

    public static Platform current() {
        String value = System.getProperty("platform", "android").toUpperCase();
        return Platform.valueOf(value);
    }
}
