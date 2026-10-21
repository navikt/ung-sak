package no.nav.ung.kodeverk.behandling;

import java.util.Optional;

public final class BehandlingStegDel {

    private BehandlingStegDel() {
    }

    public static Optional<BehandlingDel> forAktivitetspenger(BehandlingStegType steg) {
        return switch (steg) {
            case START_STEG -> Optional.of(BehandlingDel.SENTRAL);
            case INIT_PERIODER -> Optional.of(BehandlingDel.SENTRAL);
            case INIT_VILKÅR -> Optional.of(BehandlingDel.SENTRAL);
            case INNHENT_REGISTEROPP -> Optional.of(BehandlingDel.SENTRAL);
            case VURDER_SØKNADSFRIST -> Optional.of(BehandlingDel.SENTRAL);
            case ALDERSVILKÅRET -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_FAKTA_OM_BOSTED -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_BOSTEDVILKÅR -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_ANDRE_LIVSOPPHOLDSYTELSER -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_FAKTA_OM_BISTAND -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_BISTANDSVILKÅR -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_AKTIVITETSVILKÅR -> Optional.of(BehandlingDel.NAV_KONTOR);
            case NAV_KONTOR_FORESLÅ_VILKÅR -> Optional.of(BehandlingDel.NAV_KONTOR);
            case NAV_KONTOR_BESLUTTER_VILKÅR -> Optional.of(BehandlingDel.NAV_KONTOR);
            case VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR -> Optional.of(BehandlingDel.SENTRAL);
            case FORESLÅ_BEHANDLINGSRESULTAT -> Optional.of(BehandlingDel.SENTRAL);
            case AKTIVITETSPENGER_BEREGNING -> Optional.of(BehandlingDel.SENTRAL);
            case VURDER_KOMPLETTHET -> Optional.of(BehandlingDel.SENTRAL);
            case KONTROLLER_REGISTER_INNTEKT -> Optional.of(BehandlingDel.SENTRAL);
            case BEREGN_YTELSE -> Optional.of(BehandlingDel.SENTRAL);
            case SIMULER_OPPDRAG -> Optional.of(BehandlingDel.SENTRAL);
            case FORESLÅ_VEDTAK -> Optional.of(BehandlingDel.SENTRAL);
            case FATTE_VEDTAK -> Optional.of(BehandlingDel.SENTRAL);
            case IVERKSETT_VEDTAK -> Optional.of(BehandlingDel.SENTRAL);

            // Steg som ikke inngår i prosessen for aktivitetspenger
            // Klage
            case VURDER_FORMKRAV_KLAGE_FØRSTEINSTANS -> Optional.empty();
            case VURDER_KLAGE_FØRSTEINSTANS -> Optional.empty();
            case OVERFØRT_NK -> Optional.empty();

            //Ungdomsprogramytelse eller klage
            case KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT -> Optional.empty();
            case KONTROLLER_UNGDOMSPROGRAM -> Optional.empty();
            case VURDER_TILBAKETREKK -> Optional.empty();
            case VURDER_UTTAK -> Optional.empty();
            case VURDER_UNGDOMSPROGRAMVILKÅR -> Optional.empty();
            case UNGDOMSYTELSE_BEREGNING -> Optional.empty();

        };
    }
}
