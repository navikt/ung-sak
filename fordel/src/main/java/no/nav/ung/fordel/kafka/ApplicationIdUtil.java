package no.nav.ung.fordel.kafka;

public final class ApplicationIdUtil {

    private ApplicationIdUtil() {
    }

    public static String get() {
        return System.getProperty("nais.app.name", "ung-sak") + "-" + System.getProperty("nais.namespace", "default");
    }
}
