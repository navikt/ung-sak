package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vedtaksbrevresultat for hele behandlingen. Vet om det er flere vedtaksbrev.
 */
public record BehandlingVedtaksbrevResultat(
    boolean harBrev,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
    List<Vedtaksbrev> vedtaksbrevResultater,
    List<IngenBrev> ingenBrevResultater) implements FellesVedtaksbrevresultat  {

    public BehandlingVedtaksbrevResultat {
        // Valider at kun en av vedtaksbrevResultater og ingenBrevResultater har elementer
        boolean harVedtaksbrev = !vedtaksbrevResultater.isEmpty();
        boolean harIngenBrev = !ingenBrevResultater.isEmpty();
        if (harVedtaksbrev == harIngenBrev) {
            throw new IllegalArgumentException("Kun en av vedtaksbrevResultater eller ingenBrevResultater må ha elementer. Har vedtaksbrevResultater: " + vedtaksbrevResultater.size() + ", ingenBrevResultater: " + ingenBrevResultater.size());
        }
    }

    @Override
    public Optional<Vedtaksbrev> finnVedtaksbrev(DokumentMalType dokumentMalType) {
        return vedtaksbrevResultater.stream().filter(it -> it.dokumentMalType().equals(dokumentMalType)).findFirst();
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

    public Set<DokumentMalType> brevSomMåRedigeres() {
        return this.vedtaksbrevResultater().stream()
            .filter(
                v -> v.vedtaksbrevEgenskaper().kanRedigere() && !v.vedtaksbrevEgenskaper().kanOverstyreRediger())
            .map(Vedtaksbrev::dokumentMalType)
            .collect(Collectors.toSet());
    }

    public String forklaringer() {
        if (harBrev) {
            return vedtaksbrevResultater.stream().map(Vedtaksbrev::forklaring).collect(Collectors.joining(", "));
        } else {
            return ingenBrevResultater.stream().map(IngenBrev::forklaring).collect(Collectors.joining(", "));
        }
    }


    public String safePrint() {
        return "BehandlingVedtaksbrevResultat{" +
            "harBrev=" + harBrev +
            ", detaljertResultatTimeline=[" + DetaljertResultat.timelineToString(detaljertResultatTimeline)+ "]" +
            ", vedtaksbrevResultater=" + vedtaksbrevResultater +
            ", ingenBrevResultater=" + ingenBrevResultater +
            '}';
    }
}

