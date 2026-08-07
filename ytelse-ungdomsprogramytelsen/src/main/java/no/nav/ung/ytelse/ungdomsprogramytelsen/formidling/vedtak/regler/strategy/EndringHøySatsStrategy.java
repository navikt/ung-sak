package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.EndringHøySatsInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class EndringHøySatsStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringHøySatsInnholdBygger endringHøySatsInnholdBygger;

    @Inject
    public EndringHøySatsStrategy(EndringHøySatsInnholdBygger endringHøySatsInnholdBygger) {
        this.endringHøySatsInnholdBygger = endringHøySatsInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        if (resultatTidslinje.harÅrsak(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.ENDRING_HØY_SATS, endringHøySatsInnholdBygger, "Automatisk brev ved endring til høy sats."));
        }
        return List.of();
    }

}
