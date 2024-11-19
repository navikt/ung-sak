package no.nav.ung.sak.behandling.prosessering;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/**
 * Grensesnitt for å kjøre behandlingsprosess, herunder gjenopptak, registeroppdatering, koordinering av sakskompleks mv.
 * Alle kall til utføringsmetode i behandlingskontroll bør gå gjennom tasks opprettet her.
 * Merk Dem:
 *   - ta av vent og grunnlagsoppdatering kan føre til reposisjonering av behandling til annet steg
 *   - grunnlag endres ved ankomst av dokument, ved registerinnhenting og ved senere overstyring ("bekreft AP" eller egne overstyringAP)
 *   - Hendelser: Ny behandling (Manuell, dokument, mv), Gjenopptak (Manuell/Frist), Interaktiv (Oppdater/Fortsett), Dokument, Datahendelse, Vedtak, KØ-hendelser
 **/
public interface BehandlingProsesseringTjeneste {

    // Støttefunksjon for å vurdere behov for interaktiv oppdatering, ref invalid-at-midnight
    boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling);

    // Støttefunksjon for å sørge for at registerdata vil bli oppdatert, gjør ikke oppdatering
    void tvingInnhentingRegisteropplysninger(Behandling behandling);

    /** Innhenter registerdata hvis utdatert. */
    ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling);

    /** Returnerer tasks for oppdatering/fortsett for bruk med BehandlingskontrollAsynkTjeneste. Blir ikke lagret her */
    ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling, boolean forceInnhent);

    // Til bruk for å kjøre behandlingsprosessen videre. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForFortsettBehandling(Behandling behandling);
    String opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType, LocalDateTime nesteKjøringEtter);
    String opprettTasksForÅHoppeTilbakeTilGittStegOgFortsettDerfra(Behandling behandling, BehandlingStegType behandlingStegType);

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand) (Hendelse: Manuell input, Frist utløpt, mv)
    // NB oppdaterer registerdata Lagrer tasks
    void opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId);

    void opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId, boolean forceregisterinnhenting);

    ProsessTaskGruppe opprettTaskGruppeForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId, boolean skalUtledeÅrsaker);

    ProsessTaskGruppe opprettTaskGruppeForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId, boolean skalUtledeÅrsaker, boolean forceregisterinnhenting);

    void opprettTasksForInitiellRegisterInnhenting(Behandling behandling);

    List<String> utledRegisterinnhentingTaskTyper(Behandling behandling);

}
