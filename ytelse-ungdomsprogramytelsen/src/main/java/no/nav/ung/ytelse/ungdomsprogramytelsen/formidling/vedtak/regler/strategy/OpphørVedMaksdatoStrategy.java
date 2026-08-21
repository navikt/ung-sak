package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørVedMaksdatoInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class OpphørVedMaksdatoStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public OpphørVedMaksdatoStrategy(
        OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørVedMaksdatoInnholdBygger = opphørVedMaksdatoInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        if (!resultatTidslinje.harÅrsak(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)) {
            return List.of();
        }
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        // Opphør ved maksdato gir kun brev når varselet er innenfor varslingsvinduet og programperioden fortsatt er åpen;
        // er den lukket har det i stedet skjedd en reell sluttdatoendring (opphør/flytting).
        if (erRelevantForVarslingOmOpphørVedMaksdato(grunnlag)
            && !UngdomsprogramOpphørUtleder.harLukketSluttdato(grunnlag)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_VED_MAKSDATO_DOK, opphørVedMaksdatoInnholdBygger,
                "Automatisk brev ved opphør grunnet maksdato."));
        }
        return List.of();
    }

    private static boolean erRelevantForVarslingOmOpphørVedMaksdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(
            grunnlag.hentForEksaktEnPeriode().getTomDato(),
            grunnlag.getPeriodeMaksDato().orElseThrow());
    }

}
