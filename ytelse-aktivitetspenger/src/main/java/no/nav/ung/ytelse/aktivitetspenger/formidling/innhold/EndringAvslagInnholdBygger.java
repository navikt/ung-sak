package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringUnderArbeid;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringAvslagDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class EndringAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private final Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester;
    private static final Map<VilkårType, BehandlingÅrsakType> vilkårOgBehandlingÅrsak = Map.of(
        VilkårType.BOSTEDSVILKÅR, BehandlingÅrsakType.ENDRET_BOSTED
    );

    @Inject
    public EndringAvslagInnholdBygger(VilkårResultatRepository vilkårResultatRepository,
                                      InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                      @Any Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.vilkårsavklaringTjenester = vilkårsavklaringTjenester;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var behandlingÅrsakMedVilkårsavklaring = TidslinjeUtil.values(tidslinje.tilVurdering()).stream()
            .flatMap(it -> it.behandlingsårsaker().stream()).distinct()
            .collect(Collectors.toMap(
                behandlingsårsak -> behandlingsårsak,
                behandlingsårsak -> VilkårsavklaringTjeneste.finnForÅrsak(vilkårsavklaringTjenester, behandlingsårsak)
                    .flatMap(it -> it.hentSenesteAvklaringForBehandling(behandling.getId()))
            ));

        var vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var vilkårVurdering = inngangsvilkårVurderingRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ingen eksisterende vilkårvurderinggrunnlag for behandlingId: " + behandling.getId()));

        var vilkårsavklaringBosted = behandlingÅrsakMedVilkårsavklaring.get(vilkårOgBehandlingÅrsak.get(VilkårType.BOSTEDSVILKÅR));
        var vurdertPeriodeBosted = finnVurdertPeriodeForAvslag(tidslinje, VilkårType.BOSTEDSVILKÅR, vilkårsavklaringBosted);
        if (!vurdertPeriodeBosted.isEmpty()) {
            return lagDto(
                vilkårsavklaringBosted.get(),
                AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vilkårene, vilkårVurdering, vurdertPeriodeBosted)
            );
        }

        throw new IllegalStateException("Fant ingen vilkårsavklaring med avslått periode for behandlingId: " + behandling.getId());
    }

    private static LocalDateTimeline<Boolean> finnVurdertPeriodeForAvslag(DetaljertResultatTidslinje tidslinje,
                                                                          VilkårType vilkårType,
                                                                          Optional<VilkårsavklaringUnderArbeid> vilkårsavklaring) {
        if (vilkårsavklaring.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        var avslåttVilkårsPeriode = tidslinje.tilVurdering()
            .filterValue(r -> r.avslåtteVilkår().stream().anyMatch(v -> v.vilkårType() == vilkårType));

        var vilkårsavklaringPeriode = TidslinjeUtil.tilTidslinjeKomprimert(List.of(vilkårsavklaring.get().periode()));
        var vurdertPeriode = vilkårsavklaringPeriode.intersection(avslåttVilkårsPeriode);
        return vurdertPeriode.isEmpty() ? LocalDateTimeline.empty() : vurdertPeriode;
    }

    public TemplateInnholdResultat lagDto(VilkårsavklaringUnderArbeid vilkårsavklaring, AvslåttBosted avslåttBosted) {
        return new TemplateInnholdResultat(
            vilkårsavklaring.avklaringtype() == Avklaringtype.OPPHØR ? TemplateType.AKTIVITETSPENGER_OPPHØR : TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG,
            new EndringAvslagDto(
                vilkårsavklaring.periode().tilPeriode(),
                avslåttBosted
            )
        );
    }

}

