package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

import java.math.BigDecimal;
import java.math.RoundingMode;

public interface VedtaksbrevInnholdBygger {

    /**
     * Bygger komplett dto for brev template
     *
     * @param detaljertResultatTidslinje - kun periodene som er til vurdering i denne behandlingen, med sitt resultat
     */
    TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje);

    /**
     * Standard heltall avrunding for brev
     */
    static long tilHeltall(BigDecimal faktor) {
        return faktor.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}


