package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallellBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;

/**
 * Regler for vedtaksprioritet. Bare én ytelsesbehandling per fagsak kan ha vedtaksprioritet, det vil si være forbi
 * ventepunktet for vedtaksprioritet rett før {@link BehandlingStegType#BEREGN_YTELSE}. En behandling går bare forbi
 * ventepunktet når den bygger på siste vedtak.
 * <p>
 * Kallestedet må holde fagsaklåsen. Ellers kan svaret endre seg før transaksjonen er ferdig.
 */
@Dependent
public class ParallelleBehandlingerTjeneste {

    /**
     * Referansesteg for om en behandling er forbi ventepunktet for vedtaksprioritet. Ventepunktet står ikke i modellene
     * for førstegangsbehandling og revurdering når bryteren er av, så ventepunktet selv kan ikke brukes i
     * sammenligningen.
     */
    public static final BehandlingStegType FØRSTE_STEG_MED_VEDTAKSPRIORITET = BehandlingStegType.BEREGN_YTELSE;

    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    ParallelleBehandlingerTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ParallelleBehandlingerTjeneste(ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                          BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    /**
     * Finner en annen åpen ytelsesbehandling på fagsaken som er forbi ventepunktet for vedtaksprioritet og dermed har
     * vedtaksprioritet.
     */
    public Optional<ParallellBehandling> finnBehandlingMedVedtaksprioritet(FagsakYtelseType ytelseType, Long fagsakId, Long behandlingId) {
        return parallelleBehandlingerRepository.hentAndreIkkeAvsluttedeYtelsesbehandlinger(fagsakId, behandlingId).stream()
            .filter(b -> erForbiVentepunktForVedtaksprioritet(ytelseType, b))
            .findFirst();
    }

    /**
     * En behandling er forbi ventepunktet for vedtaksprioritet når den står i eller etter
     * {@link #FØRSTE_STEG_MED_VEDTAKSPRIORITET}, eller fatter eller iverksetter vedtak. Henlagte behandlinger har aldri
     * vedtaksprioritet. De hopper fram til iverksetting uten å passere ventepunktet.
     * <p>
     * Sjekken bruker gjeldende modell. En behandling som passerte steget før ventepunktet fantes i modellen, regnes også
     * som forbi.
     */
    public boolean erForbiVentepunktForVedtaksprioritet(FagsakYtelseType ytelseType, ParallellBehandling behandling) {
        if (behandling.erHenlagt()) {
            return false;
        }
        if (behandling.status() == BehandlingStatus.FATTER_VEDTAK || behandling.status() == BehandlingStatus.IVERKSETTER_VEDTAK) {
            return true;
        }
        BehandlingStegType aktivtSteg = behandling.aktivtSteg();
        if (aktivtSteg == null || aktivtSteg == BehandlingStegType.VURDER_PARALLELLE_BEHANDLINGER) {
            return false;
        }
        return behandlingskontrollTjeneste.sammenlignRekkefølge(ytelseType, behandling.behandlingType(), aktivtSteg, FØRSTE_STEG_MED_VEDTAKSPRIORITET) >= 0;
    }

    /**
     * En behandling bygger på siste vedtak når originalbehandlingen er siste avsluttede, ikke-henlagte ytelsesbehandling.
     * <p>
     * Er originalbehandlingen ikke avsluttet ennå, er behandlingen opprettet fra et vedtak under iverksetting. Den bygger
     * da på et nyere vedtak enn siste avsluttede og regnes som synkronisert. Har fagsaken ikke vedtak, er det ingenting å
     * synkronisere mot.
     */
    public boolean byggerPåSisteVedtak(Long fagsakId, Long behandlingId) {
        Optional<Long> sisteVedtak = parallelleBehandlingerRepository.finnSisteAvsluttedeIkkeHenlagteYtelsesbehandlingId(fagsakId);
        if (sisteVedtak.isEmpty()) {
            return true;
        }
        Optional<Long> originalBehandlingId = parallelleBehandlingerRepository.hentOriginalBehandlingId(behandlingId);
        if (originalBehandlingId.isEmpty()) {
            return false;
        }
        if (Objects.equals(originalBehandlingId.get(), sisteVedtak.get())) {
            return true;
        }
        return parallelleBehandlingerRepository.hentStatus(originalBehandlingId.get()) != BehandlingStatus.AVSLUTTET;
    }
}
