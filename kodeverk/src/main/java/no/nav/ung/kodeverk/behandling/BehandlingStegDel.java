package no.nav.ung.kodeverk.behandling;

public final class BehandlingStegDel {

    private BehandlingStegDel() {
    }

    public static BehandlingDel forAktivitetspenger(BehandlingStegType steg) {
        return switch (steg) {
            case START_STEG -> BehandlingDel.SENTRAL;
            case INIT_PERIODER -> BehandlingDel.SENTRAL;
            case INIT_VILKÅR -> BehandlingDel.SENTRAL;
            case INNHENT_REGISTEROPP -> BehandlingDel.SENTRAL;
            case VURDER_SØKNADSFRIST -> BehandlingDel.SENTRAL;
            case ALDERSVILKÅRET -> BehandlingDel.NAV_KONTOR;
            case VURDER_FAKTA_OM_BOSTED -> BehandlingDel.NAV_KONTOR;
            case VURDER_BOSTEDVILKÅR -> BehandlingDel.NAV_KONTOR;
            case VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER -> BehandlingDel.NAV_KONTOR;
            case VURDER_ANDRE_LIVSOPPHOLDSYTELSER -> BehandlingDel.NAV_KONTOR;
            case VURDER_FAKTA_OM_BISTAND -> BehandlingDel.NAV_KONTOR;
            case VURDER_BISTANDSVILKÅR -> BehandlingDel.NAV_KONTOR;
            case VURDER_AKTIVITETSVILKÅR -> BehandlingDel.NAV_KONTOR;
            case NAV_KONTOR_FORESLÅ_VILKÅR -> BehandlingDel.NAV_KONTOR;
            case NAV_KONTOR_BESLUTTER_VILKÅR -> BehandlingDel.NAV_KONTOR;
            case VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR -> BehandlingDel.SENTRAL;
            case FORESLÅ_BEHANDLINGSRESULTAT -> BehandlingDel.SENTRAL;
            case AKTIVITETSPENGER_BEREGNING -> BehandlingDel.SENTRAL;
            case VURDER_KOMPLETTHET -> BehandlingDel.SENTRAL;
            case KONTROLLER_REGISTER_INNTEKT -> BehandlingDel.SENTRAL;
            case BEREGN_YTELSE -> BehandlingDel.SENTRAL;
            case SIMULER_OPPDRAG -> BehandlingDel.SENTRAL;
            case FORESLÅ_VEDTAK -> BehandlingDel.SENTRAL;
            case FATTE_VEDTAK -> BehandlingDel.SENTRAL;
            case IVERKSETT_VEDTAK -> BehandlingDel.SENTRAL;

            case VURDER_FORMKRAV_KLAGE_FØRSTEINSTANS -> BehandlingDel.SENTRAL;
            case VURDER_KLAGE_FØRSTEINSTANS -> BehandlingDel.SENTRAL;
            case OVERFØRT_NK -> BehandlingDel.SENTRAL;

            case KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
                 KONTROLLER_UNGDOMSPROGRAM,
                 VURDER_TILBAKETREKK,
                 VURDER_UTTAK,
                 VURDER_UNGDOMSPROGRAMVILKÅR,
                 UNGDOMSYTELSE_BEREGNING -> throw new IllegalArgumentException("Steg " + steg + " inngår ikke i aktivitetspenger");
        };
    }
}
