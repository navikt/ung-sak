package no.nav.foreldrepenger.behandling.steg.medlemskap;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.VurderLøpendeMedlemskap;
import no.nav.vedtak.konfig.Tid;

@BehandlingStegRef(kode = "VULOMED")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderLøpendeMedlemskapSteg implements BehandlingSteg {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VurderLøpendeMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
                                       BehandlingRepositoryProvider provider) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.medlemskapVilkårPeriodeRepository = provider.getMedlemskapVilkårPeriodeRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
    }

    VurderLøpendeMedlemskapSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        // FIXME K9 : Skrive om til å vurdere de periodene som vilkåret er brutt opp på.
        Map<LocalDate, VilkårData> vurderingsTilDataMap = vurderLøpendeMedlemskap.vurderLøpendeMedlemskap(behandlingId);
        if (!vurderingsTilDataMap.isEmpty()) {
            Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
            final var behandlingsresultat = behandling.getBehandlingsresultat();
            VilkårResultatBuilder vilkårResultatBuilder = VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat());

            final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            mapPerioderTilVilkårsPerioder(vilkårBuilder, vurderingsTilDataMap);
            vilkårResultatBuilder.leggTil(vilkårBuilder);
            BehandlingLås lås = kontekst.getSkriveLås();
            final var nyttResultat = vilkårResultatBuilder.build();
            behandlingsresultat.medOppdatertVilkårResultat(nyttResultat);
            behandlingRepository.lagre(nyttResultat, lås);

            MedlemskapVilkårPeriodeGrunnlagEntitet.Builder builder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
            MedlemskapsvilkårPeriodeEntitet.Builder perioderBuilder = builder.getPeriodeBuilder();
            vurderingsTilDataMap.forEach((vurderingsdato, vilkårData) -> {
                MedlemskapsvilkårPerioderEntitet.Builder periodeBuilder = perioderBuilder.getBuilderForVurderingsdato(vurderingsdato);
                periodeBuilder.medVurderingsdato(vurderingsdato);
                periodeBuilder.medVilkårUtfall(vilkårData.getUtfallType());
                Optional.ofNullable(vilkårData.getVilkårUtfallMerknad()).ifPresent(periodeBuilder::medVilkårUtfallMerknad);
                perioderBuilder.leggTil(periodeBuilder);
            });
            builder.medMedlemskapsvilkårPeriode(perioderBuilder);
            medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builder);

            behandlingRepository.lagre(behandling, lås);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private VilkårBuilder mapPerioderTilVilkårsPerioder(VilkårBuilder vilkårBuilder,
                                                        Map<LocalDate, VilkårData> vurderingsTilDataMap) {
        LocalDate forrigedato = Tid.TIDENES_ENDE;
        final var datoer = vurderingsTilDataMap.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (LocalDate vurderingsdato : datoer) {
            final var vilkårData = vurderingsTilDataMap.get(vurderingsdato);

            final var periodeBuilder = vilkårBuilder.hentBuilderFor(vurderingsdato, forrigedato)
                .medUtfall(vilkårData.getUtfallType())
                .medAvslagsårsak(vilkårData.getAvslagsårsak())
                .medMerknadParametere(vilkårData.getMerknadParametere())
                .medRegelInput(vilkårData.getRegelInput())
                .medRegelEvaluering(vilkårData.getRegelEvaluering());
            Optional.ofNullable(vilkårData.getVilkårUtfallMerknad()).ifPresent(periodeBuilder::medMerknad);
            forrigedato = vurderingsdato.minusDays(1);
            vilkårBuilder.leggTil(periodeBuilder);
        }
        return vilkårBuilder;
    }
}
