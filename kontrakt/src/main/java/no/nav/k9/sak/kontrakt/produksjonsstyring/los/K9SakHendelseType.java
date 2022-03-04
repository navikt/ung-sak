package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

enum K9SakHendelseType {
    KRAVDOKUMENT,
    AKSJONSPUNKT,
    @Deprecated BEHANDLING,
    BEHANDLING_OPPRETTET,
    BEHANDLING_AVSLUTTET
}
