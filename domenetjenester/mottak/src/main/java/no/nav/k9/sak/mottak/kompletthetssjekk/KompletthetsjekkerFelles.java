package no.nav.k9.sak.mottak.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for {@link KompletthetsjekkerImpl}.
 * <p>
 * Favor composition over inheritance
 */
@Dependent
public class KompletthetsjekkerFelles {

    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    public static final Integer VENTEFRIST_FRAM_I_TID_FRA_MOTATT_DATO_UKER = 3;
    public static final Integer VENTEFRIST_FOR_MANGLENDE_SØKNAD = 4;

    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private BehandlingRepository behandlingRepository;

    KompletthetsjekkerFelles() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerFelles(BehandlingRepositoryProvider provider,
                                    DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        this.behandlingRepository = provider.getBehandlingRepository();
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
    }

    public Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(LocalDate.now())) {
            LocalDateTime ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
    }

    public LocalDateTime finnVentefristTilManglendeSøknad() {
        return LocalDateTime.now().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }

    public void sendBrev(Long behandlingId, DokumentMalType dokumentMalType, String årsakskode) {
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, dokumentMalType, null, årsakskode);
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

}
