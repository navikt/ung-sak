package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

import java.util.Collections;
import java.util.List;

/**
 * Vedtaksbrevresultat for hele behandlingen. Vet om det er flere vedtaksbrev.
 */
public record BehandlingVedtaksbrevResultat(
    boolean harBrev,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
    List<Vedtaksbrev> vedtaksbrevResultater,
    List<IngenBrev> ingenBrevResultater) {

    public BehandlingVedtaksbrevResultat {
        // Valider at kun en av vedtaksbrevResultater og ingenBrevResultater har elementer
        boolean harVedtaksbrev = !vedtaksbrevResultater.isEmpty();
        boolean harIngenBrev = !ingenBrevResultater.isEmpty();
        if (harVedtaksbrev == harIngenBrev) {
            throw new IllegalArgumentException("Kun en av vedtaksbrevResultater eller ingenBrevResultater må ha elementer. Har vedtaksbrevResultater: " + vedtaksbrevResultater.size() + ", ingenBrevResultater: " + ingenBrevResultater.size());
        }
    }

    public static BehandlingVedtaksbrevResultat medBrev(
        LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
        List<Vedtaksbrev> vedtaksbrevResultater) {
        return new BehandlingVedtaksbrevResultat(true, detaljertResultatTimeline, vedtaksbrevResultater, Collections.emptyList());
    }

    public static BehandlingVedtaksbrevResultat utenBrev(LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
                                                         List<IngenBrev> ingenBrevResultater) {
                return new BehandlingVedtaksbrevResultat(false, detaljertResultatTimeline, Collections.emptyList() , ingenBrevResultater );
    }

    public String safePrint() {
        return "BehandlingVedtaksbrevResultat{" +
            "harBrev=" + harBrev +
            ", detaljertResultatTimeline=" + DetaljertResultat.timelineToString(detaljertResultatTimeline) +
            ", vedtaksbrevResultater=" + vedtaksbrevResultater +
            ", ingenBrevResultater=" + ingenBrevResultater +
            '}';
    }

}

