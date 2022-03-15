package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.inntektsmelding.KompletthetssjekkerInntektsmelding;
import no.nav.k9.sak.mottak.kompletthetssjekk.KompletthetsjekkerFelles;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerKompletthetsjekker implements Kompletthetsjekker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmsorgspengerKompletthetsjekker.class);

    private Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BehandlingRepository behandlingRepository;
    private KompletthetsjekkerFelles fellesUtil;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    OmsorgspengerKompletthetsjekker() {
        // CDI
    }

    @Inject
    public OmsorgspengerKompletthetsjekker(@Any Instance<KompletthetssjekkerInntektsmelding> kompletthetssjekkerInntektsmelding,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                           BehandlingRepository behandlingRepository,
                                           KompletthetsjekkerFelles fellesUtil,
                                           OmsorgspengerGrunnlagRepository grunnlagRepository,
                                           MottatteDokumentRepository mottatteDokumentRepository, KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.kompletthetssjekkerInntektsmelding = kompletthetssjekkerInntektsmelding;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fellesUtil = fellesUtil;
        this.grunnlagRepository = grunnlagRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        if (behandling.erManueltOpprettet() && behandling.erRevurdering()) {
            // Ikke nødvendig å vente på dokument-kompletthet når saksbehandler oppretter. Vil stoppe opp i steg Foreslå vedtak.
            return KompletthetResultat.oppfylt();
        }
        if (BehandlingStatus.OPPRETTET.equals(ref.getBehandlingStatus())) {
            return KompletthetResultat.oppfylt();
        }
        // Kalles fra VurderKompletthetSteg (en gang) som setter autopunkt 7003 + fra KompletthetsKontroller (dokument på åpen behandling,
        // hendelser)
        // KompletthetsKontroller vil ikke røre åpne autopunkt, men kan ellers sette på vent med 7009.
        List<ManglendeVedlegg> manglendeInntektsmeldinger = getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt());
        if (!manglendeInntektsmeldinger.isEmpty()) {
            if (harSøknadSomArbeidstaker(ref)) {
                LOGGER.info("Behandling {} er ikke komplett - Søknad arbeidstaker uten tilhørende IM", ref.getBehandlingId());
                return settPåVent(ref, Venteårsak.AVV_IM_MOT_SØKNAD_AT, 14);
            } else {
                LOGGER.info("Behandling {} er ikke komplett - IM fra {} arbeidsgivere.", ref.getBehandlingId(), manglendeInntektsmeldinger.size());
                return settPåVent(ref, Venteårsak.AVV_IM_MOT_AAREG, 3);
            }
        }
        if (ingenSøknadsperioder(ref)) {
            // Gjelder både behandlinger som er førstegangs og som er forlengelse
            LOGGER.info("Behandling {} er ikke komplett - Ingen IM eller søknad har sendt kravperioder.", ref.getBehandlingId());
            return settPåVent(ref, Venteårsak.AVV_SØKNADSPERIODER, 28);
        }
        return KompletthetResultat.oppfylt();
    }

    @Override
    public boolean ingenSøknadsperioder(BehandlingReferanse ref) {
        return harIngenRefusjonskravFraMottatteInntektsmeldinger(ref) && harIkkeSøknadsperiode(ref);
    }

    private boolean harSøknadSomArbeidstaker(BehandlingReferanse ref) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> it.getBehandlingId() != null && it.getBehandlingId().equals(ref.getBehandlingId()))
            .anyMatch(it -> Brevkode.SØKNAD_UTBETALING_OMS_AT.equals(it.getType()));
    }

    private boolean harIkkeSøknadsperiode(BehandlingReferanse ref) {
        return grunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(ref.getBehandlingId()).isEmpty();
    }

    private KompletthetResultat settPåVent(BehandlingReferanse ref, Venteårsak venteårsak, int antallVentedager) {
        var muligFrist = behandlingRepository.hentBehandling(ref.getBehandlingId())
            .getOpprettetTidspunkt()
            .toLocalDate()
            .plusDays(antallVentedager);

        return fellesUtil.finnVentefrist(muligFrist)
            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, venteårsak))
            .orElse(KompletthetResultat.fristUtløpt());
    }

    private boolean harIngenRefusjonskravFraMottatteInntektsmeldinger(BehandlingReferanse ref) {
        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt());
        return inntektsmeldinger.stream()
            .allMatch(im -> !im.harRefusjonskrav());
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return true;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        if (ref.erRevurdering()) {
            return List.of();
        }
        List<ManglendeVedlegg> manglendeVedlegg = new ArrayList<>();
        manglendeVedlegg.addAll(getKompletthetsjekkerInntektsmelding(ref).utledManglendeInntektsmeldingerFraGrunnlag(ref, ref.getUtledetSkjæringstidspunkt()));
        return manglendeVedlegg;
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste
            .hentAlleInntektsmeldingerSomIkkeKommer(ref.getBehandlingId())
            .stream()
            .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver(), true))
            .collect(Collectors.toList());
    }

    @Override
    public Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledAlleManglendeVedleggForPerioder(BehandlingReferanse ref) {
        return kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(ref);
    }

    private KompletthetssjekkerInntektsmelding getKompletthetsjekkerInntektsmelding(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.get(KompletthetssjekkerInntektsmelding.class, kompletthetssjekkerInntektsmelding, ref.getFagsakYtelseType(), ref.getBehandlingType());
    }
}
