package no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell;

public enum PersonStatusType {
    BOSA("Bosatt"),
    UTVA("Utvandret"),
    DØD("Død");

    private String kode;

    PersonStatusType(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static PersonStatusType hentPersonStatusType(String kode) {
        for (PersonStatusType personStatusType : values()) {
            if (personStatusType.kode.equals(kode)) {
                return personStatusType;
            }
        }
        return null;
    }
}
