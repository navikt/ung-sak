package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.formidling.kontrakt.kodeverk.Mottaker;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.OrganisasjonsNummerValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.inntektsmelding.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class PsbKompletthetsjekker implements Kompletthetsjekker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsbKompletthetsjekker.class);

    private static final Integer TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER = 3;
    private static final Integer VENTEFRIST_ETTER_MOTATT_DATO_UKER = 1;
    private static final Integer VENTEFRIST_ETTER_ETTERLYSNING_UKER = 3;

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KompletthetsjekkerFelles fellesUtil;
    private SøknadRepository søknadRepository;

    PsbKompletthetsjekker() {
        // CDI
    }

    @Inject
    public PsbKompletthetsjekker(KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                 @Any Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding,
                                 InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                 KompletthetsjekkerFelles fellesUtil,
                                 SøknadRepository søknadRepository) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fellesUtil = fellesUtil;
        this.søknadRepository = søknadRepository;
    }

    private KompletthetssjekkerInntektsmelding getKompletthetsjekkerInntektsmelding(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.get(KompletthetssjekkerInntektsmelding.class, kompletthetssjekkerInntektsmelding, ref.getFagsakYtelseType(), ref.getBehandlingType());
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
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldinger(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
            Optional<LocalDateTime> ventefristManglendeIM = finnVentefristTilManglendeInntektsmelding(ref);
            return ventefristManglendeIM
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt());
        }
        return KompletthetResultat.oppfylt();
    }

    private void loggManglendeInntektsmeldinger(Long behandlingId, @SuppressWarnings("unused") List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        LOGGER.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere", behandlingId); // NOSONAR //$NON-NLS-1$
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        List<ManglendeVedlegg> manglendeVedlegg = new ArrayList<>();
        manglendeVedlegg.addAll(getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldingerFraGrunnlag(ref));
        return manglendeVedlegg.isEmpty();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        List<ManglendeVedlegg> manglendeVedlegg = new ArrayList<>();
        manglendeVedlegg.addAll(getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldingerFraGrunnlag(ref));
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
        Long behandlingId = ref.getBehandlingId();

        if (finnVentefristForEtterlysning(ref).isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra KOARB (flere ganger) som setter autopunkt 7030 + fra KompletthetsKontroller (dokument på åpen behandling, hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldingerFraGrunnlag(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(behandlingId, manglendeInntektsmeldinger);
            sendEtterlysningForManglendeInntektsmeldinger(ref, manglendeInntektsmeldinger);
            return finnVentefristForEtterlysning(ref)
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.oppfylt()); // Konvensjon for å sikre framdrift i prosessen
        }
        return KompletthetResultat.oppfylt();
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

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref) {
        LocalDate stp = ref.getUtledetSkjæringstidspunkt();
        final LocalDate muligFrist = stp.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        return fellesUtil.finnVentefrist(muligFrist);
    }

    private Optional<LocalDateTime> finnVentefristForEtterlysning(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        LocalDate permisjonsstart = ref.getUtledetSkjæringstidspunkt();
        final LocalDate muligFrist = LocalDate.now().isBefore(permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER)) ? LocalDate.now()
            : permisjonsstart.minusWeeks(TIDLIGST_VENTEFRIST_FØR_UTTAKSDATO_UKER);
        final Optional<LocalDate> annenMuligFrist = søknadRepository.hentSøknadHvisEksisterer(behandlingId).map(s -> s.getMottattDato().plusWeeks(VENTEFRIST_ETTER_MOTATT_DATO_UKER));
        final LocalDate ønsketFrist = annenMuligFrist.filter(muligFrist::isBefore).orElse(muligFrist);
        return fellesUtil.finnVentefrist(ønsketFrist.plusWeeks(VENTEFRIST_ETTER_ETTERLYSNING_UKER));
    }
}
