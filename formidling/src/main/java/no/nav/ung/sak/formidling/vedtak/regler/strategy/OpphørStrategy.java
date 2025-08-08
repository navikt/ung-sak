package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.OpphørInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class OpphørStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final OpphørInnholdBygger opphørInnholdBygger;

    @Inject
    public OpphørStrategy(
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
        OpphørInnholdBygger opphørInnholdBygger
    ) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.opphørInnholdBygger = opphørInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medBrev(
            DokumentMalType.OPPHØR_DOK, opphørInnholdBygger,
            "Automatisk brev ved opphør.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            && resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO)
            && erFørsteSluttdato(behandling, ungdomsprogramPeriodeRepository);
    }

    static boolean erFørsteSluttdato(Behandling behandling, UngdomsprogramPeriodeRepository repo) {
        return behandling.getOriginalBehandlingId()
            .flatMap(repo::hentGrunnlag)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder().stream()
                .anyMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(false);
    }
}
