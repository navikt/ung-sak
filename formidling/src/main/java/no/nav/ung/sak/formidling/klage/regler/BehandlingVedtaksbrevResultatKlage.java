package no.nav.ung.sak.formidling.klage.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.vedtak.regler.FellesVedtaksbrevresultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrev;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Vedtaksbrevresultat for hele behandlingen. Vet om det er flere vedtaksbrev.
 */
public record BehandlingVedtaksbrevResultatKlage(
    boolean harBrev,
    List<Vedtaksbrev> vedtaksbrevResultater,
    List<IngenBrev> ingenBrevResultater)  implements FellesVedtaksbrevresultat {

    public BehandlingVedtaksbrevResultatKlage {
        // Valider at kun en av vedtaksbrevResultater og ingenBrevResultater har elementer
        boolean harVedtaksbrev = !vedtaksbrevResultater.isEmpty();
        boolean harIngenBrev = !ingenBrevResultater.isEmpty();
        if (harVedtaksbrev == harIngenBrev) {
            throw new IllegalArgumentException("Kun en av vedtaksbrevResultater eller ingenBrevResultater må ha elementer. Har vedtaksbrevResultater: " + vedtaksbrevResultater.size() + ", ingenBrevResultater: " + ingenBrevResultater.size());
        }
    }

    public static BehandlingVedtaksbrevResultatKlage medBrev(List<Vedtaksbrev> vedtaksbrevResultater) {
        return new BehandlingVedtaksbrevResultatKlage(true, vedtaksbrevResultater, Collections.emptyList());
    }

    public static BehandlingVedtaksbrevResultatKlage utenBrev(List<IngenBrev> ingenBrevResultater) {
        return new BehandlingVedtaksbrevResultatKlage(false, Collections.emptyList(), ingenBrevResultater );
    }

    @Override
    public Optional<Vedtaksbrev> finnVedtaksbrev(DokumentMalType dokumentMalType) {
        return vedtaksbrevResultater.stream().filter(it -> it.dokumentMalType().equals(dokumentMalType)).findFirst();
    }


    public String safePrint() {
        return "BehandlingVedtaksbrevResultat{" +
            "harBrev=" + harBrev +
            ", vedtaksbrevResultater=" + vedtaksbrevResultater +
            ", ingenBrevResultater=" + ingenBrevResultater +
            '}';
    }
}

