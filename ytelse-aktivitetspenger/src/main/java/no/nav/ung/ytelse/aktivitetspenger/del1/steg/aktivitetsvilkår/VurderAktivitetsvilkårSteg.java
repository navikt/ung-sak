package no.nav.ung.ytelse.aktivitetspenger.del1.steg.aktivitetsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
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

    VurderAktivitetsvilkårSteg() {
        //for CDI proxy
    }

    @Inject
    public VurderAktivitetsvilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
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
        boolean erFørstegangsbehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getType() == BehandlingType.FØRSTEGANGSSØKNAD;
        if (erFørstegangsbehandling) {
            automatiskInnvilgAktivitetsvilkåret(kontekst);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = finnPerioderSomSkalVurderes(kontekst);
        if (!tidslinjeTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    private void automatiskInnvilgAktivitetsvilkåret(BehandlingskontrollKontekst kontekst) {
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = finnPerioderSomSkalVurderes(kontekst);
        Avslagsårsak avslagsårsak = null; //ingen avslagsårsak indikerer innvilgelse
        tidslinjeTilVurdering.getLocalDateIntervals().forEach(periode -> vilkårTjeneste.lagreVilkårresultat(kontekst, VilkårType.AKTIVITETSVILKÅR, DatoIntervallEntitet.fra(periode), avslagsårsak));
    }

}
