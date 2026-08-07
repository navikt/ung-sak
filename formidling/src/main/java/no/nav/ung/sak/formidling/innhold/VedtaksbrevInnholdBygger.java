package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;

import java.math.BigDecimal;
import java.math.RoundingMode;

public interface VedtaksbrevInnholdBygger {

    /**
     * Bygger komplett dto for brev template. Byggeren velger selv om den trenger hele vilkårsbildet eller kun
     * periodene til vurdering ({@link DetaljertResultatTidslinje#tilVurdering()}).
     */
    TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje);

    /**
     * Standard heltall avrunding for brev
     */
    static long tilHeltall(BigDecimal faktor) {
        return faktor.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}


