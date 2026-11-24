package no.nav.ung.sak.behandling.revurdering;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

/**
 * Avgjør om kontroll av inntekt skal behandles i kontrollbehandling eller i ordinær behandling.
 * <ol>
 *     <li>Har fagsaken en kontrollbehandling som ikke er avsluttet, går kontrollen dit. Det gjelder også når bryteren er av,
 *     slik at åpne kontrollbehandlinger blir fullført.</li>
 *     <li>Er bryteren på og ingen åpen ordinær behandling har kontrollårsak, går kontrollen til kontrollbehandling.</li>
 *     <li>Ellers brukes ordinær behandling som før. Det gjelder også en åpen ordinær behandling med kontrollårsak, som
 *     fullføres i gammel flyt når bryteren slås på.</li>
 * </ol>
 * Bare kontroll av registerinntekt rutes. Har oppgaven andre årsaker i tillegg, brukes ordinær behandling.
 * <p>
 * Kallet må skje under fagsaklåsen, slik at svaret ikke endres før kontrollen er lagt til eller kontrollbehandlingen er opprettet.
 */
@ApplicationScoped
public class KontrollbehandlingRuting {

    private static final BehandlingÅrsakType KONTROLLÅRSAK = BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT;

    private BehandlingRepository behandlingRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private KontrollbehandlingBryter kontrollbehandlingBryter;

    KontrollbehandlingRuting() {
        // for CDI proxy
    }

    @Inject
    public KontrollbehandlingRuting(BehandlingRepository behandlingRepository,
                                    ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                    KontrollbehandlingBryter kontrollbehandlingBryter) {
        this.behandlingRepository = behandlingRepository;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.kontrollbehandlingBryter = kontrollbehandlingBryter;
    }

    public boolean skalTilKontrollbehandling(Fagsak fagsak, Set<BehandlingÅrsakType> årsaker) {
        if (årsaker.isEmpty() || !årsaker.stream().allMatch(KONTROLLÅRSAK::equals)) {
            return false;
        }
        if (parallelleBehandlingerRepository.finnIkkeAvsluttetKontrollbehandling(fagsak.getId()).isPresent()) {
            return true;
        }
        if (!kontrollbehandlingBryter.erPå(fagsak.getYtelseType())) {
            return false;
        }
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsak.getId(), BehandlingType.getOrdinæreYtelseBehandlingTyper()).stream()
            .map(behandlingRepository::hentBehandling)
            .noneMatch(b -> b.getBehandlingÅrsakerTyper().contains(KONTROLLÅRSAK));
    }
}
