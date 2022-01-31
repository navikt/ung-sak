package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.InaktivitetUtlederInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.PerioderMedInaktivitetUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie.MapFerie;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn.MapTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.utenlandsopphold.MapUtenlandsopphold;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak.MapUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov;
import no.nav.pleiepengerbarn.uttak.kontrakter.RettVedDød;
import no.nav.pleiepengerbarn.uttak.kontrakter.Søker;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode;
import no.nav.pleiepengerbarn.uttak.kontrakter.YtelseType;

@Dependent
public class MapInputTilUttakTjeneste {

    private HentDataTilUttakTjeneste hentDataTilUttakTjeneste;
    private String unntak;
    private Boolean utenlandsperioderMappingEnablet;
    private Boolean utvidVedDødsfall;


    @Inject
    public MapInputTilUttakTjeneste(HentDataTilUttakTjeneste hentDataTilUttakTjeneste,
                                    @KonfigVerdi(value = "psb.uttak.unntak.aktiviteter", required = false, defaultVerdi = "") String unntak,
                                    @KonfigVerdi(value = "UTENLANDSPERIODER_MAPPING_ENABLET", defaultVerdi = "false") Boolean utenlandsperioderMappingEnablet,
                                    @KonfigVerdi(value = "PSB_UTVIDE_VED_DODSFALL", defaultVerdi = "false") Boolean utvidVedDødsfall) {
        this.hentDataTilUttakTjeneste = hentDataTilUttakTjeneste;
        this.unntak = unntak;
        this.utenlandsperioderMappingEnablet = utenlandsperioderMappingEnablet;
        this.utvidVedDødsfall = utvidVedDødsfall;
    }


    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        return toRequestData(hentDataTilUttakTjeneste.hentUtData(referanse, false));
    }

    public Uttaksgrunnlag hentUtUbesluttededataOgMapRequest(BehandlingReferanse referanse) {
        return toRequestData(hentDataTilUttakTjeneste.hentUtData(referanse, true));
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

        final List<SøktUttak> søktUttak = new MapUttak().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering, input.getUtvidetPeriodeSomFølgeAvDødsfall());

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
            .medSakerSomMåSpesialHåndteres(MapUnntakFraAktivitetGenerering.mapUnntak(unntak));

        if (utvidVedDødsfall) {
            input.getUtvidetPeriodeSomFølgeAvDødsfall().ifPresent(arbeidstidInput::medAutomatiskUtvidelseVedDødsfall);
        }

        final List<Arbeid> arbeid = new MapArbeid().map(arbeidstidInput);

        final Map<LukketPeriode, Pleiebehov> pleiebehov = toPleiebehov(input.getPleiebehov());

        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(vurderteSøknadsperioder, perioderFraSøknader, tidslinjeTilVurdering);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(input.getVilkårene());

        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(input.getEtablertTilsynPerioder());

        var innvilgedePerioderMedSykdom = finnInnvilgedePerioderSykdom(input.getVilkårene(), input.getDefinerendeVilkårtyper());

        var unntakEtablertTilsynForPleietrengende = input.getUnntakEtablertTilsynForPleietrengende().orElse(null);
        var beredskapsperioder = tilBeredskap(unntakEtablertTilsynForPleietrengende, innvilgedePerioderMedSykdom);
        var nattevåksperioder = tilNattevåk(unntakEtablertTilsynForPleietrengende, innvilgedePerioderMedSykdom);
        final Map<LukketPeriode, List<String>> kravprioritet = mapKravprioritetsliste(input.getKravprioritet());
        final List<LukketPeriode> perioderSomSkalTilbakestilles = input.getPerioderSomSkalTilbakestilles().stream().map(p -> new LukketPeriode(p.getFomDato(), p.getTomDato())).toList();
        Map<LukketPeriode, UtenlandsoppholdInfo> utenlandsoppholdperioder;
        if (utenlandsperioderMappingEnablet) {
            utenlandsoppholdperioder = new MapUtenlandsopphold().map(vurderteSøknadsperioder, perioderFraSøknader, tidslinjeTilVurdering);
        } else {
            utenlandsoppholdperioder = Map.of();
        }


        return new Uttaksgrunnlag(
            mapTilYtelseType(behandling),
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
            tilsynsperioder,
            beredskapsperioder,
            nattevåksperioder,
            kravprioritet,
            utenlandsoppholdperioder
        );
    }

    private YtelseType mapTilYtelseType(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PSB;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLS;
            default -> throw new IllegalStateException("Ikke støttet ytelse for uttak Pleiepenger: " + behandling.getFagsakYtelseType());
        };
    }

    private Set<DatoIntervallEntitet> finnInnvilgedePerioderSykdom(Vilkårene vilkårene, Set<VilkårType> definerendeVilkårtyper) {
        final var resultat = new TreeSet<DatoIntervallEntitet>();
        for (VilkårType vilkårType : definerendeVilkårtyper) {
            var innvilgedePerioder = vilkårene.getVilkår(vilkårType).orElseThrow().getPerioder()
                .stream()
                .filter(it -> no.nav.k9.kodeverk.vilkår.Utfall.OPPFYLT.equals(it.getUtfall()))
                .map(VilkårPeriode::getPeriode)
                .collect(Collectors.toSet());
            resultat.addAll(innvilgedePerioder);
        }
        return resultat;
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

    private Map<LukketPeriode, Utfall> tilBeredskap(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        if (unntakEtablertTilsynForPleietrengende == null || unntakEtablertTilsynForPleietrengende.getBeredskap() == null) {
            return Map.of();
        }
        return tilUnntakEtablertTilsynMap(unntakEtablertTilsynForPleietrengende.getBeredskap(), innvilgedePerioderMedSykdom);
    }

    private Map<LukketPeriode, Utfall> tilNattevåk(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        if (unntakEtablertTilsynForPleietrengende == null || unntakEtablertTilsynForPleietrengende.getNattevåk() == null) {
            return Map.of();
        }
        return tilUnntakEtablertTilsynMap(unntakEtablertTilsynForPleietrengende.getNattevåk(), innvilgedePerioderMedSykdom);
    }

    private Map<LukketPeriode, Utfall> tilUnntakEtablertTilsynMap(UnntakEtablertTilsyn unntakEtablertTilsyn, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        var map = new HashMap<LukketPeriode, Utfall>();
        unntakEtablertTilsyn.getPerioder()
            .stream()
            .filter(it -> erRelevantForBehandling(it, innvilgedePerioderMedSykdom))
            .forEach(periode -> {
                    var utfall = switch (periode.getResultat()) {
                        case OPPFYLT -> Utfall.OPPFYLT;
                        case IKKE_OPPFYLT -> Utfall.IKKE_OPPFYLT;
                        case IKKE_VURDERT -> throw new IllegalStateException("Skal ikke komme perioder som ikke er vurdert til uttak.");
                    };
                    map.put(new LukketPeriode(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato()), utfall);
                }
            );
        return map;
    }

    private boolean erRelevantForBehandling(UnntakEtablertTilsynPeriode it, Set<DatoIntervallEntitet> innvilgedePerioderMedSykdom) {
        return Set.of(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT).contains(it.getResultat())
            || innvilgedePerioderMedSykdom.stream().anyMatch(at -> at.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFomDato(), it.getPeriode().getTomDato())));
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

    private Map<LukketPeriode, Pleiebehov> toPleiebehov(List<EtablertPleieperiode> pleiebehov) {
        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = new HashMap<>();
        pleiebehov.forEach(p -> tilsynsbehov.put(toLukketPeriode(p.getPeriode()), mapToPleiebehov(p.getGrad())));
        return tilsynsbehov;
    }

    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }

    private Pleiebehov mapToPleiebehov(Pleiegrad grad) {
        return switch (grad) {
            case INGEN -> Pleiebehov.PROSENT_0;
            case KONTINUERLIG_TILSYN -> Pleiebehov.PROSENT_100;
            case UTVIDET_KONTINUERLIG_TILSYN, INNLEGGELSE -> Pleiebehov.PROSENT_200;
            case LIVETS_SLUTT_TILSYN -> Pleiebehov.PROSENT_6000;
            default -> throw new IllegalStateException("Ukjent Pleiegrad: " + grad);
        };
    }

    private HashMap<String, List<Vilkårsperiode>> toInngangsvilkår(Vilkårene vilkårene) {
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = new HashMap<>();
        vilkårene.getVilkårene().forEach(v -> {
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
