package no.nav.k9.sak.domene.behandling.steg.medlemskap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.medlemskap.VurderLøpendeMedlemskap;
import no.nav.k9.sak.inngangsvilkår.medlemskap.VurdertMedlemskapOgForlengelser;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(kode = "VURDERMV")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderMedlemskapSteg implements BehandlingSteg {

    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private Boolean enableForlengelse;

    @Inject
    public VurderMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
                                BehandlingRepositoryProvider provider,
                                @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                @KonfigVerdi(value = "forlengelse.medlemskap.enablet", defaultVerdi = "false") Boolean enableForlengelse) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.enableForlengelse = enableForlengelse;
    }

    VurderMedlemskapSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        if (enableForlengelse) {
            vurderingMedForlengelse(kontekst);
        } else {
            legacyVurderingUtenStøtteForForlengelse(behandlingId);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void vurderingMedForlengelse(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());

        var vurderingerOgForlengelsesPerioder = vurderLøpendeMedlemskap.vurderMedlemskapOgHåndterForlengelse(behandlingId);

        final var vilkåreneFørVurdering = vilkårResultatRepository.hent(behandlingId);
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkåreneFørVurdering);

        final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
            .medKantIKantVurderer(tjeneste.getKantIKantVurderer());

        var utgangspunkt = hentUtgangspunkt(referanse);

        mapPerioderTilVilkårsPerioderMedForlengelse(vilkårBuilder, utgangspunkt, vurderingerOgForlengelsesPerioder);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        final var nyttResultat = vilkårResultatBuilder.build();

        validerAtAltErVurdert(nyttResultat);

        vilkårResultatRepository.lagre(behandlingId, nyttResultat);
    }

    private void validerAtAltErVurdert(Vilkårene nyttResultat) {
        var vilkår = nyttResultat.getVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).orElseThrow();

        if (vilkår.getPerioder().stream().anyMatch(periode -> Objects.equals(periode.getGjeldendeUtfall(), Utfall.IKKE_VURDERT))) {
            throw new IllegalStateException("Har perioder som ikke er vurdert etter vurdering.");
        }
    }

    private Optional<Vilkår> hentUtgangspunkt(BehandlingReferanse referanse) {
        if (referanse.getOriginalBehandlingId().isPresent()) {
            var originalBehandling = referanse.getOriginalBehandlingId().get();

            var originalBehandlingResultat = vilkårResultatRepository.hent(originalBehandling);
            return originalBehandlingResultat.getVilkår(VilkårType.MEDLEMSKAPSVILKÅRET);
        }
        return Optional.empty();
    }

    private void legacyVurderingUtenStøtteForForlengelse(Long behandlingId) {
        // FIXME K9 : Skrive om til å vurdere de periodene som vilkåret er brutt opp på.
        Map<LocalDate, VilkårData> vurderingsTilDataMap = vurderLøpendeMedlemskap.vurderMedlemskap(behandlingId);
        if (!vurderingsTilDataMap.isEmpty()) {
            final var vilkåreneFørVurdering = vilkårResultatRepository.hent(behandlingId);
            VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkåreneFørVurdering);

            final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
            mapPerioderTilVilkårsPerioder(vilkårBuilder, vurderingsTilDataMap);
            vilkårResultatBuilder.leggTil(vilkårBuilder);
            final var nyttResultat = vilkårResultatBuilder.build();
            vilkårResultatRepository.lagre(behandlingId, nyttResultat);
        }
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

    private VilkårBuilder mapPerioderTilVilkårsPerioderMedForlengelse(VilkårBuilder vilkårBuilder,
                                                                      Optional<Vilkår> utgangspunkt,
                                                                      VurdertMedlemskapOgForlengelser vurderinger) {

        var forlengelser = vurderinger.getForlengelser();

        for (DatoIntervallEntitet periode : forlengelser) {
            var forlengelseFra = utgangspunkt.orElseThrow()
                .finnPeriodeForSkjæringstidspunkt(periode.getFomDato());

            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode)
                .forlengelseAv(forlengelseFra));
        }

        var vurderingsTilDataMap = vurderinger.getVurderinger();
        var datoer = vurderingsTilDataMap
            .keySet()
            .stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        var forrigedato = utledForrigeDato(vilkårBuilder, datoer, forlengelser);
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

    private LocalDate utledForrigeDato(VilkårBuilder vilkårBuilder, List<LocalDate> datoer, NavigableSet<DatoIntervallEntitet> forlengelser) {
        var forrigedato = vilkårBuilder.getMaxDatoTilVurdering();
        if (datoer.isEmpty() && forlengelser.isEmpty()) {
            return forrigedato;
        }
        var relevanteDatoer = new ArrayList<LocalDate>();
        if (!datoer.isEmpty()) {
            relevanteDatoer.add(datoer.get(0));
        }
        forlengelser.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .ifPresent(relevanteDatoer::add);

        var størstedato = relevanteDatoer.stream()
            .max(LocalDate::compareTo)
            .orElse(forrigedato);

        if (forrigedato.isBefore(størstedato)) {
            return Tid.TIDENES_ENDE;
        }
        return forrigedato;
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
