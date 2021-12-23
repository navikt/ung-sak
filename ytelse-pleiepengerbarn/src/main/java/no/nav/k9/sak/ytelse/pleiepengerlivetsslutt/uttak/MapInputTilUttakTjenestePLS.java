package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.uttak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.HentDataTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.InputParametere;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.InaktivitetUtlederInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.PerioderMedInaktivitetUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie.MapFerie;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak.MapUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov;
import no.nav.pleiepengerbarn.uttak.kontrakter.RettVedDød;
import no.nav.pleiepengerbarn.uttak.kontrakter.Søker;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode;
import no.nav.pleiepengerbarn.uttak.kontrakter.YtelseType;

@Dependent
public class MapInputTilUttakTjenestePLS {

    private HentDataTilUttakTjeneste hentDataTilUttakTjeneste;


    @Inject
    public MapInputTilUttakTjenestePLS(HentDataTilUttakTjeneste hentDataTilUttakTjeneste) {
        this.hentDataTilUttakTjeneste = hentDataTilUttakTjeneste;
    }

    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        return toRequestData(hentDataTilUttakTjeneste.hentUtData(referanse, false));
    }

    private Uttaksgrunnlag toRequestData(InputParametere input) {

        var behandling = input.getBehandling();
        var vurderteSøknadsperioder = input.getVurderteSøknadsperioder();
        var personopplysningerAggregat = input.getPersonopplysningerAggregat();

        // Henter ut alt og lager tidlinje av denne for så å ta ut den delen som er relevant
        // NB! Kan gi issues ved lange fagsaker mtp ytelse
        var perioderFraSøknader = input.getPerioderFraSøknad();
        var kravDokumenter = vurderteSøknadsperioder.keySet();

        evaluerDokumenter(perioderFraSøknader, kravDokumenter);

        var søkerPersonopplysninger = personopplysningerAggregat.getSøker();
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());

        RettVedDød rettVedDød = utledRettVedDød(input);
        var barn = new Barn(pleietrengendePersonopplysninger.getAktørId().getId(), pleietrengendePersonopplysninger.getDødsdato(), rettVedDød);

        var søker = new Søker(søkerPersonopplysninger.getAktørId().getId());

        var tidslinjeTilVurdering = new LocalDateTimeline<>(mapPerioderTilVurdering(input));

        final List<SøktUttak> søktUttak = new MapUttak().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        var inaktivitetUtlederInput = new InaktivitetUtlederInput(behandling.getAktørId(), tidslinjeTilVurdering, input.getInntektArbeidYtelseGrunnlag());
        var inaktivTidslinje = new PerioderMedInaktivitetUtleder().utled(inaktivitetUtlederInput);

        var arbeidstidInput = new ArbeidstidMappingInput()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medKravDokumenter(kravDokumenter)
            .medPerioderFraSøknader(perioderFraSøknader)
            .medTidslinjeTilVurdering(tidslinjeTilVurdering)
            .medVilkår(input.getVilkårene().getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow())
            .medOpptjeningsResultat(input.getOpptjeningResultat().orElse(null))
            .medInaktivTidslinje(inaktivTidslinje)
            .medInntektArbeidYtelseGrunnlag(input.getInntektArbeidYtelseGrunnlag())
            .medBruker(behandling.getAktørId())
            .medSakerSomMåSpesialHåndteres(Map.of());

        final List<Arbeid> arbeid = new MapArbeid().map(arbeidstidInput);


        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(vurderteSøknadsperioder, perioderFraSøknader, tidslinjeTilVurdering);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(input.getVilkårene());

        var innvilgedePerioderMedSykdom = finnInnvilgedePerioderSykdom(input.getVilkårene());

        var pleiebehov = innvilgedePerioderMedSykdom.stream()
            .collect(Collectors.toMap(this::toLukketPeriode, e -> Pleiebehov.PROSENT_6000));

        final Map<LukketPeriode, List<String>> kravprioritet = mapKravprioritetsliste(input.getKravprioritet());
        final List<LukketPeriode> perioderSomSkalTilbakestilles = input.getPerioderSomSkalTilbakestilles().stream().map(p -> new LukketPeriode(p.getFomDato(), p.getTomDato())).toList();

        return new Uttaksgrunnlag(
            YtelseType.PLS,
            barn,
            søker,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            behandling.getUuid().toString(),
            søktUttak,
            perioderSomSkalTilbakestilles,
            arbeid,
            pleiebehov,
            lovbestemtFerie,
            inngangsvilkår,
            Map.of(), // tilsynsperioder
            Map.of(), // beredskapsperioder
            Map.of(), // nattevåksperioder
            kravprioritet
        );
    }

    private Set<DatoIntervallEntitet> finnInnvilgedePerioderSykdom(Vilkårene vilkårene) {
        return vilkårene.getVilkår(VilkårType.I_LIVETS_SLUTTFASE).orElseThrow()
            .getPerioder()
            .stream()
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());
    }

    private RettVedDød utledRettVedDød(InputParametere input) {
        RettVedDød rettVedDød = null;
        if (input.getRettPleiepengerVedDødGrunnlag().isPresent()) {
            rettVedDød = switch (input.getRettPleiepengerVedDødGrunnlag().get().getRettVedPleietrengendeDød().getRettVedDødType()) {
                case RETT_6_UKER -> RettVedDød.RETT_6_UKER;
                case RETT_12_UKER -> RettVedDød.RETT_12_UKER;
            };
        }
        return rettVedDød;
    }

    public Map<LukketPeriode, List<String>> mapKravprioritetsliste(LocalDateTimeline<List<Kravprioritet>> kravprioritet) {
        final Map<LukketPeriode, List<String>> resultat = new HashMap<>();
        kravprioritet.forEach(s -> {
            resultat.put(new LukketPeriode(s.getFom(), s.getTom()), s.getValue().stream().map(kp -> kp.getAktuellBehandlingUuid().toString()).collect(Collectors.toList()));
        });
        return resultat;
    }

    private void evaluerDokumenter(Set<PerioderFraSøknad> perioderFraSøknader, Set<KravDokument> kravDokumenter) {
        var journalpostIds = perioderFraSøknader.stream().map(PerioderFraSøknad::getJournalpostId).collect(Collectors.toSet());

        var relevanteKravdokumenter = kravDokumenter.stream().map(KravDokument::getJournalpostId).filter(journalpostIds::contains).collect(Collectors.toSet());

        if (journalpostIds.size() != relevanteKravdokumenter.size()) {
            throw new IllegalStateException("Fant ikke alle dokumentene siden '" + journalpostIds + "' != '" + relevanteKravdokumenter + "'");
        }
    }

    private List<LocalDateSegment<Boolean>> mapPerioderTilVurdering(InputParametere input) {

        var timeline = new LocalDateTimeline<>(input.getPerioderTilVurdering()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));

        return new ArrayList<>(timeline.compress().toSegments());
    }

    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }

    private HashMap<String, List<Vilkårsperiode>> toInngangsvilkår(Vilkårene vilkårene) {
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = new HashMap<>();
        vilkårene.getVilkårene().forEach(v -> {
            if (v.getVilkårType() == VilkårType.BEREGNINGSGRUNNLAGVILKÅR) {
                return;
            }
            final List<Vilkårsperiode> vilkårsperioder = v.getPerioder()
                .stream()
                .map(vp -> new Vilkårsperiode(new LukketPeriode(vp.getFom(), vp.getTom()), mapUtfall(v.getVilkårType(), vp)))
                .collect(Collectors.toList());
            inngangsvilkår.put(v.getVilkårType().getKode(), vilkårsperioder);
        });
        return inngangsvilkår;
    }

    private Utfall mapUtfall(VilkårType vilkårType, VilkårPeriode vp) {
        if (Arrays.stream(Utfall.values()).noneMatch(it -> it.name().equals(vp.getGjeldendeUtfall().getKode()))) {
            throw new IllegalStateException("Vilkårsperiode med ikke supportert utfall '" + vp.getGjeldendeUtfall() + "', vilkår='" + vilkårType + "', periode='" + vp.getPeriode() + "'");
        }
        return Utfall.valueOf(vp.getGjeldendeUtfall().getKode());
    }

}
