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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.VurderLøpendeMedlemskap;
import no.nav.vedtak.konfig.Tid;

@BehandlingStegRef(kode = "VULOMED")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderLøpendeMedlemskapSteg implements BehandlingSteg {

    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public VurderLøpendeMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
                                       BehandlingRepositoryProvider provider) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
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
            final var vilkåreneFørVurdering = vilkårResultatRepository.hent(behandlingId);
            VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkåreneFørVurdering);

            final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            mapPerioderTilVilkårsPerioder(vilkårBuilder, vurderingsTilDataMap);
            vilkårResultatBuilder.leggTil(vilkårBuilder);
            BehandlingLås lås = kontekst.getSkriveLås();
            final var nyttResultat = vilkårResultatBuilder.build();
            vilkårResultatRepository.lagre(behandlingId, nyttResultat);

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
