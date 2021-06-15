package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
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
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private KompletthetsjekkerFelles fellesUtil;
    private SøknadRepository søknadRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    PSBKompletthetsjekker() {
        // CDI
    }

    @Inject
    public PSBKompletthetsjekker(KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                 InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                 KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                 KompletthetsjekkerFelles fellesUtil,
                                 SøknadRepository søknadRepository,
                                 @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.fellesUtil = fellesUtil;
        this.søknadRepository = søknadRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
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
        // Utled vilkårsperioder
        TreeSet<DatoIntervallEntitet> vilkårsPerioder = utledVilkårsperioderRelevantForBehandling(ref);

        if (vilkårsPerioder.isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling,
        // hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        var ventefrister = new ArrayList<LocalDateTime>();
        var utledet = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraRegister(ref);
        List<Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>>> relevanteKompletthetsvurderinger = utledRelevanteKompletthetsvurderinger(vilkårsPerioder, utledet);
        for (Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> entry : relevanteKompletthetsvurderinger) {
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

    private TreeSet<DatoIntervallEntitet> utledVilkårsperioderRelevantForBehandling(BehandlingReferanse ref) {
        return perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato))
            .collect(Collectors.toCollection(TreeSet::new));
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
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref)
            .values()
            .stream()
            .allMatch(List::isEmpty);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref)
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggForPerioder(BehandlingReferanse ref) {
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref);
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

        // Utled vilkårsperioder
        TreeSet<DatoIntervallEntitet> vilkårsPerioder = utledVilkårsperioderRelevantForBehandling(ref);

        if (vilkårsPerioder.isEmpty()) {
            return KompletthetResultat.oppfylt();
        }

        var result = new ArrayList<LocalDateTime>();
        // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        var utledet = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref);
        List<Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>>> relevanteKompletthetsvurderinger = utledRelevanteKompletthetsvurderinger(vilkårsPerioder, utledet);

        var inntektsmeldingerSomSkalEtterlyses = new HashSet<Arbeidsgiver>();

        for (Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> entry : relevanteKompletthetsvurderinger) {
            var manglendeInntektsmeldinger = entry.getValue();
            if (!manglendeInntektsmeldinger.isEmpty()) {
                loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
                inntektsmeldingerSomSkalEtterlyses.addAll(manglendeInntektsmeldinger.stream()
                    .filter(it -> DokumentTypeId.INNTEKTSMELDING.equals(it.getDokumentType()))
                    .filter(it -> !it.getBrukerHarSagtAtIkkeKommer())
                    .map(it -> (OrganisasjonsNummerValidator.erGyldig(it.getArbeidsgiver()) || OrgNummer.erKunstig(it.getArbeidsgiver())) ? Arbeidsgiver.virksomhet(it.getArbeidsgiver()) : Arbeidsgiver.fra(new AktørId(it.getArbeidsgiver())))
                    .collect(Collectors.toSet()));
                finnVentefristForEtterlysning(ref, entry.getKey().getFomDato()).ifPresent(result::add);
            }
        }

        if (!inntektsmeldingerSomSkalEtterlyses.isEmpty()) {
            sendEtterlysningForManglendeInntektsmeldinger(ref, inntektsmeldingerSomSkalEtterlyses);
        }

        return result.stream()
            .min(LocalDateTime::compareTo)
            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
            .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift i prosessen
    }

    private List<Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>>> utledRelevanteKompletthetsvurderinger(TreeSet<DatoIntervallEntitet> vilkårsPerioder, Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledet) {
        var relevanteKompletthetsvurderinger = utledet.entrySet()
            .stream()
            .filter(it -> vilkårsPerioder.contains(it.getKey()))
            .collect(Collectors.toList());
        return relevanteKompletthetsvurderinger;
    }

    private void sendEtterlysningForManglendeInntektsmeldinger(BehandlingReferanse ref, Set<Arbeidsgiver> arbeidsgiverIdenterSomSkalMottaEtterlysning) {
        arbeidsgiverIdenterSomSkalMottaEtterlysning.forEach(a -> {
                var idType = a.getErVirksomhet() ? IdType.ORGNR : IdType.AKTØRID;
                fellesUtil.sendBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK, new Mottaker(a.getIdentifikator(), idType));
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
