package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.UendretInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class UendretStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private UendretInnholdBygger uendretInnholdBygger;

    public UendretStrategy() {
    }

    @Inject
    public UendretStrategy(UendretInnholdBygger uendretInnholdBygger) {
        this.uendretInnholdBygger = uendretInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        // Fordi brevets formål er å informere om at det ikke er endringer på vedtaket, sjekkes det i tillegg at det ikke finnes andre årsaker.
        var harIkkeAndreBehandlingÅrsaker = resultatTidslinje.tilVurdering().stream()
            .allMatch(it -> it.getValue().harÅrsak(BehandlingÅrsakType.ENDRET_BOSTED));

        var perioderMedBehandlingÅrsakSomKanHaUendretBrev = resultatTidslinje.filtrerPåÅrsak(BehandlingÅrsakType.ENDRET_BOSTED);

        var uendretVurderingAvInngangsvilkår = !perioderMedBehandlingÅrsakSomKanHaUendretBrev.isEmpty() &&
            perioderMedBehandlingÅrsakSomKanHaUendretBrev.stream()
            .allMatch(it -> it.getValue().avslåtteVilkår().isEmpty());

        if (harIkkeAndreBehandlingÅrsaker && uendretVurderingAvInngangsvilkår) {
            return List.of(new VedtaksbrevStrategyResultat(
                DokumentMalType.INGEN_ENDRING,
                uendretInnholdBygger,
                VedtaksbrevEgenskaper.builder()
                    .kanHindre(true)
                    .kanOverstyreHindre(true)
                    .kanRedigere(true)
                    .kanOverstyreRediger(true)
                    .build(),
                null,
                "Revurdering med forslag til avslag/opphør uten endringer"
            ));
        }
        return List.of();
    }
}
