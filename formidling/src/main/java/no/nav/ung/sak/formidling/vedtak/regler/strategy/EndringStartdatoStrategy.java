package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringStartdatoStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;

    @Inject
    public EndringStartdatoStrategy(EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger) {
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger, "Automatisk brev ved endring av startdato");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            && resultater.innholder(DetaljertResultatType.ENDRING_STARTDATO);
    }

}
