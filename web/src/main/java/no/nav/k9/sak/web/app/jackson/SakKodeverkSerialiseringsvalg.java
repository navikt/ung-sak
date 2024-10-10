package no.nav.k9.sak.web.app.jackson;

public enum SakKodeverkSerialiseringsvalg {
    OBJEKT_UTEN_NAVN,
    OBJEKT_MED_NAVN,
    KODE_STRING,
    STANDARD;  // <- Ingen overstyring, samme som har vore for REST endepunkt.
}
