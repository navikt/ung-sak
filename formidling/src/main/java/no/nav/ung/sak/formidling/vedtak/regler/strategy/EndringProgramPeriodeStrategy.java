package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringProgramPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndringProgramPeriodeStrategy(EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger, "Automatisk brev ved endring av programperiode");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            && (resultater.innholder(DetaljertResultatType.ENDRING_STARTDATO)
                || resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO) && !OpphørStrategy.erFørsteSluttdato(behandling, ungdomsprogramPeriodeRepository));
    }

}
