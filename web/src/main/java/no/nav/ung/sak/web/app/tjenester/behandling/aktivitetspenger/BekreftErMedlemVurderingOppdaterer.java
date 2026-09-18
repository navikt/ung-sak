package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import no.nav.ung.sak.kontrakt.aktivitetspenger.BekreftErMedlemVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.UtenlandsoppholdDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.medlemskap.ForutgåendeMedlemskapTjeneste;

import java.util.NavigableSet;
import java.util.TreeSet;

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
        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());

        var resultatBuilder = param.getVilkårResultatBuilder();
        var vilkårPeriodeBuilder = resultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        var relevantePerioder = filtrerBortIkkeRelevantePerioder(param.getBehandlingId(), perioderTilVurdering);
        var periodeVurdert = relevantePerioder.stream().filter(it -> it.equals(DatoIntervallEntitet.fra(dto.getVilkårsperiode()))).findFirst().orElseThrow(
            () -> new IllegalStateException("Kunne ikke finne periode " + dto.getVilkårsperiode() + " i relevante perioder " + relevantePerioder)
        );

        Utfall utfall = dto.getErVilkårOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        Avslagsårsak avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? mapAvslagsårsak(dto.getAvslagsårsak()) : null;

        var medlemskap = forutgåendeMedlemskapTjeneste.hentMedlemskapForBehandlingSomDto(param.getBehandlingId())
            .stream()
            .map(this::maskerUtenlandskNasjonalId)
            .toList();

        String regelInput = new VilkårJsonObjectMapper().writeValueAsString(medlemskap);

        vilkårPeriodeBuilder.leggTil(vilkårPeriodeBuilder
            .hentBuilderFor(periodeVurdert)
            .medUtfallManuell(utfall)
            .medAvslagsårsak(avslagsårsak)
            .medRegelInput(regelInput)
            .medBegrunnelse(dto.getBegrunnelse()));

        resultatBuilder.leggTil(vilkårPeriodeBuilder);

        return OppdateringResultat.nyttResultat();
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

    private NavigableSet<DatoIntervallEntitet> filtrerBortIkkeRelevantePerioder(Long behandlingId, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var ikkeRelevantePerioder = vilkårResultatRepository.hent(behandlingId)
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> Utfall.IKKE_RELEVANT.equals(p.getGjeldendeUtfall()))
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .toList();
        if (ikkeRelevantePerioder.isEmpty()) {
            return perioderTilVurdering;
        }
        var resultat = new TreeSet<>(perioderTilVurdering);
        ikkeRelevantePerioder.forEach(resultat::remove);
        return resultat;
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
