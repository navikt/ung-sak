package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

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
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringBarnetilleggInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringBarnetilleggStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger;

    @Inject
    public EndringBarnetilleggStrategy(EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger) {
        this.endringBarnetilleggInnholdBygger = endringBarnetilleggInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        if (resultatTidslinje.harÅrsak(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.ENDRING_BARNETILLEGG, endringBarnetilleggInnholdBygger, "Automatisk brev ved fødsel av barn."));
        }
        return List.of();
    }
}
