package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakUtbetalingsgrad;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PleietrengendeKravprioritet.Kravprioritet;
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
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.Inntektsgradering;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.OverstyrtInput;
import no.nav.pleiepengerbarn.uttak.kontrakter.OverstyrtUtbetalingsgradPerArbeidsforhold;
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

    private static final String SPEILE_SAK_SOM_HAR_BLITT_FEIL = "BiJUC";


    private final HentDataTilUttakTjeneste hentDataTilUttakTjeneste;
    private final String unntak;
    private final boolean skalKjøreNyLogikkForSpeiling;
    private final boolean skalHaInaktivVed847B;
    private final boolean nyUtledningAvUtenlandsopphold;

    @Inject
    public MapInputTilUttakTjeneste(HentDataTilUttakTjeneste hentDataTilUttakTjeneste,
                                    @KonfigVerdi(value = "psb.uttak.unntak.aktiviteter", required = false, defaultVerdi = "") String unntak,
                                    @KonfigVerdi(value = "IKKE_YRKESAKTIV_UTEN_SPEILING", required = false, defaultVerdi = "false") boolean skalKjøreNyLogikkForSpeiling,
                                    @KonfigVerdi(value = "INAKTIV_VED_8_47_B", defaultVerdi = "false") boolean skalHaInaktivVed847B,
                                    @KonfigVerdi(value = "ENABLE_NY_UTLEDNING_AV_UTENLANDSOPPHOLD", defaultVerdi = "false") boolean nyUtledningAvUtenlandsopphold) {
        this.hentDataTilUttakTjeneste = hentDataTilUttakTjeneste;
        this.unntak = unntak;
        this.skalKjøreNyLogikkForSpeiling = skalKjøreNyLogikkForSpeiling;
        this.skalHaInaktivVed847B = skalHaInaktivVed847B;
        this.nyUtledningAvUtenlandsopphold = nyUtledningAvUtenlandsopphold;
    }


    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {
        return toRequestData(hentDataTilUttakTjeneste.hentUtData(referanse, false, true));
    }

    public Uttaksgrunnlag hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse referanse) {
        return toRequestData(hentDataTilUttakTjeneste.hentUtData(referanse, false, false));
    }


    public Uttaksgrunnlag hentUtUbesluttededataOgMapRequest(BehandlingReferanse referanse) {
        return toRequestData(hentDataTilUttakTjeneste.hentUtData(referanse, true, true));
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

        var søker = new Søker(søkerPersonopplysninger.getAktørId().getId(), søkerPersonopplysninger.getDødsdato());

        var tidslinjeTilVurdering = new LocalDateTimeline<>(mapPerioderTilVurdering(input));

        final List<SøktUttak> søktUttak = new MapUttak().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering, input.getUtvidetPeriodeSomFølgeAvDødsfall());

        var opptjeningTidslinje = new LocalDateTimeline<>(input.getVilkårene()
            .getVilkår(VilkårType.OPPTJENINGSVILKÅRET)
            .orElseThrow()
            .getPerioder()
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), true)).toList());

        var inaktivitetUtlederInput = new InaktivitetUtlederInput(
            behandling.getAktørId(),
            opptjeningTidslinje,
            input.getInntektArbeidYtelseGrunnlag(),
            skalKjøreNyLogikkForSpeiling || behandling.getFagsak().getSaksnummer().getVerdi().equals(SPEILE_SAK_SOM_HAR_BLITT_FEIL),
            input.getBeregningsgrunnlag());
        var inaktivTidslinje = new PerioderMedInaktivitetUtleder().utled(inaktivitetUtlederInput);

        var arbeidstidInput = new ArbeidstidMappingInput()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medKravDokumenter(kravDokumenter)
            .medPerioderFraSøknader(perioderFraSøknader)
            .medTidslinjeTilVurdering(tidslinjeTilVurdering)
            .medVilkår(input.getVilkårene().getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow())
            .medSkalHaInaktivVed847B(skalHaInaktivVed847B)
            .medOpptjeningsResultat(input.getOpptjeningResultat().orElse(null))
            .medInaktivTidslinje(inaktivTidslinje)
            .medInntektArbeidYtelseGrunnlag(input.getInntektArbeidYtelseGrunnlag())
            .medBruker(behandling.getAktørId())
            .medSakerSomMåSpesialHåndteres(MapUnntakFraAktivitetGenerering.mapUnntak(unntak))
            .medTilkommetAktivitetsperioder(input.getTilkommetAktivitetsperioder());

        input.getUtvidetPeriodeSomFølgeAvDødsfall().ifPresent(arbeidstidInput::medAutomatiskUtvidelseVedDødsfall);

        final List<Arbeid> arbeid = new MapArbeid().map(arbeidstidInput);

        final Map<LukketPeriode, Pleiebehov> pleiebehov = toPleiebehov(input);

        final List<LukketPeriode> lovbestemtFerie = new MapFerie().map(vurderteSøknadsperioder, perioderFraSøknader, tidslinjeTilVurdering);

        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(input.getVilkårene());

        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(input.getEtablertTilsynPerioder());

        var innvilgedePerioderMedSykdom = finnInnvilgedePerioderSykdom(input.getVilkårene(), input.getDefinerendeVilkårtyper());

        var unntakEtablertTilsynForPleietrengende = input.getUnntakEtablertTilsynForPleietrengende().orElse(null);
        var beredskapsperioder = tilBeredskap(unntakEtablertTilsynForPleietrengende, innvilgedePerioderMedSykdom);
        var nattevåksperioder = tilNattevåk(unntakEtablertTilsynForPleietrengende, innvilgedePerioderMedSykdom);
        final Map<LukketPeriode, List<String>> kravprioritet = mapKravprioritetsliste(input.getKravprioritet());
        final List<LukketPeriode> perioderSomSkalTilbakestilles = input.getPerioderSomSkalTilbakestilles().stream().map(p -> new LukketPeriode(p.getFomDato(), p.getTomDato())).toList();
        Map<LukketPeriode, UtenlandsoppholdInfo> utenlandsoppholdperioder = MapUtenlandsopphold.map(vurderteSøknadsperioder, perioderFraSøknader, tidslinjeTilVurdering, nyUtledningAvUtenlandsopphold);

        Map<String, String> sisteVedtatteBehandlingForAvktuellBehandling = mapSisteVedtatteBehandlingForBehandling(input.getSisteVedtatteBehandlingForBehandling());
        Map<LukketPeriode, OverstyrtInput> overstyrtUttak = map(input.getOverstyrtUttak());

        var nedjustertSøkersUttaksgrad = mapNedjustertUttaksgrad(input.getNedjustertUttaksgrad());

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
            input.getVirkningsdatoNyeRegler(),
            overstyrtUttak,
            nedjustertSøkersUttaksgrad,
            lovbestemtFerie,
            inngangsvilkår,
            tilsynsperioder,
            beredskapsperioder,
            nattevåksperioder,
            kravprioritet,
            sisteVedtatteBehandlingForAvktuellBehandling,
            utenlandsoppholdperioder
        );
    }

    private Map<LukketPeriode, Inntektsgradering> mapNedjustertUttaksgrad(LocalDateTimeline<BigDecimal> nedjustertUttaksgrad) {
        Map<LukketPeriode, Inntektsgradering> nedjustert = new HashMap<>();
        nedjustertUttaksgrad.stream().forEach(segment -> {
            LukketPeriode periode = new LukketPeriode(segment.getFom(), segment.getTom());
            nedjustert.put(periode, new Inntektsgradering(segment.getValue()));
        });
        return nedjustert;
    }


    private Map<LukketPeriode, OverstyrtInput> map(LocalDateTimeline<OverstyrtUttakPeriode> overstyrtUttak) {
        Map<LukketPeriode, OverstyrtInput> overstyrt = new HashMap<>();
        overstyrtUttak.stream().forEach(segment -> {
            LukketPeriode periode = new LukketPeriode(segment.getFom(), segment.getTom());
            OverstyrtInput overstyrtInput = map(segment.getValue());
            overstyrt.put(periode, overstyrtInput);
        });
        return overstyrt;
    }

    private OverstyrtInput map(OverstyrtUttakPeriode overstyrtUttakPeriode) {
        List<OverstyrtUtbetalingsgradPerArbeidsforhold> overstyrteUtbetalingsgrader = overstyrtUttakPeriode.getOverstyrtUtbetalingsgrad().stream().map(this::map).toList();
        return new OverstyrtInput(overstyrtUttakPeriode.getSøkersUttaksgrad(), overstyrteUtbetalingsgrader);
    }

    private OverstyrtUtbetalingsgradPerArbeidsforhold map(OverstyrtUttakUtbetalingsgrad overstyrtUttakUtbetalingsgrad) {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold(
            overstyrtUttakUtbetalingsgrad.getAktivitetType().getKode(),
            overstyrtUttakUtbetalingsgrad.getArbeidsgiver() != null ? overstyrtUttakUtbetalingsgrad.getArbeidsgiver().getArbeidsgiverOrgnr() : null,
            overstyrtUttakUtbetalingsgrad.getArbeidsgiver() != null ? overstyrtUttakUtbetalingsgrad.getArbeidsgiver().getArbeidsgiverAktørId() : null,
            overstyrtUttakUtbetalingsgrad.getInternArbeidsforholdRef() != null ? overstyrtUttakUtbetalingsgrad.getInternArbeidsforholdRef().getReferanse() : null
        );

        return new OverstyrtUtbetalingsgradPerArbeidsforhold(overstyrtUttakUtbetalingsgrad.getUtbetalingsgrad(), arbeidsforhold);
    }


    private Map<String, String> mapSisteVedtatteBehandlingForBehandling(Map<UUID, UUID> sisteVedtatteBehandlingForBehandling) {
        Map<String, String> behandlinger = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : sisteVedtatteBehandlingForBehandling.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                behandlinger.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return behandlinger;
    }

    private YtelseType mapTilYtelseType(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PSB;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLS;
            case OPPLÆRINGSPENGER -> YtelseType.OLP;
            default ->
                throw new IllegalStateException("Ikke støttet ytelse for uttak Pleiepenger: " + behandling.getFagsakYtelseType());
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
                        case IKKE_VURDERT ->
                            throw new IllegalStateException("Skal ikke komme perioder som ikke er vurdert til uttak.");
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

        var utsattBehandlingAvPerioder = input.getUtsattBehandlingAvPerioder();
        if (utsattBehandlingAvPerioder.isPresent()) {
            var perioderSomHarBlittUtsatt = utsattBehandlingAvPerioder.map(UtsattBehandlingAvPeriode::getPerioder).orElseThrow();
            var utsattTidslinje = new LocalDateTimeline<>(perioderSomHarBlittUtsatt.stream()
                .map(UtsattPeriode::getPeriode)
                .map(DatoIntervallEntitet::toLocalDateInterval)
                .map(it -> new LocalDateSegment<>(it, false))
                .collect(Collectors.toSet()), StandardCombinators::alwaysTrueForMatch);

            timeline = timeline.disjoint(utsattTidslinje); // Tar bort periodene som har blitt utsatt
        }

        return new ArrayList<>(timeline.compress().toSegments());
    }

    private Map<LukketPeriode, Pleiebehov> toPleiebehov(InputParametere inputParametere) {
        var pleiebehov = inputParametere.getPleiebehov();
        var timeline = new LocalDateTimeline<>(pleiebehov.stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), mapToPleiebehov(it.getGrad())))
            .toList());

        if (inputParametere.getUtvidetPeriodeSomFølgeAvDødsfall().isPresent()) {
            var manglendePerioderIDødsPerioden = new LocalDateTimeline<Pleiebehov>(List.of(new LocalDateSegment<>(inputParametere.getUtvidetPeriodeSomFølgeAvDødsfall().get().toLocalDateInterval(), null))).disjoint(timeline);
            for (LocalDateSegment<Pleiebehov> segment : manglendePerioderIDødsPerioden) {
                var manglendeSegment = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), Pleiebehov.PROSENT_100)));
                timeline = timeline.combine(manglendeSegment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }

        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = new HashMap<>();
        timeline.toSegments().forEach(p -> tilsynsbehov.put(toLukketPeriode(DatoIntervallEntitet.fra(p.getLocalDateInterval())), p.getValue()));
        return tilsynsbehov;
    }

    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }

    private Pleiebehov mapToPleiebehov(Pleiegrad grad) {
        return switch (grad) {
            case INGEN -> Pleiebehov.PROSENT_0;
            case LIVETS_SLUTT_TILSYN, KONTINUERLIG_TILSYN, NØDVENDIG_OPPLÆRING -> Pleiebehov.PROSENT_100;
            case LIVETS_SLUTT_TILSYN_FOM2023, UTVIDET_KONTINUERLIG_TILSYN, INNLEGGELSE -> Pleiebehov.PROSENT_200;
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
