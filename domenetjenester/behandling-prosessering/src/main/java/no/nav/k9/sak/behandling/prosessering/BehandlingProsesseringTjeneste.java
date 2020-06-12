package no.nav.k9.sak.behandling.prosessering;

import java.time.LocalDateTime;
import java.util.Set;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

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

    // AV/PÅ Vent
    void taBehandlingAvVent(Behandling behandling);

    void settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon apDef, LocalDateTime fristTid, Venteårsak venteårsak, String venteårsakVariant);

    // For snapshot av grunnlag før man gjør andre endringer enn registerinnhenting
    EndringsresultatSnapshot taSnapshotAvBehandlingsgrunnlag(Behandling behandling);

    // Returnerer endringer i grunnlag mellom snapshot og nåtilstand
    EndringsresultatDiff finnGrunnlagsEndring(Behandling behandling, EndringsresultatSnapshot før);

    // Spole prosessen basert på diff. Til bruk ved grunnlagsendringer utenom register (søknad)
    void reposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff grunnlagDiff);

    /** Returnerer tasks for oppdatering/fortsett for bruk med BehandlingskontrollAsynkTjeneste. Blir ikke lagret her */
    ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling);

    // Til bruk for å kjøre behandlingsprosessen videre. Lagrer tasks. Returnerer gruppe-handle
    String opprettTasksForFortsettBehandling(Behandling behandling);
    String opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType, LocalDateTime nesteKjøringEtter);

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand) (Hendelse: Manuell input, Frist utløpt, mv)
    // NB oppdaterer registerdata Lagrer tasks
    void opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId);

    void opprettTasksForInitiellRegisterInnhenting(Behandling behandling);

    void feilPågåendeTaskHvisFremtidigTaskEksisterer(Behandling behandling, Set<String> set);
}
