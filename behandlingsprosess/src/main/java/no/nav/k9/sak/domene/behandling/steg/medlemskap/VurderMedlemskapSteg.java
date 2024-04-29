package no.nav.k9.sak.domene.behandling.steg.medlemskap;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.medlemskap.VurderLøpendeMedlemskap;
import no.nav.k9.sak.inngangsvilkår.medlemskap.VurdertMedlemskapOgForlengelser;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;

@BehandlingStegRef(value = VURDER_MEDLEMSKAPVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderMedlemskapSteg implements BehandlingSteg {

    private final Logger log = LoggerFactory.getLogger(VurderMedlemskapSteg.class);

    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private ProsessTriggereRepository prosessTriggereRepository;
    private AksjonspunktutlederForMedlemskap aksjonspunktutlederForMedlemskap;

    @Inject
    public VurderMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
                                BehandlingRepositoryProvider provider,
                                @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                ProsessTriggereRepository prosessTriggereRepository,
                                AksjonspunktutlederForMedlemskap aksjonspunktutlederForMedlemskap) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.aksjonspunktutlederForMedlemskap = aksjonspunktutlederForMedlemskap;
    }

    VurderMedlemskapSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        boolean stegMåRekjøres = utledPerioderSomHarBlittLagtTilVurderingPgaEndringIPerioder(kontekst);
        if (stegMåRekjøres) {
            return BehandleStegResultat.tilbakeførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP));
        }
        vurderingMedForlengelse(kontekst);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean utledPerioderSomHarBlittLagtTilVurderingPgaEndringIPerioder(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        final var vilkåreneFørVurdering = vilkårResultatRepository.hent(behandlingId);
        var perioderTilVurdering = tjeneste.utled(behandlingId, VilkårType.MEDLEMSKAPSVILKÅRET);

        var perioderSattTilIkkeVurdert = vilkåreneFørVurdering.getVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> Objects.equals(it.getUtfall(), Utfall.IKKE_VURDERT))
            .map(VilkårPeriode::getPeriode)
            .filter(periode -> perioderTilVurdering.stream().noneMatch(at -> Objects.equals(at, periode)))
            .collect(Collectors.toCollection(TreeSet::new));

        if (perioderSattTilIkkeVurdert.isEmpty()) {
            return false;
        }

        log.info("Fant {} perioder som har tilstand IKKE_VURDERT og ikke ligger til vurdering", perioderSattTilIkkeVurdert.size());
        var triggere = perioderSattTilIkkeVurdert.stream().map(it -> new Trigger(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_MEDLEMSKAP, it)).collect(Collectors.toSet());
        prosessTriggereRepository.leggTil(behandlingId, triggere);

        var aksjonspunktResultats = aksjonspunktutlederForMedlemskap.utledAksjonspunkterFor(new AksjonspunktUtlederInput(referanse));
        // krever at steget kjøre på nytt
        return !aksjonspunktResultats.isEmpty();
    }

    private void vurderingMedForlengelse(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());

        VurdertMedlemskapOgForlengelser vurderingerOgForlengelsesPerioder = vurderLøpendeMedlemskap.vurderMedlemskapOgHåndterForlengelse(behandlingId);

        final var vilkåreneFørVurdering = vilkårResultatRepository.hent(behandlingId);
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkåreneFørVurdering);

        final var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET)
            .medMaksMellomliggendePeriodeAvstand(tjeneste.maksMellomliggendePeriodeAvstand())
            .medKantIKantVurderer(tjeneste.getKantIKantVurderer());

        Optional<Vilkår> utgangspunkt = hentUtgangspunkt(referanse);

        mapPerioderTilVilkårsPerioderMedForlengelse(vilkårBuilder, utgangspunkt, vurderingerOgForlengelsesPerioder);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        final var nyttResultat = vilkårResultatBuilder.build();

        validerAtAltErVurdert(nyttResultat);

        vilkårResultatRepository.lagre(behandlingId, nyttResultat);
    }

    private void validerAtAltErVurdert(Vilkårene nyttResultat) {
        var vilkår = nyttResultat.getVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).orElseThrow();

        var perioderUtenVurdering = vilkår.getPerioder().stream()
            .filter(periode -> Objects.equals(periode.getGjeldendeUtfall(), Utfall.IKKE_VURDERT))
            .map(VilkårPeriode::getPeriode)
            .toList();
        if (!perioderUtenVurdering.isEmpty()) {
            throw new IllegalStateException("Har perioder som ikke er vurdert etter vurdering: " + perioderUtenVurdering);
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

    VilkårBuilder mapPerioderTilVilkårsPerioderMedForlengelse(VilkårBuilder vilkårBuilder,
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
        var datoer = new TreeSet<>(vurderingsTilDataMap
            .keySet());

        for (LocalDate vurderingsdato : datoer) {
            final var vilkårData = vurderingsTilDataMap.get(vurderingsdato);

            var periodeBuilder = vilkårBuilder.hentBuilderFor(vilkårData.getPeriode())
                .medUtfall(vilkårData.getUtfallType())
                .medAvslagsårsak(vilkårData.getAvslagsårsak())
                .medMerknadParametere(vilkårData.getMerknadParametere())
                .medRegelInput(vilkårData.getRegelInput())
                .medRegelEvaluering(vilkårData.getRegelEvaluering());
            Optional.ofNullable(vilkårData.getVilkårUtfallMerknad()).ifPresent(periodeBuilder::medMerknad);
            vilkårBuilder.leggTil(periodeBuilder);
        }
        return vilkårBuilder;
    }
}
