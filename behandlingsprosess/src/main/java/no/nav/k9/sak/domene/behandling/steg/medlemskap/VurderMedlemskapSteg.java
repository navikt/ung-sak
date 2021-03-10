package no.nav.k9.sak.domene.behandling.steg.medlemskap;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.medlemskap.VurderLøpendeMedlemskap;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;

@BehandlingStegRef(kode = "VURDERMV")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderMedlemskapSteg implements BehandlingSteg {

    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public VurderMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
                                BehandlingRepositoryProvider provider) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
    }

    VurderMedlemskapSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        // FIXME K9 : Skrive om til å vurdere de periodene som vilkåret er brutt opp på.
        Map<LocalDate, VilkårData> vurderingsTilDataMap = vurderLøpendeMedlemskap.vurderMedlemskap(behandlingId);
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
        var datoer = vurderingsTilDataMap.keySet()
            .stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        var forrigedato = utledForrigeDato(vilkårBuilder, datoer);
        for (LocalDate vurderingsdato : datoer) {
            final var vilkårData = vurderingsTilDataMap.get(vurderingsdato);

            var periodeBuilder = vilkårBuilder.hentBuilderFor(vurderingsdato, forrigedato)
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

    private LocalDate utledForrigeDato(VilkårBuilder vilkårBuilder, List<LocalDate> datoer) {
        var forrigedato = vilkårBuilder.getMaxDatoTilVurdering();
        if (datoer.isEmpty()) {
            return forrigedato;
        }
        var størstedato = datoer.get(0);

        if (størstedato != null && forrigedato.isBefore(størstedato)) {
            return Tid.TIDENES_ENDE;
        }
        return forrigedato;
    }
}
