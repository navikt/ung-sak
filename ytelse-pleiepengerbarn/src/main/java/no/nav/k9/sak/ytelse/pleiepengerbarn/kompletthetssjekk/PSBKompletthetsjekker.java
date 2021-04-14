package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.formidling.kontrakt.kodeverk.Mottaker;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.OrganisasjonsNummerValidator;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class PSBKompletthetsjekker implements Kompletthetsjekker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PSBKompletthetsjekker.class);

    private static final Integer TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER = 3;
    private static final Integer VENTEFRIST_ETTER_MOTATT_DATO_UKER = 1;
    private static final Integer VENTEFRIST_ETTER_ETTERLYSNING_UKER = 3;

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning;
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KompletthetsjekkerFelles fellesUtil;
    private SøknadRepository søknadRepository;

    PSBKompletthetsjekker() {
        // CDI
    }

    @Inject
    public PSBKompletthetsjekker(KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                 @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                 @FagsakYtelseTypeRef("PSB") InntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning,
                                 ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                 InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                 InntektArbeidYtelseTjeneste iayTjeneste,
                                 KompletthetsjekkerFelles fellesUtil,
                                 SøknadRepository søknadRepository) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.fellesUtil = fellesUtil;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDateTime> forTidligFrist = kompletthetssjekkerSøknad.erSøknadMottattForTidlig(ref);
        return forTidligFrist.map(localDateTime -> KompletthetResultat.ikkeOppfylt(localDateTime, Venteårsak.FOR_TIDLIG_SOKNAD)).orElseGet(KompletthetResultat::oppfylt);
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        if (BehandlingStatus.OPPRETTET.equals(ref.getBehandlingStatus())) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling,
        // hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraRegisterFunction(arbeidsforholdTjeneste);
        var ventefrister = new ArrayList<LocalDateTime>();
        var result = utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction);
        for (Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> entry : result.entrySet()) {
            var manglendeInntektsmeldinger = entry.getValue()
                .stream()
                .filter(it -> DokumentTypeId.INNTEKTSMELDING.equals(it.getDokumentType()))
                .collect(Collectors.toList());

            if (!manglendeInntektsmeldinger.isEmpty()) {
                loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
                Optional<LocalDateTime> ventefristManglendeIM = finnVentefristTilManglendeInntektsmelding(entry.getKey());
                ventefristManglendeIM.ifPresent(ventefrister::add);
            }
        }
        return utledKompletthetResultat(ventefrister);
    }

    private KompletthetResultat utledKompletthetResultat(List<LocalDateTime> ventefrister) {

        if (ventefrister.isEmpty()) {
            return KompletthetResultat.oppfylt();
        }

        var kompletthetResultat = ventefrister.stream()
            .filter(it -> it.isAfter(LocalDateTime.now()))
            .min(LocalDateTime::compareTo)
            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
            .orElse(KompletthetResultat.fristUtløpt());

        return kompletthetResultat;
    }

    private void loggManglendeInntektsmeldinger(Long behandlingId, @SuppressWarnings("unused") List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        LOGGER.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere", behandlingId); // NOSONAR //$NON-NLS-1$
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId()));
        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction)
            .values()
            .stream()
            .allMatch(List::isEmpty);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId()));
        return utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction)
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedlegg(BehandlingReferanse ref,
                                                                                BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction) {
        var perioderMedManglendeVedlegg = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();

        // Utled vilkårsperioder
        var vilkårsPerioder = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato))
            .collect(Collectors.toCollection(TreeSet::new));

        if (vilkårsPerioder.isEmpty()) {
            return perioderMedManglendeVedlegg;
        }

        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var tidslinje = new LocalDateTimeline<>(vilkårsPerioder.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()));

        var relevantePerioder = vilkårsPerioder.stream().map(it -> utledRelevantPeriode(tidslinje, it)).collect(Collectors.toSet());

        // For alle relevanteperioder vurder kompletthet
        for (DatoIntervallEntitet periode : relevantePerioder) {
            var utledManglendeVedleggForPeriode = utledManglendeVedleggForPeriode(ref, inntektsmeldinger, periode, vilkårsPerioder, finnArbeidsforholdForIdentPåDagFunction);
            perioderMedManglendeVedlegg.putAll(utledManglendeVedleggForPeriode);
        }

        return perioderMedManglendeVedlegg;
    }

    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledManglendeVedleggForPeriode(BehandlingReferanse ref, Set<Inntektsmelding> inntektsmeldinger,
                                                                                              DatoIntervallEntitet relevantPeriode,
                                                                                              Set<DatoIntervallEntitet> vilkårsPerioder,
                                                                                              BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> finnArbeidsforholdForIdentPåDagFunction) {

        var result = new HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>>();
        var relevanteInntektsmeldinger = utledRelevanteInntektsmeldinger(inntektsmeldinger, relevantPeriode);
        var tilnternArbeidsforhold = new FinnEksternReferanse(iayTjeneste, ref.getBehandlingId());
        for (DatoIntervallEntitet periode : vilkårsPerioder) {
            var arbeidsgiverSetMap = finnArbeidsforholdForIdentPåDagFunction.apply(ref, relevantPeriode.getFomDato());
            utledManglendeInntektsmeldingerPerDag(result, relevanteInntektsmeldinger, periode, tilnternArbeidsforhold, arbeidsgiverSetMap);
        }
        return result;
    }

    private <V> void utledManglendeInntektsmeldingerPerDag(HashMap<DatoIntervallEntitet, List<ManglendeVedlegg>> result,
                                                           Set<Inntektsmelding> relevanteInntektsmeldinger,
                                                           DatoIntervallEntitet periode,
                                                           BiFunction<Arbeidsgiver, InternArbeidsforholdRef, V> tilnternArbeidsforhold,
                                                           Map<Arbeidsgiver, Set<V>> påkrevdeInntektsmeldinger) {
        if (påkrevdeInntektsmeldinger.isEmpty()) {
            result.put(periode, List.of());
        } else {
            var prioriterteInntektsmeldinger = inntektsmeldingerRelevantForBeregning.utledInntektsmeldingerSomGjelderForPeriode(relevanteInntektsmeldinger, periode);

            for (Inntektsmelding inntektsmelding : prioriterteInntektsmeldinger) {
                if (påkrevdeInntektsmeldinger.containsKey(inntektsmelding.getArbeidsgiver())) {
                    final Set<V> arbeidsforhold = påkrevdeInntektsmeldinger.get(inntektsmelding.getArbeidsgiver());
                    if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
                        V matchKey = tilnternArbeidsforhold.apply(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef());
                        arbeidsforhold.remove(matchKey);
                    } else {
                        arbeidsforhold.clear();
                    }
                    if (arbeidsforhold.isEmpty()) {
                        påkrevdeInntektsmeldinger.remove(inntektsmelding.getArbeidsgiver());
                    }
                }
            }

            var manglendeInntektsmeldinger = påkrevdeInntektsmeldinger.keySet()
                .stream()
                .map(key -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, key.getIdentifikator()))
                .collect(Collectors.toList());
            result.put(periode, manglendeInntektsmeldinger);
        }
    }

    private Set<Inntektsmelding> utledRelevanteInntektsmeldinger(Set<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet relevantPeriode) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getStartDatoPermisjon().isEmpty() || relevantPeriode.inkluderer(im.getStartDatoPermisjon().orElseThrow()))
            .collect(Collectors.toSet());
    }

    DatoIntervallEntitet utledRelevantPeriode(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet periode) {
        return utledRelevantPeriode(tidslinje, periode, true);
    }

    private DatoIntervallEntitet utledRelevantPeriode(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet periode, boolean justerStart) {
        DatoIntervallEntitet orginalRelevantPeriode = periode;
        if (justerStart) {
            orginalRelevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusWeeks(4), periode.getTomDato().plusWeeks(4));
        }

        if (tidslinje.isEmpty()) {
            return orginalRelevantPeriode;
        }
        var intersection = tidslinje.intersection(new LocalDateInterval(periode.getFomDato().minusWeeks(4), periode.getTomDato().plusWeeks(4)));
        var relevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(intersection.getMinLocalDate().minusWeeks(4), intersection.getMaxLocalDate().plusWeeks(4));

        if (orginalRelevantPeriode.equals(relevantPeriode)) {
            return relevantPeriode;
        }

        return utledRelevantPeriode(tidslinje, relevantPeriode, false);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste
            .hentAlleInntektsmeldingerSomIkkeKommer(ref.getBehandlingId())
            .stream()
            .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver().getIdentifikator(), true))
            .collect(Collectors.toList());
    }

    @Override
    public KompletthetResultat vurderEtterlysningInntektsmelding(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();

        var result = new ArrayList<LocalDateTime>();
        // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        var finnArbeidsforholdForIdentPåDagFunction = new UtledManglendeInntektsmeldingerFraGrunnlagFunction(iayTjeneste,
            new FinnEksternReferanse(iayTjeneste, behandlingId));
        var utledet = utledAlleManglendeVedlegg(ref, finnArbeidsforholdForIdentPåDagFunction);
        for (Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> entry : utledet.entrySet()) {
            var manglendeInntektsmeldinger = entry.getValue();
            if (!manglendeInntektsmeldinger.isEmpty()) {
                loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
                sendEtterlysningForManglendeInntektsmeldinger(ref, manglendeInntektsmeldinger);
                finnVentefristForEtterlysning(ref, entry.getKey().getFomDato()).ifPresent(result::add);
            }
        }

        return result.stream()
            .min(LocalDateTime::compareTo)
            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
            .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift i prosessen
    }

    private void sendEtterlysningForManglendeInntektsmeldinger(BehandlingReferanse ref, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        var arbeidsgiverIdenterSomSkalMottaEtterlysning = manglendeInntektsmeldinger.stream()
            .filter(a -> !a.getBrukerHarSagtAtIkkeKommer())
            .map(ManglendeVedlegg::getArbeidsgiver);

        arbeidsgiverIdenterSomSkalMottaEtterlysning.forEach(a -> {
                var idType = (OrganisasjonsNummerValidator.erGyldig(a) || OrgNummer.erKunstig(a)) ? IdType.ORGNR : IdType.AKTØRID;
                fellesUtil.sendBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK, new Mottaker(a, idType));
            }
        );
    }

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(DatoIntervallEntitet key) {
        Objects.requireNonNull(key);
        final LocalDate muligFrist = key.getFomDato().minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        return fellesUtil.finnVentefrist(muligFrist);
    }

    private Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref, LocalDate permisjonsstart) {
        Long behandlingId = ref.getBehandlingId();
        final LocalDate muligFrist = LocalDate.now().isBefore(permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER)) ? LocalDate.now()
            : permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        final Optional<LocalDate> annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId).map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        final LocalDate ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return fellesUtil.finnVentefrist(ønsketFrist.plusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER));
    }
}
