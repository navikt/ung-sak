package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
import no.nav.ung.sak.formidling.vedtak.resultat.BehandlingÅrsakHelper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.ForlengetPeriodeInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class ForlengetPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public ForlengetPeriodeStrategy(
        ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.forlengetPeriodeInnholdBygger = forlengetPeriodeInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        // Forlengelse gjelder kun ved en åpen programperiode; er den lukket har det skjedd en reell sluttdatoendring (opphør/flytting).
        if (new BehandlingÅrsakHelper(detaljertResultat).har(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM)
            && harForlengetPeriode(behandling)
            && !UngdomsprogramOpphørUtleder.harLukketProgramperiode(behandling.getId(), ungdomsprogramPeriodeRepository)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.FORLENGET_PERIODE, forlengetPeriodeInnholdBygger,
                "Automatisk brev ved forlenget periode"));
        }
        return List.of();
    }

    private boolean harForlengetPeriode(Behandling behandling) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .map(UngdomsprogramPeriodeGrunnlag::harForlengetPeriode)
            .orElse(false);
    }

}
