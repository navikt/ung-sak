package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Sammenligner sett av inntektsmeldinger mellom forrige og gjeldene behandling
 */
@ApplicationScoped
public class HarEndretInntektsmeldingVurderer {

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;

    private boolean brukIdTilInntektsmeldingfiltreringEnabled;


    public HarEndretInntektsmeldingVurderer() {
    }

    @Inject
    public HarEndretInntektsmeldingVurderer(BehandlingRepository behandlingRepository,
                                            MottatteDokumentRepository mottatteDokumentRepository,
                                            @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                            @KonfigVerdi(value = "PSB_FILTRER_IM_PAA_BEHANDLING_ID", defaultVerdi = "false") boolean brukIdTilInntektsmeldingfiltreringEnabled) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.brukIdTilInntektsmeldingfiltreringEnabled = brukIdTilInntektsmeldingfiltreringEnabled;
    }


    public boolean harEndringPåInntektsmeldingerTilBrukForPerioden(BehandlingReferanse referanse,
                                                                   Collection<Inntektsmelding> inntektsmeldinger,
                                                                   DatoIntervallEntitet periode,
                                                                   InntektsmeldingerEndringsvurderer endringsvurderer) {

        var originalBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow());
        var mottatteInntektsmeldinger = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(referanse.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();

        var inntektsmeldingerForrigeVedtak = finnInntektsmeldingerFraForrigeVedtak(referanse, originalBehandling, inntektsmeldinger, mottatteInntektsmeldinger);


        var relevanteInntektsmeldingerForrigeVedtak = utledRelevanteForPeriode(BehandlingReferanse.fra(originalBehandling), inntektsmeldingerForrigeVedtak, periode);
        var relevanteInntektsmeldinger = utledRelevanteForPeriode(referanse, inntektsmeldinger, periode);

        return endringsvurderer.erEndret(relevanteInntektsmeldingerForrigeVedtak, relevanteInntektsmeldinger);
    }

    private List<Inntektsmelding> finnInntektsmeldingerFraForrigeVedtak(BehandlingReferanse referanse, Behandling originalBehandling, Collection<Inntektsmelding> inntektsmeldinger, List<MottattDokument> mottatteInntektsmeldinger) {
        if (brukIdTilInntektsmeldingfiltreringEnabled) {
            return inntektsmeldinger.stream()
                .filter(it -> erInntektsmeldingITidligereBehandling(it, referanse.getBehandlingId(), mottatteInntektsmeldinger))
                .toList();
        }

        return inntektsmeldinger.stream()
            .filter(it -> finnEksaktMottattTidspunkt(it, mottatteInntektsmeldinger).isBefore(originalBehandling.getAvsluttetDato()))
            .toList();
    }

    private List<Inntektsmelding> utledRelevanteForPeriode(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldingBegrenset = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, inntektsmeldinger, periode);
        return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingBegrenset, periode);
    }

    private boolean erInntektsmeldingITidligereBehandling(Inntektsmelding inntektsmelding, Long behandlingId, List<MottattDokument> mottatteInntektsmeldinger) {
        return mottatteInntektsmeldinger.stream()
            .filter(it -> Objects.equals(it.getJournalpostId(), inntektsmelding.getJournalpostId()))
            .anyMatch(md -> md.getBehandlingId() != behandlingId);
    }

    private LocalDateTime finnEksaktMottattTidspunkt(Inntektsmelding inntektsmelding, List<MottattDokument> mottatteInntektsmeldinger) {
        return mottatteInntektsmeldinger.stream()
            .filter(it -> Objects.equals(it.getJournalpostId(), inntektsmelding.getJournalpostId()))
            .findAny()
            .map(MottattDokument::getMottattTidspunkt)
            .orElse(LocalDateTime.now());
    }


    @FunctionalInterface
    public interface InntektsmeldingerEndringsvurderer {

        boolean erEndret(List<Inntektsmelding> gjeldendeInntektsmeldinger, List<Inntektsmelding> inntektsmeldingerForrigeVedtak);

    }


}
