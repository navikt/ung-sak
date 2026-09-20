package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.BekreftErMedlemVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.UtenlandsoppholdDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;

import java.util.List;
import java.util.NavigableSet;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftErMedlemVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftErMedlemVurderingOppdaterer implements AksjonspunktOppdaterer<BekreftErMedlemVurderingDto> {

    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private final ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public BekreftErMedlemVurderingOppdaterer(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                              ForutgåendeMedlemskapTjeneste forutgåendeMedlemskapTjeneste,
                                              VilkårResultatRepository vilkårResultatRepository) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.forutgåendeMedlemskapTjeneste = forutgåendeMedlemskapTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftErMedlemVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var periodeTilVurderingTidslinje = lagPeriodeTilVurderingTidslinje(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType(), param.getBehandlingId());

        var periodeVurdert = finnOgValiderPeriodeVurdert(dto, param, periodeTilVurderingTidslinje);

        Utfall utfall = dto.getErVilkårInnvilget() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        Avslagsårsak avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? mapAvslagsårsak(dto.getAvslagsårsak()) : null;

        var medlemskap = forutgåendeMedlemskapTjeneste.hentMedlemskapForBehandlingSomDto(param.getBehandlingId())
            .stream()
            .map(this::maskerUtenlandskNasjonalId)
            .toList();

        String regelInput = new VilkårJsonObjectMapper().writeValueAsString(medlemskap);

        var resultatBuilder = param.getVilkårResultatBuilder();
        var vilkårPeriodeBuilder = resultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        vilkårPeriodeBuilder.leggTil(vilkårPeriodeBuilder
            .hentBuilderFor(periodeVurdert)
            .medUtfallManuell(utfall)
            .medAvslagsårsak(avslagsårsak)
            .medRegelInput(regelInput)
            .medBegrunnelse(dto.getBegrunnelse()));

        resultatBuilder.leggTil(vilkårPeriodeBuilder);

        return OppdateringResultat.nyttResultat();
    }

    private DatoIntervallEntitet finnOgValiderPeriodeVurdert(BekreftErMedlemVurderingDto dto, AksjonspunktOppdaterParameter param, LocalDateTimeline<Boolean> periodeTilVurderingTidslinje) {
        var vilkår = vilkårResultatRepository.hent(param.getRef().getBehandlingId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET).orElseThrow();

        var vilkårTidslinje = new LocalDateTimeline<>(vilkår.getPerioder().stream()
            .filter(it -> it.getUtfall() != Utfall.IKKE_RELEVANT)
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), it))
            .toList());

        var relevantePerioder = vilkårTidslinje.intersection(periodeTilVurderingTidslinje);
        DatoIntervallEntitet periodeVurdert = DatoIntervallEntitet.fra(dto.getVilkårsperiode());
        var periodeVurdertTidslinje = TidslinjeUtil.tilTidslinje(List.of(periodeVurdert));
        if (!periodeVurdertTidslinje.disjoint(relevantePerioder).isEmpty()) {
            throw new IllegalStateException("Periode vurdert " + periodeVurdert + " er ikke delmengde av periode til vurdering " + relevantePerioder);
        }
        return periodeVurdert;
    }

    private LocalDateTimeline<Boolean> lagPeriodeTilVurderingTidslinje(FagsakYtelseType fagsakYtelseType, BehandlingType type, Long id) {
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(fagsakYtelseType, type);
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(id, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        return TidslinjeUtil.tilTidslinje(perioderTilVurdering);
    }

    private MedlemskapDto maskerUtenlandskNasjonalId(MedlemskapDto medlemskapDto) {
        return new MedlemskapDto(
            medlemskapDto.forutgåendePeriode(),
            medlemskapDto.harBoddINorge(),
            medlemskapDto.harJobbetINorge(),
            medlemskapDto.harJobbetUtenforNorge(),
            medlemskapDto.journalpostId(),
            medlemskapDto.utenlandsopphold().stream()
                .map(u -> new UtenlandsoppholdDto(
                        u.periode(),
                        u.land(),
                        u.landkode(),
                        u.harTrygdeavtale(),
                        u.harJobbetIPerioden(),
                        u.utenlandskNasjonalId() != null ? "[MASKERT]" : null
                    )
                ).toList(
                ));
    }

    private Avslagsårsak mapAvslagsårsak(MedlemskapAvslagsÅrsakType medlemskapAvslagsÅrsakType) {
        return switch (medlemskapAvslagsÅrsakType) {
            case SØKER_IKKE_MEDLEM -> Avslagsårsak.SØKER_ER_IKKE_MEDLEM;
        };
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
