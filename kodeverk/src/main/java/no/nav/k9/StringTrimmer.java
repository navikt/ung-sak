package no.nav.k9;

/**
 * Klasse for trimming av string.
 *
 * Trimmer også non-breaking whitespaces (https://docs.oracle.com/javase/8/docs/api/java/lang/Character.html#isWhitespace-char-)
 */
public class StringTrimmer {

    public static String trim(String string) {
        return string.replaceAll("(^\\h*)|(\\h*$)"," ").trim();
    }

}
