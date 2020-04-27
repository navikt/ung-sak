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
import no.nav.k9.sak.mottak.kompletthet.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthet.sjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.mottak.kompletthet.sjekk.KompletthetssjekkerSøknad;

@ApplicationScoped
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerKompletthetsjekker implements Kompletthetsjekker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmsorgspengerKompletthetsjekker.class);
    public static final int ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING = 3;

    private Instance<KompletthetssjekkerSøknad> kompletthetssjekkerSøknad;
    private Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BehandlingRepository behandlingRepository;
    private KompletthetsjekkerFelles fellesUtil;

    OmsorgspengerKompletthetsjekker() {
        // CDI
    }

    @Inject
    public OmsorgspengerKompletthetsjekker(@Any Instance<KompletthetssjekkerSøknad> kompletthetssjekkerSøknad,
                                           @Any Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                           BehandlingRepository behandlingRepository,
                                           KompletthetsjekkerFelles fellesUtil) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fellesUtil = fellesUtil;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        if (!getKomplethetsjekker(ref).erSøknadMottatt(ref)) {
            // Litt implisitt forutsetning her, men denne sjekken skal bare ha bli kalt dersom søknad eller IM er mottatt
            LOGGER.info("Behandling {} er ikke komplett - søknad er ikke mottatt", ref.getBehandlingId()); // NOSONAR //$NON-NLS-1$
            return KompletthetResultat.ikkeOppfylt(fellesUtil.finnVentefristTilManglendeSøknad(), Venteårsak.AVV_DOK);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDateTime> forTidligFrist = getKomplethetsjekker(ref).erSøknadMottattForTidlig(ref);
        if (forTidligFrist.isPresent()) {
            return KompletthetResultat.ikkeOppfylt(forTidligFrist.get(), Venteårsak.FOR_TIDLIG_SOKNAD);
        }
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
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldinger(ref);
        if (!manglendeInntektsmeldinger.isEmpty()) {
            loggManglendeInntektsmeldinger(ref.getBehandlingId(), manglendeInntektsmeldinger);
            Optional<LocalDateTime> ventefristManglendeIM = finnVentefristTilManglendeInntektsmelding(ref);
            return ventefristManglendeIM
                .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
                .orElse(KompletthetResultat.fristUtløpt()); // Setter til oppfylt om fristen er passert
        }
        return KompletthetResultat.oppfylt();
    }

    private void loggManglendeInntektsmeldinger(Long behandlingId, List<ManglendeVedlegg> manglendeInntektsmeldinger) {
        String arbgivere = manglendeInntektsmeldinger.stream().map(ManglendeVedlegg::getArbeidsgiver).collect(Collectors.toList()).toString();
        LOGGER.info("Behandling {} er ikke komplett - mangler IM fra arbeidsgivere: {}", behandlingId, arbgivere); // NOSONAR //$NON-NLS-1$
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true;
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

    private Optional<LocalDateTime> finnVentefristTilManglendeInntektsmelding(BehandlingReferanse ref) {
        // TODO: Er dette "fornuftig" / godt nok?
        var muligFrist = behandlingRepository.hentBehandling(ref.getBehandlingId())
            .getOpprettetTidspunkt()
            .toLocalDate()
            .plusDays(ANTALL_DAGER_VENTER_PÅ_INNTEKTSMELDING);
        return fellesUtil.finnVentefrist(muligFrist);
    }

    private KompletthetssjekkerSøknad getKomplethetsjekker(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.get(KompletthetssjekkerSøknad.class, kompletthetssjekkerSøknad, ref.getFagsakYtelseType(), ref.getBehandlingType());
    }

    private KompletthetssjekkerInntektsmelding getKompletthetsjekkerInntektsmelding(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.get(KompletthetssjekkerInntektsmelding.class, kompletthetssjekkerInntektsmelding, ref.getFagsakYtelseType(), ref.getBehandlingType());
    }
}
