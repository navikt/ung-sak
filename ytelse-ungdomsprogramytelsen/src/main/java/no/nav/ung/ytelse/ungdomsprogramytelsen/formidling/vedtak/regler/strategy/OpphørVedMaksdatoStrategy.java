package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørVedMaksdatoInnholdBygger;

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
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        // Opphør ved maksdato gjelder kun en åpen programperiode; er den lukket har det skjedd en reell sluttdatoendring (opphør/flytting).
        if (resultater.innholder(DetaljertResultatType.OPPHØR_VED_MAKSDATO)
            && !UngdomsprogramOpphørUtleder.harLukketProgramperiode(behandling.getId(), ungdomsprogramPeriodeRepository)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_VED_MAKSDATO_DOK, opphørVedMaksdatoInnholdBygger,
                "Automatisk brev ved opphør grunnet maksdato."));
        }
        return List.of();
    }

}

