package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.formidling.kontrakt.kodeverk.Mottaker;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.inntektsmelding.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.OrganisasjonsNummerValidator;

@ApplicationScoped
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerKompletthetsjekker implements Kompletthetsjekker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmsorgspengerKompletthetsjekker.class);
    public static final int ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING_TOTALT = 3;
    public static final int ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING_FØR_ETTERLYSNING = 1;

    private Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BehandlingRepository behandlingRepository;
    private KompletthetsjekkerFelles fellesUtil;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    boolean etterlysInntektsmeldingerLansert;

    OmsorgspengerKompletthetsjekker() {
        // CDI
    }

    @Inject
    public OmsorgspengerKompletthetsjekker(@Any Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                           BehandlingRepository behandlingRepository,
                                           KompletthetsjekkerFelles fellesUtil,
                                           @FagsakYtelseTypeRef("OMP") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                           @FagsakYtelseTypeRef("OMP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           @KonfigVerdi(value = "OMP_ETTERLYS_IM", defaultVerdi = "true", required = false) boolean etterlysInntektsmeldingerLansert) {
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fellesUtil = fellesUtil;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.etterlysInntektsmeldingerLansert = etterlysInntektsmeldingerLansert;
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        if (BehandlingStatus.OPPRETTET.equals(ref.getBehandlingStatus())) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling,
        // hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldinger(ref, utledSkjæringstidspunkt(ref));
        if (!manglendeInntektsmeldinger.isEmpty()) {
            int antallArbeidsgivere = manglendeInntektsmeldinger.stream().map(ManglendeVedlegg::getArbeidsgiver).collect(Collectors.toSet()).size();
            LOGGER.info("Behandling {} er ikke komplett - mangler {} IM fra {} arbeidsgivere.", ref.getBehandlingId(), manglendeInntektsmeldinger.size(), antallArbeidsgivere); // NOSONAR //$NON-NLS-1$
            Optional<LocalDateTime> ventefristManglendeIM = finnVentefristTilManglendeInntektsmelding(ref, etterlysInntektsmeldingerLansert ? ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING_FØR_ETTERLYSNING : ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING_TOTALT);
            return ventefristManglendeIM
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt()); // Setter til oppfylt om fristen er passert
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        List<ManglendeVedlegg> manglendeVedlegg = new ArrayList<>();
        manglendeVedlegg.addAll(getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldingerFraGrunnlag(ref, utledSkjæringstidspunkt(ref)));
        return manglendeVedlegg;
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
        if (!etterlysInntektsmeldingerLansert){
            return KompletthetResultat.oppfylt();
        }

        Long behandlingId = ref.getBehandlingId();

        // Utled vilkårsperioder
        TreeSet<DatoIntervallEntitet> vilkårsPerioder = utledVilkårsperioderRelevantForBehandling(ref);

        if (vilkårsPerioder.isEmpty()) {
            return KompletthetResultat.oppfylt();
        }

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (ref.erRevurdering() && behandling.erManueltOpprettet()) {
            return KompletthetResultat.oppfylt();
        }
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldinger(ref, utledSkjæringstidspunkt(ref));
        if (manglendeInntektsmeldinger.isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        Set<Arbeidsgiver> arbeidsgivere = manglendeInntektsmeldinger.stream()
            .filter(it -> DokumentTypeId.INNTEKTSMELDING.equals(it.getDokumentType()))
            .filter(it -> !it.getBrukerHarSagtAtIkkeKommer())
            .map(it -> (OrganisasjonsNummerValidator.erGyldig(it.getArbeidsgiver()) || OrgNummer.erKunstig(it.getArbeidsgiver())) ? Arbeidsgiver.virksomhet(it.getArbeidsgiver()) : Arbeidsgiver.fra(new AktørId(it.getArbeidsgiver())))
            .collect(Collectors.toSet());
        sendEtterlysningForManglendeInntektsmeldinger(ref, arbeidsgivere);
        return finnVentefristTilManglendeInntektsmelding(ref, ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING_TOTALT)
            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
            .orElse(KompletthetResultat.fristUtløpt());
    }

    private TreeSet<DatoIntervallEntitet> utledVilkårsperioderRelevantForBehandling(BehandlingReferanse ref) {
        return perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private void sendEtterlysningForManglendeInntektsmeldinger(BehandlingReferanse ref, Set<Arbeidsgiver> arbeidsgiverIdenterSomSkalMottaEtterlysning) {
        arbeidsgiverIdenterSomSkalMottaEtterlysning.forEach(a -> {
                var idType = a.getErVirksomhet() ? IdType.ORGNR : IdType.AKTØRID;
                fellesUtil.sendBrev(ref.getBehandlingId(), DokumentMalType.ETTERLYS_INNTEKTSMELDING_DOK, new Mottaker(a.getIdentifikator(), idType));
            }
        );
    }

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref, int antallDager) {
        var muligFrist = behandlingRepository.hentBehandling(ref.getBehandlingId())
            .getOpprettetTidspunkt()
            .toLocalDate()
            .plusDays(antallDager);
        return fellesUtil.finnVentefrist(muligFrist);
    }

    private LocalDate utledSkjæringstidspunkt(BehandlingReferanse ref) {
        return skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(ref.getBehandlingId(), FagsakYtelseType.OMSORGSPENGER);
    }

    private KompletthetssjekkerInntektsmelding getKompletthetsjekkerInntektsmelding(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.get(KompletthetssjekkerInntektsmelding.class, kompletthetssjekkerInntektsmelding, ref.getFagsakYtelseType(), ref.getBehandlingType());
    }
}
