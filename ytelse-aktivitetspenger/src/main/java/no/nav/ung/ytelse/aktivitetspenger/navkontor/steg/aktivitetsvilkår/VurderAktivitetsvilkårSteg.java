package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_AKTIVITETSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_AKTIVITETSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderAktivitetsvilkårSteg extends VilkårVurderingSteg {

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderAktivitetsvilkårSteg() {
        //for CDI proxy
    }

    @Inject
    public VurderAktivitetsvilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                      VilkårResultatRepository vilkårResultatRepository1) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository1;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.AKTIVITETSVILKÅR;
    }

    @Override
    public Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        EnumSet<VilkårType> avhengigheter = EnumSet.noneOf(VilkårType.class);
        avhengigheter.add(VilkårType.ALDERSVILKÅR);
        avhengigheter.add(VilkårType.SØKNADSFRIST);
        avhengigheter.addAll(manuelleVilkårRekkefølgeTjeneste.finnManuelleVilkårSomErFør(getAktuellVilkårType(), ytelseType, behandlingType));
        return avhengigheter;
    }

    @Override
    public BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst) {
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = finnPerioderSomSkalVurderes(kontekst);
        if (førstegangsbehandlingEllerForrigeErAvslåttFørstegangsbehandling(kontekst.getBehandlingId())) {
            automatiskMarkereAktivitetsvilkåretIkkeRelevant(kontekst.getBehandlingId(), tidslinjeTilVurdering);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (!tidslinjeTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    private boolean førstegangsbehandlingEllerForrigeErAvslåttFørstegangsbehandling(long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.getType() == BehandlingType.FØRSTEGANGSSØKNAD) {
            return true;
        }

        var originalBehandlingId = behandling.getOriginalBehandlingId();
        if (originalBehandlingId.isEmpty()) {
            return false;
        }

        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId.get());
        if (originalBehandling.getType() != BehandlingType.FØRSTEGANGSSØKNAD) {
            return false;
        }

        return vilkårTjeneste.samletVilkårsresultat(originalBehandling.getId())
            .filterValue(utfall -> utfall.getSamletUtfall() == Utfall.OPPFYLT)
            .isEmpty();
    }

    private void automatiskMarkereAktivitetsvilkåretIkkeRelevant(Long behandlingId, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var vilkåreneOptional = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        VilkårResultatBuilder vilkårResultatBuilder = vilkåreneOptional.map(Vilkårene::builderFraEksisterende).orElse(Vilkårene.builder());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.AKTIVITETSVILKÅR);

        tidslinjeTilVurdering.segmenter()
            .forEach(segment -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(segment.getFom(), segment.getTom())
                .medUtfall(Utfall.IKKE_RELEVANT)
                .medBegrunnelse("Automatisk vurdering: Ingen vurdering av aktivitetsvilkår for ny søkt periode")));

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }
}
