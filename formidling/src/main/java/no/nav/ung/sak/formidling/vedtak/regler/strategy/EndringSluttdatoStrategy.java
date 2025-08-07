package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.innhold.OpphørInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringSluttdatoStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final OpphørInnholdBygger opphørInnholdBygger;
    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;

    @Inject
    public EndringSluttdatoStrategy(
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
        OpphørInnholdBygger opphørInnholdBygger,
        EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger
    ) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.opphørInnholdBygger = opphørInnholdBygger;
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (erFørsteOpphør(behandling)) {
            return VedtaksbrevStrategyResultat.medBrev(
                DokumentMalType.OPPHØR_DOK, opphørInnholdBygger,
                "Automatisk brev ved opphør.");
        }

        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger, "Automatisk brev ved endring av sluttdato");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            && resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO);
    }

    private boolean erFørsteOpphør(Behandling behandling) {
        var forrigeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getOriginalBehandlingId().orElseThrow(
            () -> new IllegalStateException("Må ha original behandling ved opphør")
        )).orElseThrow(() -> new IllegalStateException("Mangler grunnlag for forrige behandling"));
        return forrigeGrunnlag.getUngdomsprogramPerioder().getPerioder().stream()
            .anyMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato()));
    }
}
