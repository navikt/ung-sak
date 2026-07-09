package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørOpphevetInnholdBygger;

import java.util.List;

/**
 * Vedtaksbrev for opphevelse av et tidligere opphør av ungdomsprogrammet, f.eks. når opphørsdato/sluttdato ble
 * satt feil eller bruker har fått medhold i klage på opphøret. Denne behandles uavhengig av øvrig
 * programperiode-logikk i {@link ProgramPeriodeStrategy}, siden årsaken er en egen, distinkt behandlingsårsak.
 * <p>
 * Skillet mellom om opphøret faktisk ble vedtatt i en tidligere behandling ({@code DetaljertResultatType.OPPHØR_OPPHEVET},
 * gir brev) eller om opphør og opphevelse havnet på samme, fortsatt åpne behandling
 * ({@code DetaljertResultatType.OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING}, ingen brev) er allerede avgjort av
 * {@code UngDetaljertResultatTidslinjeUtleder} - denne strategien trenger bare å lese resultattypen.
 * <p>
 * NB: for avbrutt-tilfellet må vi returnere et eksplisitt "ingen brev, årsak IKKE_RELEVANT"-resultat
 * (ikke tom liste). Tom liste tolkes av {@code YtelseVedtaksbrevRegler} som at strategien ikke er relevant
 * for behandlingen, og fører da til at perioden faller på fallback-resultatet IKKE_IMPLEMENTERT, som igjen
 * gir aksjonspunktet FORESLÅ_VEDTAK_MANUELT og krever manuell "Fatt vedtak" i stedet for automatisk vedtak.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class OpphørOpphevetStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger;

    @Inject
    public OpphørOpphevetStrategy(OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger) {
        this.opphørOpphevetInnholdBygger = opphørOpphevetInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        if (resultater.innholder(DetaljertResultatType.OPPHØR_OPPHEVET)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_OPPHEVET_DOK, opphørOpphevetInnholdBygger,
                "Automatisk brev ved opphevelse av opphør."));
        }
        if (resultater.innholder(DetaljertResultatType.OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING)) {
            return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,
                "Opphør og opphevelse havnet på samme, fortsatt åpne behandling - opphøret ble aldri vedtatt, ikke behov for vedtaksbrev."));
        }
        return List.of();
    }

}

