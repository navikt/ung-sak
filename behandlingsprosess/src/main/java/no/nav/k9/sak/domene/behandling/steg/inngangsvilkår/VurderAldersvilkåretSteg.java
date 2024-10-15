package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.ALDERSVILKÅRET;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder.VurderAldersVilkårTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@BehandlingStegRef(value = ALDERSVILKÅRET)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class VurderAldersvilkåretSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderAldersvilkåretSteg.class);

    private final VurderAldersVilkårTjeneste vurderAldersVilkårTjeneste = new VurderAldersVilkårTjeneste();
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    VurderAldersvilkåretSteg() {
        // for proxy
    }

    @Inject
    public VurderAldersvilkåretSteg(@Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                    BehandlingRepository behandlingRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.ALDERSVILKÅR);

        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), behandling.getFagsak().getPeriode().getFomDato());
        LocalDate fødselsdato = personopplysningerAggregat.getSøker().getFødselsdato();

        Vilkårene resultat = vurderVilkår(behandling, vilkårene, perioderTilVurdering, fødselsdato);

        var perioderUtenVurdering = finnPerioderUtenVurdering(resultat);
        if (!perioderUtenVurdering.isEmpty()) {
            log.warn("Hadde perioder uten vurdering etter vurdering av perioder til vurdering: {}", perioderUtenVurdering);
            if (behandling.getId() == 1828755L) { //EAWVS //TODO fjern når vi ser om det funker for denne behandlingen
                resultat = vurderVilkår(behandling, resultat, perioderUtenVurdering, fødselsdato);
            }
        }

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), resultat);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    private static NavigableSet<DatoIntervallEntitet> finnPerioderUtenVurdering(Vilkårene resultat) {
        return resultat.getVilkår(VilkårType.ALDERSVILKÅR).orElseThrow().getPerioder()
            .stream()
            .filter(vilkårPeriode -> vilkårPeriode.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private Vilkårene vurderVilkår(Behandling behandling, Vilkårene eksisterende, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato) {
        var resultatBuilder = Vilkårene.builderFraEksisterende(eksisterende);
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ALDERSVILKÅR);
        vurderAldersVilkårTjeneste.vurderPerioder(vilkårBuilder, perioderTilVurdering, fødselsdato);
        resultatBuilder.leggTil(vilkårBuilder);
        var resultat = resultatBuilder.build();
        vilkårResultatRepository.lagre(behandling.getId(), resultat);
        return resultat;
    }
}
