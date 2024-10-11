package no.nav.k9.sak.web.app.jackson;

public enum SakKodeverkOverstyringSerialisering {
    OBJEKT_UTEN_NAVN,
    OBJEKT_MED_NAVN,
    KODE_STRING,
    INGEN;  // <- Ingen overstyring, samme som har vore for REST endepunkt.
}
