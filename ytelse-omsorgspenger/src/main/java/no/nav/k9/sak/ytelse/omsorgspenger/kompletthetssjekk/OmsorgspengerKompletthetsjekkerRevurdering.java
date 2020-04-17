package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.kompletthet.KompletthetssjekkerSøknad;
import no.nav.k9.sak.mottak.kompletthet.sjekk.KompletthetsjekkerFelles;

@ApplicationScoped
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerKompletthetsjekkerRevurdering implements Kompletthetsjekker {

    private KompletthetssjekkerSøknad kompletthetssjekkerSøknad;
    private KompletthetsjekkerFelles fellesUtil;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SøknadRepository søknadRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    OmsorgspengerKompletthetsjekkerRevurdering() {
        // CDI
    }

    @Inject
    public OmsorgspengerKompletthetsjekkerRevurdering(@FagsakYtelseTypeRef @BehandlingTypeRef("BT-004") KompletthetssjekkerSøknad kompletthetssjekkerSøknad,
                                                      KompletthetsjekkerFelles fellesUtil,
                                                      InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                                      SøknadRepository søknadRepository,
                                                      BehandlingVedtakRepository behandlingVedtakRepository) {
        this.kompletthetssjekkerSøknad = kompletthetssjekkerSøknad;
        this.fellesUtil = fellesUtil;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.søknadRepository = søknadRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        // Ikke relevant for revurdering - denne kontrollen er allerede håndtert av førstegangsbehandlingen
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        // Ikke relevant for revurdering - denne kontrollen er allerede håndtert av førstegangsbehandlingen
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        Behandling behandling = fellesUtil.hentBehandling(ref.getBehandlingId());
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            return KompletthetResultat.oppfylt();
        }

        if (endringssøknadErMottatt(behandling) && endringssøknadIkkeErKomplett(ref)) {
            return opprettKompletthetResultatMedVentefrist(ref.getBehandlingId());
        }

        // Når endringssøknad ikke er mottatt har vi ikke noe å sjekke kompletthet mot
        // og behandlingen slippes igjennom. Dette gjelder ved fødselshendelse og inntektsmelding.
        return KompletthetResultat.oppfylt();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        return kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
        return kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref);
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return inntektsmeldingTjeneste
            .hentAlleInntektsmeldingerSomIkkeKommer(ref.getBehandlingId())
            .stream()
            .map(e -> new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, e.getArbeidsgiver().getIdentifikator(), true))
            .collect(Collectors.toList());
    }

    private boolean endringssøknadErMottatt(Behandling behandling) {
        LocalDate vedtaksdato = behandlingVedtakRepository.hentBehandlingVedtakFraRevurderingensOriginaleBehandling(behandling).getVedtaksdato();
        Optional<SøknadEntitet> søknadOptional = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        return søknadOptional.isPresent() && søknadOptional.get().erEndringssøknad() && !søknadOptional.get().getMottattDato().isBefore(vedtaksdato);
    }

    private boolean endringssøknadIkkeErKomplett(BehandlingReferanse ref) {
        return !kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(ref).isEmpty();
    }

    private KompletthetResultat opprettKompletthetResultatMedVentefrist(Long behandlingId) {
        Optional<LocalDateTime> ventefristTidligMottattSøknad = fellesUtil.finnVentefristTilForTidligMottattSøknad(behandlingId);
        return ventefristTidligMottattSøknad
            .map(frist -> KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK))
            .orElse(KompletthetResultat.fristUtløpt());
    }
}
