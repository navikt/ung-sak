package no.nav.ung.sak.behandlingslager.behandling.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

/**
 * Styrer om inntektskontroll skal kjøres i egen kontrollbehandling, per ytelse.
 * <p>
 * Med bryteren av opprettes ingen kontrollbehandlinger, ventepunktet for vedtaksprioritet står ikke i modellene for
 * førstegangsbehandling og revurdering, og ingen synk opprettes. Åpne kontrollbehandlinger fullføres likevel.
 * <p>
 * Slå bare av bryteren når ingen kontrollbehandling er åpen og ingen behandling står eller venter i ventepunktet for
 * vedtaksprioritet.
 */
@ApplicationScoped
public class KontrollbehandlingBryter {

    private boolean ungdomsytelseEnabled;
    private boolean aktivitetspengerEnabled;

    KontrollbehandlingBryter() {
        // for CDI proxy
    }

    @Inject
    public KontrollbehandlingBryter(@KonfigVerdi(value = "KONTROLLBEHANDLING_UNG_ENABLED", required = false, defaultVerdi = "false") boolean ungdomsytelseEnabled,
                                    @KonfigVerdi(value = "KONTROLLBEHANDLING_AVP_ENABLED", required = false, defaultVerdi = "false") boolean aktivitetspengerEnabled) {
        this.ungdomsytelseEnabled = ungdomsytelseEnabled;
        this.aktivitetspengerEnabled = aktivitetspengerEnabled;
    }

    public boolean erPå(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case UNGDOMSYTELSE -> ungdomsytelseEnabled;
            case AKTIVITETSPENGER -> aktivitetspengerEnabled;
            default -> false;
        };
    }
}
