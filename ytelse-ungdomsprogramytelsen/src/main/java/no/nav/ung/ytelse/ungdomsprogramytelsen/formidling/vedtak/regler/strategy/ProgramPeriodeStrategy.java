package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.ForlengetPeriodeInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørVedMaksdatoInnholdBygger;

import java.util.ArrayList;
import java.util.List;

/**
 * Samler all brevutledning for endringer i ungdomsprogramperioden. De ulike utfallene (opphør, flytting av
 * start-/sluttdato, forlengelse, opphør ved maksdato) er innbyrdes avhengige og avgjøres derfor her i én
 * strategi i stedet for å la separate strategier sjekke hverandres resultattyper.
 */
@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class ProgramPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørInnholdBygger opphørInnholdBygger;
    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;
    private final ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger;
    private final OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public ProgramPeriodeStrategy(
        OpphørInnholdBygger opphørInnholdBygger,
        EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger,
        ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger,
        OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørInnholdBygger = opphørInnholdBygger;
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
        this.forlengetPeriodeInnholdBygger = forlengetPeriodeInnholdBygger;
        this.opphørVedMaksdatoInnholdBygger = opphørVedMaksdatoInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        boolean harSluttdato = resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO);
        boolean harStartdato = resultater.innholder(DetaljertResultatType.ENDRING_STARTDATO);

        var brev = new ArrayList<VedtaksbrevStrategyResultat>();
        boolean leggTilProgramperiodeEndring = harStartdato;

        if (harSluttdato) {
            if (erFørsteSluttdato(behandling, ungdomsprogramPeriodeRepository)) {
                brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                    DokumentMalType.OPPHØR_DOK, opphørInnholdBygger,
                    "Automatisk brev ved opphør."));
            } else {
                leggTilProgramperiodeEndring = true;
            }
        }

        if (leggTilProgramperiodeEndring) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger,
                "Automatisk brev ved endring av programperiode"));
        }

        // Forlengelse og opphør ved maksdato er kun aktuelt når det ikke samtidig er endring av sluttdato.
        if (!harSluttdato) {
            if (resultater.innholder(DetaljertResultatType.FORLENGET_PERIODE)) {
                brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                    DokumentMalType.FORLENGET_PERIODE, forlengetPeriodeInnholdBygger,
                    "Automatisk brev ved forlenget periode"));
            }
            if (resultater.innholder(DetaljertResultatType.OPPHØR_VED_MAKSDATO)) {
                brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                    DokumentMalType.OPPHOR_VED_MAKSDATO_DOK, opphørVedMaksdatoInnholdBygger,
                    "Automatisk brev ved opphør grunnet maksdato."));
            }
        }

        return List.copyOf(brev);
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        return resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO)
            || resultater.innholder(DetaljertResultatType.ENDRING_STARTDATO)
            || resultater.innholder(DetaljertResultatType.FORLENGET_PERIODE)
            || resultater.innholder(DetaljertResultatType.OPPHØR_VED_MAKSDATO);
    }

    private static boolean erFørsteSluttdato(Behandling behandling, UngdomsprogramPeriodeRepository repo) {
        return behandling.getOriginalBehandlingId()
            .flatMap(repo::hentGrunnlag)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder().stream()
                .anyMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(false);
    }

}
