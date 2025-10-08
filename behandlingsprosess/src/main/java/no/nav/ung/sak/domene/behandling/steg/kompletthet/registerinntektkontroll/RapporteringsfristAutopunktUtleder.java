package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.ytelse.kontroll.RelevanteKontrollperioderUtleder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Dependent
public class RapporteringsfristAutopunktUtleder {

    private RelevanteKontrollperioderUtleder kontrollperioderUtleder;
    private final int inntektskontrollDagIMåned;

    @Inject
    public RapporteringsfristAutopunktUtleder(RelevanteKontrollperioderUtleder kontrollperioderUtleder, @KonfigVerdi(value = "INNTEKTSKONTROLL_DAG_I_MAANED", defaultVerdi = "8") int inntektskontrollDagIMåned) {
        this.kontrollperioderUtleder = kontrollperioderUtleder;
        this.inntektskontrollDagIMåned = inntektskontrollDagIMåned;
    }

    public Optional<AksjonspunktResultat> utledAutopunktForRapporteringsfrist(BehandlingReferanse behandlingReferanse) {
        var årsakTidslinje = kontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandlingReferanse.getBehandlingId(), Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT));


        final var ikkePassertRapporteringsfristTidslinje = årsakTidslinje.filterValue(it -> !it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        var harIkkePassertRapporteringsfrist = !ikkePassertRapporteringsfristTidslinje.isEmpty();
        if (harIkkePassertRapporteringsfrist) {
            // Dersom vi ikkje har passert rapporteringsfrist (ikkje har kontroll-årsak) så skal vi vente til rapporteringsfrist
            final var sisteDatoForRapportertInntekt = ikkePassertRapporteringsfristTidslinje.getMaxLocalDate();
            // Ønker å sette på vent til vi har fått årsak RE_KONTROLL_REGISTER_INNTEKT, første gjenopptagelse skjer samme dag som inntektskontroll
            LocalDateTime venteFrist = sisteDatoForRapportertInntekt.plusMonths(1).withDayOfMonth(inntektskontrollDagIMåned).atStartOfDay();
            return Optional.of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_RAPPORTERINGSFRIST,
                Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST,
                venteFrist.isBefore(LocalDateTime.now()) ? LocalDateTime.now().plusDays(1) : venteFrist));
        }

        return Optional.empty();

    }

}
