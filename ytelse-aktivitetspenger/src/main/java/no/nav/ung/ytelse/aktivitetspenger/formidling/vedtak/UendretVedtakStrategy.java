package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class UendretVedtakStrategy implements VedtaksbrevInnholdbyggerStrategy {

    @Inject
    public UendretVedtakStrategy() {    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var endretBosted = resultatTidslinje.filtrerPåÅrsak(BehandlingÅrsakType.ENDRET_BOSTED);
        boolean harAvslag = endretBosted.stream().anyMatch(it -> !it.getValue().avslåtteVilkår().isEmpty());
        if (!endretBosted.isEmpty() && !harAvslag) {
            return List.of(VedtaksbrevStrategyResultat.utenBrev( IngenBrevÅrsakType.IKKE_RELEVANT, "Revurdering uten endring. "));
        }
        return List.of();
    }

    @Override
    public Presedens presedens() {
        return Presedens.NORMAL;
    }
}
