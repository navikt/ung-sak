package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.util.Objects;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkKravstillerType;

class DetaljertVilkårUtfall {
    private StønadstatistikkKravstillerType kravstiller;
    private String type;
    private String organisasjonsnummer;
    private String aktørId;
    private String arbeidsforholdId;
    private Utfall utfall;

    private DetaljertVilkårUtfall(Utfall utfall) {
        this.utfall = utfall;
    }

    public static DetaljertVilkårUtfall forKravstiller(Utfall utfall, StønadstatistikkKravstillerType kravstiller) {
        DetaljertVilkårUtfall resultat = new DetaljertVilkårUtfall(utfall);
        resultat.setKravstiller(kravstiller);
        return resultat;
    }

    public static DetaljertVilkårUtfall forArbeidsforhold(Utfall utfall, String type, String organisasjonsnummer, String aktørId, String arbeidsforholdId) {
        DetaljertVilkårUtfall resultat = new DetaljertVilkårUtfall(utfall);
        resultat.setType(type);
        resultat.setOrganisasjonsnummer(organisasjonsnummer);
        resultat.setAktørId(aktørId);
        resultat.setArbeidsforholdId(arbeidsforholdId);
        return resultat;
    }

    public static DetaljertVilkårUtfall forKravstillerPerArbeidsforhold(Utfall utfall, StønadstatistikkKravstillerType kravstiller, String type, String organisasjonsnummer, String aktørId, String arbeidsforholdId) {
        DetaljertVilkårUtfall resultat = new DetaljertVilkårUtfall(utfall);
        resultat.setKravstiller(kravstiller);
        resultat.setType(type);
        resultat.setOrganisasjonsnummer(organisasjonsnummer);
        resultat.setAktørId(aktørId);
        resultat.setArbeidsforholdId(arbeidsforholdId);
        return resultat;
    }

    public StønadstatistikkKravstillerType getKravstiller() {
        return kravstiller;
    }

    public void setKravstiller(StønadstatistikkKravstillerType kravstiller) {
        this.kravstiller = kravstiller;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public void setUtfall(Utfall utfall) {
        this.utfall = utfall;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetaljertVilkårUtfall that = (DetaljertVilkårUtfall) o;
        return kravstiller == that.kravstiller && type == that.type && Objects.equals(organisasjonsnummer, that.organisasjonsnummer) && Objects.equals(aktørId, that.aktørId) && Objects.equals(arbeidsforholdId, that.arbeidsforholdId) && utfall == that.utfall;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kravstiller, type, organisasjonsnummer, aktørId, arbeidsforholdId, utfall);
    }
}
