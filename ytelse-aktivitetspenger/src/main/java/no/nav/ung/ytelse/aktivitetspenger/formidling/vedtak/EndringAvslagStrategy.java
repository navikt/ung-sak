package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringAvslagInnholdBygger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringAvslagStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringAvslagInnholdBygger endringAvslagInnholdBygger;
    private final Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester;

    private static final Map<VilkårType, BehandlingÅrsakType> vilkårOgBehandlingÅrsak = Map.of(
        VilkårType.BOSTEDSVILKÅR, BehandlingÅrsakType.ENDRET_BOSTED
    );

    @Inject
    public EndringAvslagStrategy(EndringAvslagInnholdBygger endringAvslagInnholdBygger,
                                 @Any Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester) {
        this.endringAvslagInnholdBygger = endringAvslagInnholdBygger;
        this.vilkårsavklaringTjenester = vilkårsavklaringTjenester;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        List<Vilkårsavklaring> vilkårsavklaringerForAvslåtteVilkår = vilkårOgBehandlingÅrsak.entrySet().stream()
            .map(entry -> harVilkårsavklaringForAvslåttVilkår(behandling, resultatTidslinje.tilVurdering(), entry.getKey(), entry.getValue()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        var harVilkårSomErAvslåttIBehandling = !vilkårsavklaringerForAvslåtteVilkår.isEmpty();
        var harAvklaringMedOpphør = vilkårsavklaringerForAvslåtteVilkår.stream()
            .anyMatch(avklaring -> avklaring.avklaringtype() == Avklaringtype.OPPHØR);

        if (harVilkårSomErAvslåttIBehandling) {
            return List.of(new VedtaksbrevStrategyResultat(
                harAvklaringMedOpphør ? DokumentMalType.OPPHØR_DOK : DokumentMalType.AVSLAG__DOK,
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

    /**
     * Avslåtte vilkårsperioder sjekkes mot vilkårsavklaringen for det samme vilkåret
     */
    private Optional<Vilkårsavklaring> harVilkårsavklaringForAvslåttVilkår(Behandling behandling,
                                                                           LocalDateTimeline<DetaljertResultat> tilVurdering,
                                                                           VilkårType vilkårType,
                                                                           BehandlingÅrsakType behandlingÅrsakType) {
        var vilkårsAvklaring = VilkårsavklaringTjeneste.finnForÅrsak(vilkårsavklaringTjenester, behandlingÅrsakType)
            .flatMap(it -> it.hentSenesteAvklaringForBehandling(behandling.getId()));

        return vilkårsAvklaring.flatMap(avklaring -> {
            var avklaringsperiode = avklaring.periode().toLocalDateInterval();
            boolean harAvslåtteVilkår = perioderMedAvslåttVilkår(tilVurdering, vilkårType).anyMatch(avklaringsperiode::overlaps);
            if (!harAvslåtteVilkår) {
                return Optional.empty();
            }
            return Optional.of(avklaring);
        });
    }

    private static Stream<LocalDateInterval> perioderMedAvslåttVilkår(LocalDateTimeline<DetaljertResultat> tilVurdering, VilkårType vilkårType) {
        return tilVurdering.stream()
            .filter(it -> it.getValue().avslåtteVilkår().stream().anyMatch(vilkår -> vilkår.vilkårType() == vilkårType))
            .map(LocalDateSegment::getLocalDateInterval);
    }
}

