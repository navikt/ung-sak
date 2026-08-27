package no.nav.ung.ytelse.aktivitetspenger.del1.steg.aktivitetsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.time.LocalDateTime;
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
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    VurderAktivitetsvilkårSteg() {
        //for CDI proxy
    }

    @Inject
    public VurderAktivitetsvilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                      VilkårResultatRepository vilkårResultatRepository1,
                                      InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                      InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository1;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
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

        List<AktivitetsvilkårResultatPeriode> automatiskVurdering = tidslinjeTilVurdering.getLocalDateIntervals().stream()
            .map(periode -> new AktivitetsvilkårResultatPeriode(
                DatoIntervallEntitet.fra(periode),
                true,
                null,
                false,
                null,
                null,
                null,
                LocalDateTime.now()
            ))
            .toList();

        var vilkåreneOptional = vilkårResultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårBuilder = vilkåreneOptional.map(Vilkårene::builderFraEksisterende).orElse(Vilkårene.builder());

        inngangsvilkårVurderingRepository.lagreAktivitetVurderinger(kontekst.getBehandlingId(), automatiskVurdering);
        inngangsvilkårVurderingTjeneste.settAktivitetsvilkårResultat(kontekst.getBehandlingId(), vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårBuilder.build());
    }

}
