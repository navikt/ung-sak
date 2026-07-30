package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.søknad.felles.type.Landkode;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(ForutgåendeMedlemskapsvilkårSteg.class);

    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public ForutgåendeMedlemskapsvilkårSteg() {
    }

    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(VilkårResultatRepository vilkårResultatRepository,
                                            OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                            MottatteDokumentRepository mottatteDokumentRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var periodeTilVurdering = getPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType())
            .utled(behandlingId, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        if (periodeTilVurdering.isEmpty()) {
            log.info("Ingen perioder til vurdering");
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        periodeTilVurdering = filtrerBortIkkeRelevantePerioder(periodeTilVurdering, vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET));

        if (periodeTilVurdering.isEmpty()) {
            log.info("Ingen relevante perioder til vurdering");
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        //TODO endre til å hente fra kun vilkår som vurderes før dette vilkåret
        var avslåttTidslinje = lagAvslåttTidslinje(vilkårene);
        var avslåttePerioder = finnAvslåttePerioder(periodeTilVurdering, avslåttTidslinje);
        if (!avslåttePerioder.isEmpty()) {
            log.info("Setter perioder til ikke relevant pga andre avslåtte vilkår på hele periodeTilVurdering.");
            vilkårResultatRepository.settPerioderTilIkkeRelevant(behandlingId, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, avslåttePerioder);
            periodeTilVurdering.removeAll(avslåttePerioder);
        }

        if (periodeTilVurdering.isEmpty()) {
            log.info("Ingen periode til vurdering etter å ha fjernet avslåtte vilkår");
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return vurderForutgåendeMedlemskap(periodeTilVurdering, behandlingId, behandling.getFagsakId());
    }

    private BehandleStegResultat vurderForutgåendeMedlemskap(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Long behandlingId, Long fagsakId) {
        var grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            log.info("Fant ingen grunnlag. Lager aksjonspunkt.");
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        var grunnlag = grunnlagOpt.get();
        var bostederTidslinje = lagBostederTidslinje(grunnlag, fagsakId);

        var stegerVurderinger = perioderTilVurdering.stream()
            .map(periode -> vurder(periode, bostederTidslinje))
            .toList();

        var trengerManuellVurdering = stegerVurderinger.stream()
            .anyMatch(v -> !v.vurdering().filterValue(u -> u != Utfall.OPPFYLT).isEmpty());

        if (trengerManuellVurdering) {
            log.info("Fant bosteder som ikke har trygdeavtale, lager aksjonspunkt.");
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        oppfyllVilkår(behandlingId, bostederTidslinje, stegerVurderinger);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static StegVurdering vurder(DatoIntervallEntitet periodeTilVurdering, LocalDateTimeline<String> bostederTidslinje) {
        LocalDate virkningsdato = periodeTilVurdering.getFomDato();
        var forutgåendePeriodeTilVurdering = new LocalDateInterval(virkningsdato.minusYears(5), virkningsdato.minusDays(1));
        var bostedVurdering = new LocalDateTimeline<>(forutgåendePeriodeTilVurdering, Boolean.TRUE)
            .combine(bostederTidslinje, ForutgåendeMedlemskapsvilkårSteg::vurderBosted, JoinStyle.LEFT_JOIN);
        return new StegVurdering(periodeTilVurdering, forutgåendePeriodeTilVurdering, bostedVurdering);
    }

    private void oppfyllVilkår(Long behandlingId, LocalDateTimeline<String> bostederTidslinje, List<StegVurdering> stegVurderinger) {
        var jsonMapper = new VilkårJsonObjectMapper();
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandlingId));
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        stegVurderinger.forEach(stegVurdering -> {
            var regelInput = jsonMapper.writeValueAsString(new RegelInput(bostederTidslinje));
            var regelEvaluering = jsonMapper.writeValueAsString(stegVurdering);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(stegVurdering.periodeTilVurdering())
                .medUtfall(Utfall.OPPFYLT)
                .medAvslagsårsak(null)
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering));
        });

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }

    private static LocalDateSegment<Utfall> vurderBosted(LocalDateInterval intervall, LocalDateSegment<Boolean> forutgåendePeriodeTilVurdering, LocalDateSegment<String> landkode) {
        if (landkode == null || landkode.getValue() == null) {
            return new LocalDateSegment<>(intervall, Utfall.IKKE_VURDERT);
        }
        if (TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(landkode.getValue(), intervall.getFomDato())) {
            return new LocalDateSegment<>(intervall, Utfall.OPPFYLT);
        }
        return new LocalDateSegment<>(intervall, Utfall.IKKE_OPPFYLT);
    }

    private LocalDateTimeline<String> lagBostederTidslinje(OppgittForutgåendeMedlemskapGrunnlag grunnlag, Long fagsakId) {
        var journalpostIder = grunnlag.getOppgittePerioder().stream()
            .map(OppgittForutgåendeMedlemskapPeriode::getJournalpostId)
            .toList();

        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder);
        var nyesteJournalpostId = mottatteDokumenter.stream()
            .max(Comparator.comparing(MottattDokument::getMottattTidspunkt))
            .map(MottattDokument::getJournalpostId)
            .orElseThrow();

        var nyestePeriode = grunnlag.getOppgittePerioder().stream()
            .filter(p -> p.getJournalpostId().equals(nyesteJournalpostId))
            .findFirst()
            .orElseThrow();

        var antattBostedNorgeTidslinje = new LocalDateTimeline<>(nyestePeriode.getPeriode().getFomDato(), nyestePeriode.getPeriode().getTomDato(), Landkode.NORGE.getLandkode());

        var bostederUtlandTidslinje = new LocalDateTimeline<>(
            nyestePeriode.getBostederUtland().stream()
                .map(b -> new LocalDateSegment<>(b.getPeriode().getFomDato(), b.getPeriode().getTomDato(), b.getLandkode()))
                .toList());

        return bostederUtlandTidslinje.crossJoin(antattBostedNorgeTidslinje);
    }

    private static LocalDateTimeline<Boolean> lagAvslåttTidslinje(Vilkårene vilkårene) {
        var avslåtteSegmenter = vilkårene.getVilkårene().stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> Utfall.IKKE_OPPFYLT.equals(p.getGjeldendeUtfall()))
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .toList();
        return new LocalDateTimeline<>(avslåtteSegmenter, StandardCombinators::alwaysTrueForMatch)
            .compress();
    }

    private static NavigableSet<DatoIntervallEntitet> filtrerBortIkkeRelevantePerioder(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Optional<Vilkår> vilkår) {
        var ikkeRelevantePerioder = vilkår
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> Utfall.IKKE_RELEVANT.equals(p.getGjeldendeUtfall()))
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .toList();
        if (ikkeRelevantePerioder.isEmpty()) {
            return perioderTilVurdering;
        }
        var resultat = new TreeSet<>(perioderTilVurdering);
        resultat.removeAll(ikkeRelevantePerioder);
        return resultat;
    }

    private static NavigableSet<DatoIntervallEntitet> finnAvslåttePerioder(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDateTimeline<Boolean> avslåttTidslinje) {
        if (avslåttTidslinje.isEmpty()) {
            return new TreeSet<>();
        }
        return perioderTilVurdering.stream()
            .filter(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), Boolean.TRUE).disjoint(avslåttTidslinje).isEmpty())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    record RegelInput(LocalDateTimeline<String> bostederLandkodeTidslinje) { }

    record StegVurdering(DatoIntervallEntitet periodeTilVurdering, LocalDateInterval forutgåendePeriode, LocalDateTimeline<Utfall> vurdering) {}

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, fagsakYtelseType, behandlingType);
    }
}
