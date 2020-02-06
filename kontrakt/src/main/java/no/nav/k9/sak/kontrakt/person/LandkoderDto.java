package no.nav.k9.sak.kontrakt.person;

public class LandkoderDto {

    private String kode;
    private String kodeverk;
    private String navn;

    public LandkoderDto() {
        // trengs for deserialisering av JSON
    }

    public String getKode() {
        return kode;
    }

    public String getKodeverk() {
        return kodeverk;
    }

    public String getNavn() {
        return navn;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    public void setKodeverk(String kodeverk) {
        this.kodeverk = kodeverk;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }
}
