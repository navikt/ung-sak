package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;

public class EndringStartdatoByggerStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;

    @Inject
    public EndringStartdatoByggerStrategy(EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger) {
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
    }

    @Override
    public ByggerResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new ByggerResultat(endringProgramPeriodeInnholdBygger, "Automatisk brev ved endring av startdato");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater
            .utenom(DetaljertResultatType.INNVILGET_UTEN_ÅRSAK)
            .innholderBare(DetaljertResultatType.ENDRING_STARTDATO);
    }

}
