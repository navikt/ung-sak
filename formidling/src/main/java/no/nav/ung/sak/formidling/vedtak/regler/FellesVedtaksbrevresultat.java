package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.dokument.DokumentMalType;

import java.util.List;
import java.util.Optional;

public interface FellesVedtaksbrevresultat {
    boolean harBrev();
    String safePrint();
    List<IngenBrev> ingenBrevResultater();
    List<Vedtaksbrev> vedtaksbrevResultater();

    Optional<Vedtaksbrev> finnVedtaksbrev(DokumentMalType dokumentMalType);
}
