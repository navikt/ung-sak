package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
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
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringAvslagInnholdBygger;

import java.util.List;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringAvslagStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringAvslagInnholdBygger endringAvslagInnholdBygger;

    @Inject
    public EndringAvslagStrategy(EndringAvslagInnholdBygger endringAvslagInnholdBygger) {
        this.endringAvslagInnholdBygger = endringAvslagInnholdBygger;

    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        boolean erIkkeOppfyltRevurdering = resultatTidslinje.filtrerPåÅrsak(BehandlingÅrsakType.ENDRET_BOSTED)
            .stream()
            .anyMatch(it -> !it.getValue().avslåtteVilkår().isEmpty());

        if (erIkkeOppfyltRevurdering) {
        return List.of(new VedtaksbrevStrategyResultat(
            DokumentMalType.OPPHØR_DOK,
            endringAvslagInnholdBygger,
            VedtaksbrevEgenskaper.builder()
                .kanHindre(true)
                .kanOverstyreHindre(true)
                .kanRedigere(true)
                .kanOverstyreRediger(true)
                .build(),
            null,
            "Avslagsbrev ved revurdering med ikke oppfylte vilkår"
        ));
        }

        return List.of();
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }
}

