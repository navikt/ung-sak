package no.nav.ung.sak.behandlingslager.formidling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class XhtmlBrevRenser {

    private final Logger LOG = LoggerFactory.getLogger(XhtmlBrevRenser.class);

    private static final Document.OutputSettings XHTML_SETTINGS = new Document.OutputSettings()
        .syntax(Document.OutputSettings.Syntax.xml)
        .charset(StandardCharsets.UTF_8)
        .prettyPrint(false);

    private static final List<String> SAFE_TAGS = List.of("b", "em", "i", "strong", "u", "br", "li", "ol", "p", "ul", "h1", "h2", "h3", "a");

    private static final Safelist SAFELIST = Safelist.none().addTags(SAFE_TAGS.toArray(new String[0]));

    public String rens(String input) {
        Objects.requireNonNull(input, "Input må være satt");
        String rensetHtml = Jsoup.clean(input, "", SAFELIST, XHTML_SETTINGS);
        loggHvisRenset(input, rensetHtml);
        return rensetHtml;
    }

    private void loggHvisRenset(String input, String rensetHtml) {
        var original = Jsoup.parseBodyFragment(input);
        var renset = Jsoup.parseBodyFragment(rensetHtml);
        int orgAntallElementer = original.select("*").size();
        int cleanedAntallElementer = renset.select("*").size();
        if (orgAntallElementer != cleanedAntallElementer) {
            LOG.warn("Rensing endret antall elementer i original! Antall elementer i original: {}, antall i renset: {}", orgAntallElementer, cleanedAntallElementer);
        }
    }

}
