package no.nav.ung.sak.formidling.vedtak.regler;

import java.util.List;

public interface FellesVedtaksbrevresultat {
    boolean harBrev();
    String safePrint();
    List<IngenBrev> ingenBrevResultater();
    List<Vedtaksbrev> vedtaksbrevResultater();
}
