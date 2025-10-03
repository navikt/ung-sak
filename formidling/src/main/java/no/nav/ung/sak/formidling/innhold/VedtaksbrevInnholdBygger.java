package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import org.apache.commons.lang3.NotImplementedException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public interface VedtaksbrevInnholdBygger {

    /**
     * Bygger komplett dto for brev template
     *
     * @param behandling
     * @param detaljertResultatTidslinje - tidslinje med relevante perioder for denne behandlingen og deres resultat
     * @return
     */
    TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje);
    default TemplateInnholdResultat bygg(Behandling behandling) {
        throw new NotImplementedException("Ikke implementert for typen innholdsbygger");
    }


    /**
     * Standard heltall avrunding for brev
     */
    static long tilHeltall(BigDecimal faktor) {
        return faktor.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}


