package no.nav.ung.sak.behandling.revurdering;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

/**
 * Avgjør om kontroll av inntekt og inntektsrapportering skal behandles i kontrollbehandling eller i ordinær behandling.
 * <ol>
 *     <li>Har fagsaken en kontrollbehandling som ikke er avsluttet, går kontrollen dit. Det gjelder også når bryteren er av,
 *     slik at åpne kontrollbehandlinger blir fullført.</li>
 *     <li>Er bryteren på og ingen åpen ordinær behandling kontrollerer inntekt, går kontrollen til kontrollbehandling.</li>
 *     <li>Ellers brukes ordinær behandling som før. Det gjelder også en åpen ordinær behandling som kontrollerer inntekt
 *     når bryteren slås på. Den fullføres i gammel flyt.</li>
 * </ol>
 * En ordinær behandling kontrollerer inntekt når behandlingsårsakene eller prosesstriggerne har kontroll av registerinntekt
 * eller inntektsrapportering. Rapportert inntekt lagres på behandlingen som mottar rapporteringen. Går kontrollen til en
 * annen behandling enn rapporteringen, mangler kontrollen rapportert inntekt, og deltakeren kan få varsel om avvik som ikke finnes.
 * <p>
 * Bare kontroll av registerinntekt rutes. Har oppgaven andre årsaker i tillegg, brukes ordinær behandling.
 * <p>
 * Kallet må skje under fagsaklåsen, slik at svaret ikke endres før kontrollen er lagt til eller kontrollbehandlingen er opprettet.
 */
@ApplicationScoped
public class KontrollbehandlingRuting {

    private static final BehandlingÅrsakType KONTROLLÅRSAK = BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT;

    private static final Set<BehandlingÅrsakType> KONTROLLÅRSAKER_I_ORDINÆR_BEHANDLING = Set.of(
        BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT,
        BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT);

    private BehandlingRepository behandlingRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private KontrollbehandlingBryter kontrollbehandlingBryter;

    KontrollbehandlingRuting() {
        // for CDI proxy
    }

    @Inject
    public KontrollbehandlingRuting(BehandlingRepository behandlingRepository,
                                    ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                    ProsessTriggereRepository prosessTriggereRepository,
                                    KontrollbehandlingBryter kontrollbehandlingBryter) {
        this.behandlingRepository = behandlingRepository;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.kontrollbehandlingBryter = kontrollbehandlingBryter;
    }

    public boolean skalTilKontrollbehandling(Fagsak fagsak, Set<BehandlingÅrsakType> årsaker) {
        if (årsaker.isEmpty() || !årsaker.stream().allMatch(KONTROLLÅRSAK::equals)) {
            return false;
        }
        if (harIkkeAvsluttetKontrollbehandling(fagsak)) {
            return true;
        }
        if (!kontrollbehandlingBryter.erPå(fagsak.getYtelseType())) {
            return false;
        }
        return !harÅpenOrdinærBehandlingSomKontrollererInntekt(fagsak);
    }

    /**
     * Inntektsrapportering følger kontrollen. Uten vedtak på fagsaken går rapporteringen til ordinær behandling, fordi
     * kontrollbehandling bygger på siste vedtak.
     */
    public boolean skalInntektsrapporteringTilKontrollbehandling(Fagsak fagsak) {
        if (harIkkeAvsluttetKontrollbehandling(fagsak)) {
            return true;
        }
        if (!kontrollbehandlingBryter.erPå(fagsak.getYtelseType())) {
            return false;
        }
        if (behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsak.getId()).isEmpty()) {
            return false;
        }
        return !harÅpenOrdinærBehandlingSomKontrollererInntekt(fagsak);
    }

    private boolean harIkkeAvsluttetKontrollbehandling(Fagsak fagsak) {
        return parallelleBehandlingerRepository.finnIkkeAvsluttetKontrollbehandling(fagsak.getId()).isPresent();
    }

    private boolean harÅpenOrdinærBehandlingSomKontrollererInntekt(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsak.getId(), BehandlingType.getOrdinæreYtelseBehandlingTyper()).stream()
            .map(behandlingRepository::hentBehandling)
            .anyMatch(this::kontrollererInntekt);
    }

    private boolean kontrollererInntekt(Behandling behandling) {
        var triggerårsaker = prosessTriggereRepository.hentGrunnlag(behandling.getId()).stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Set::stream)
            .map(Trigger::getÅrsak);
        return Stream.concat(behandling.getBehandlingÅrsakerTyper().stream(), triggerårsaker)
            .anyMatch(KONTROLLÅRSAKER_I_ORDINÆR_BEHANDLING::contains);
    }
}
