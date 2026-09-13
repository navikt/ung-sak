package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.FørstegangsAvslagInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class AvslagInngangsvilkårStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final FørstegangsAvslagInnholdBygger førstegangsAvslagInnholdBygger;

    @Inject
    public AvslagInngangsvilkårStrategy(FørstegangsAvslagInnholdBygger førstegangsAvslagInnholdBygger) {
        this.førstegangsAvslagInnholdBygger = førstegangsAvslagInnholdBygger;
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var periodeTilVurdering = resultatTidslinje.filtrerPåÅrsak(BehandlingÅrsakType.NY_SØKT_PERIODE);
        boolean fullAvslag = !periodeTilVurdering.isEmpty()
            && periodeTilVurdering.stream().noneMatch(it -> it.getValue().avslåtteVilkår().isEmpty());

        if (fullAvslag) {
            return List.of(new VedtaksbrevStrategyResultat(
                DokumentMalType.AVSLAG__DOK,
                førstegangsAvslagInnholdBygger,
                VedtaksbrevEgenskaper.builder()
                    .kanHindre(true)
                    .kanOverstyreHindre(true)
                    .kanRedigere(true)
                    .kanOverstyreRediger(true)
                    .build(),
                null,
                "Avslagsbrev ved avslag på inngangsvilkår"
            ));
        }

        return List.of();
    }

}
