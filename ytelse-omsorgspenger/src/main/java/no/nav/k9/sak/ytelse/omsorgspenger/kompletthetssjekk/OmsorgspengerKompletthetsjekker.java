package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.inntektsmelding.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;

@ApplicationScoped
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerKompletthetsjekker implements Kompletthetsjekker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmsorgspengerKompletthetsjekker.class);
    public static final int ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING = 3;

    private Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BehandlingRepository behandlingRepository;
    private KompletthetsjekkerFelles fellesUtil;

    OmsorgspengerKompletthetsjekker() {
        // CDI
    }

    @Inject
    public OmsorgspengerKompletthetsjekker(@Any Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                           BehandlingRepository behandlingRepository,
                                           KompletthetsjekkerFelles fellesUtil) {
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fellesUtil = fellesUtil;
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
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt());
        if (!manglendeInntektsmeldinger.isEmpty()) {
            LOGGER.info("Behandling {} er ikke komplett - IM fra {} arbeidsgivere.", ref.getBehandlingId(), manglendeInntektsmeldinger.size(), manglendeInntektsmeldinger.size()); // NOSONAR //$NON-NLS-1$
            Optional<LocalDateTime> ventefristManglendeIM = finnVentefristTilManglendeInntektsmelding(ref);
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
        manglendeVedlegg.addAll(getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldingerFraGrunnlag(ref, ref.getUtledetSkjæringstidspunkt()));
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

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref) {
        // TODO: Er dette "fornuftig" / godt nok?
        var muligFrist = behandlingRepository.hentBehandling(ref.getBehandlingId())
            .getOpprettetTidspunkt()
            .toLocalDate()
            .plusDays(ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING);
        return fellesUtil.finnVentefrist(muligFrist);
    }

    private KompletthetssjekkerInntektsmelding getKompletthetsjekkerInntektsmelding(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.get(KompletthetssjekkerInntektsmelding.class, kompletthetssjekkerInntektsmelding, ref.getFagsakYtelseType(), ref.getBehandlingType());
    }
}
