package no.nav.ung.kodeverk.bosatt;

/**
 * Kilde for bostedsavklaring — sporer hvem som har fastsatt fakta.
 */
public enum Kilde {
    /** Fakta er automatisk satt basert på opplysninger fra brukers søknad. */
    SØKNAD,
    /** Fakta er registrert manuelt av saksbehandler via VURDER_BOSTED-aksjonspunktet. */
    SAKSBEHANDLER
}
