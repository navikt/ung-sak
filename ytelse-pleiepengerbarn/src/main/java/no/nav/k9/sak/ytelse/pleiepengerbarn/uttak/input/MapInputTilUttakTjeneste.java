package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie.MapFerie;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn.MapTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak.MapUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov;
import no.nav.pleiepengerbarn.uttak.kontrakter.Søker;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode;

@Dependent
public class MapInputTilUttakTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(referanse, referanse.getFagsakPeriode().getFomDato());
        var pleiebehov = pleiebehovResultatRepository.hent(referanse.getBehandlingId());
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);

        return toRequestData(behandling, personopplysningerAggregat, vurderteSøknadsperioder, vilkårene, uttakGrunnlag, pleiebehov);
    }

    private Uttaksgrunnlag toRequestData(Behandling behandling,
                                         PersonopplysningerAggregat personopplysningerAggregat,
                                         Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder,
                                         Vilkårene vilkårene,
                                         UttaksPerioderGrunnlag uttaksPerioderGrunnlag,
                                         PleiebehovResultat pleiebehovResultat) {

        var perioderFraSøknader = uttaksPerioderGrunnlag.getRelevantSøknadsperioder()
            .getUttakPerioder();
        var kravDokumenter = vurderteSøknadsperioder.keySet()
            .stream()
            .filter(it -> perioderFraSøknader.stream().anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())))
            .collect(Collectors.toCollection(TreeSet::new));
        var søkerPersonopplysninger = personopplysningerAggregat.getSøker();
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());

        var barn = new Barn(pleietrengendePersonopplysninger.getAktørId().getId(), pleietrengendePersonopplysninger.getDødsdato());
        var søker = new Søker(søkerPersonopplysninger.getAktørId().getId(), søkerPersonopplysninger.getFødselsdato(), søkerPersonopplysninger.getDødsdato());

        // TODO: Map:
        final List<String> andrePartersSaksnummer = List.of();

        final List<SøktUttak> søktUttak = new MapUttak().map(kravDokumenter, perioderFraSøknader);

        // TODO: Se kommentarer/TODOs under denne:
        final List<Arbeid> arbeid = new MapArbeid().map(kravDokumenter, perioderFraSøknader);

        final Map<LukketPeriode, Pleiebehov> pleiebehov = toPleiebehov(pleiebehovResultat);

        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(kravDokumenter, perioderFraSøknader);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(vilkårene);

        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(kravDokumenter, perioderFraSøknader);

        return new Uttaksgrunnlag(
            barn,
            søker,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            behandling.getUuid().toString(),
            andrePartersSaksnummer,
            søktUttak,
            arbeid,
            pleiebehov,
            lovbestemtFerie,
            inngangsvilkår,
            tilsynsperioder);
    }

    private Map<LukketPeriode, Pleiebehov> toPleiebehov(PleiebehovResultat pleiebehov) {
        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = new HashMap<>();
        pleiebehov.getPleieperioder().getPerioder().forEach(p -> {
            tilsynsbehov.put(toLukketPeriode(p.getPeriode()), mapToPleiebehov(p.getGrad()));
        });
        return tilsynsbehov;
    }

    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }

    private Pleiebehov mapToPleiebehov(Pleiegrad grad) {
        switch (grad) {
            case INGEN:
                return Pleiebehov.PROSENT_0;
            case KONTINUERLIG_TILSYN:
                return Pleiebehov.PROSENT_100;
            case UTVIDET_KONTINUERLIG_TILSYN:
            case INNLEGGELSE:
                return Pleiebehov.PROSENT_200;
            default:
                throw new IllegalStateException("Ukjent Pleiegrad: " + grad);
        }
    }

    private HashMap<String, List<Vilkårsperiode>> toInngangsvilkår(Vilkårene vilkårene) {
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = new HashMap<>();
        vilkårene.getVilkårene().forEach(v -> {
            if (v.getVilkårType() == VilkårType.BEREGNINGSGRUNNLAGVILKÅR) {
                return;
            }
            final List<Vilkårsperiode> vilkårsperioder = v.getPerioder()
                .stream()
                .map(vp -> new Vilkårsperiode(new LukketPeriode(vp.getFom(), vp.getTom()), Utfall.valueOf(vp.getUtfall().getKode())))
                .collect(Collectors.toList());
            inngangsvilkår.put(v.getVilkårType().getKode(), vilkårsperioder);
        });
        return inngangsvilkår;
    }

}
