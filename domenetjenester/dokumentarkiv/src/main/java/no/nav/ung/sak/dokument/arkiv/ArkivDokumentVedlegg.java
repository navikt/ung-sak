package no.nav.ung.sak.dokument.arkiv;

import java.util.Objects;

/*
 * Til bruk for journalposter der hoveddokument er ett scannet dokument som inneholder både hoveddokument og vedlegg
 */
public class ArkivDokumentVedlegg {
    private String tittel;

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArkivDokumentVedlegg that = (ArkivDokumentVedlegg) o;
        return Objects.equals(tittel, that.tittel);
    }

    @Override
    public int hashCode() {

        return Objects.hash(tittel);
    }

    public static class Builder {
        private final ArkivDokumentVedlegg arkivDokumentVedlegg;

        private Builder() {
            this.arkivDokumentVedlegg = new ArkivDokumentVedlegg();
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medTittel(String tittel) {
            this.arkivDokumentVedlegg.setTittel(tittel);
            return this;
        }

        public ArkivDokumentVedlegg build() {
            return this.arkivDokumentVedlegg;
        }

    }
}
