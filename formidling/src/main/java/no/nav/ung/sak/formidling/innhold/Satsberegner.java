package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;

class Satsberegner {


    static long beregnDagsatsInklBarnetillegg(UngdomsytelseSatser satser) {
        return tilHeltall(satser.dagsats().add(BigDecimal.valueOf(satser.dagsatsBarnetillegg())));
    }

    static long beregnBarnetilleggSats(UngdomsytelseSatser satser) {
        if (satser.antallBarn() <= 0) {
            return 0;
        }
         return tilHeltall(BigDecimal.valueOf(satser.dagsatsBarnetillegg())
             .divide(BigDecimal.valueOf(satser.antallBarn()), RoundingMode.HALF_UP));
    }
}
