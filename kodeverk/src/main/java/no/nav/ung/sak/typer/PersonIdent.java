package no.nav.ung.sak.typer;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.api.IndexKey;

/**
 * Denne mapper p.t Norsk person ident (fødselsnummer, inkl F-nr, D-nr eller FDAT)
 * <ul>
 * <li>F-nr: http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-2 (F-nr)</li>
 *
 * <li>D-nr: http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-5 (D-nr), samt hvem som kan utstede
 * (http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-6)</li>
 *
 * <li>FDAT: Personer uten FNR. Disse har fødselsdato + 00000 (normalt) eller fødselsdato + 00001 (dødfødt).
 * </ul>
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonIdent implements Comparable<PersonIdent>, IndexKey {

    private static final int[] CHECKSUM_EN_VECTOR = new int[] { 3, 7, 6, 1, 8, 9, 4, 5, 2 };
    private static final int[] CHECKSUM_TO_VECTOR = new int[] { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

    private static final int FNR_LENGDE = 11;
    private static final int AKTOR_ID_LENGDE = 13;

    private static final int PERSONNR_LENGDE = 5;

    @JsonValue
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String ident;

    PersonIdent() {
        //
    }

    @JsonCreator
    public PersonIdent(@NotNull @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String ident) {
        Objects.requireNonNull(ident, "ident kan ikke være null");
        this.ident = ident.trim();
    }

    /**
     * @return true hvis angitt str er et fødselsnummer (F-Nr eller D-Nr). False hvis ikke, eller er FDAT nummer.
     */
    public static boolean erGyldigFnr(final String str) {
        if (str == null) {
            return false;
        }
        String s = str.trim();
        return s.length() == FNR_LENGDE && !isFdatPersonNummer(getPersonnummer(s)) && validerFnrStruktur(s);
    }

    /** Trekker ut 5 siste siffer fra nummer, gitt at det er FNR. */
    private static String getPersonnummer(String str) {
        return (str == null || str.length() < PERSONNR_LENGDE)
            ? null
            : (str.length() > FNR_LENGDE ? null : str.substring(str.length() - PERSONNR_LENGDE, str.length()));
    }

    private static boolean isFdatPersonNummer(String personnummer) {
        return personnummer != null && personnummer.length() == PERSONNR_LENGDE && personnummer.startsWith("0000");
    }

    private static int sum(String foedselsnummer, int... faktors) {
        int sum = 0;
        for (int i = 0, l = faktors.length; i < l; ++i) {
            sum += Character.digit(foedselsnummer.charAt(i), 10) * faktors[i];
        }
        return sum;
    }

    private static boolean validerFnrStruktur(String foedselsnummer) {
        if (foedselsnummer.length() != FNR_LENGDE) {
            return false;
        }
        int checksumEn = FNR_LENGDE - (sum(foedselsnummer, CHECKSUM_EN_VECTOR) % FNR_LENGDE);
        if (checksumEn == FNR_LENGDE) {
            checksumEn = 0;
        }
        int checksumTo = FNR_LENGDE - (sum(foedselsnummer, CHECKSUM_TO_VECTOR) % FNR_LENGDE);
        if (checksumTo == FNR_LENGDE) {
            checksumTo = 0;
        }
        return checksumEn == Character.digit(foedselsnummer.charAt(FNR_LENGDE - 2), 10)
            && checksumTo == Character.digit(foedselsnummer.charAt(FNR_LENGDE - 1), 10);
    }

    public static PersonIdent fra(String ident) {
        return ident == null ? null : new PersonIdent(ident);
    }

    @Override
    public String getIndexKey() {
        return ident + "(" + (erDnr() ? "DNR" : erFdatNummer() ? "FDAT" : "FNR") + ")";
    }

    @Override
    public int compareTo(PersonIdent o) {
        return this.ident.compareTo(o.ident);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !this.getClass().equals(obj.getClass())) {
            return false;
        }
        PersonIdent other = (PersonIdent) obj;
        return Objects.equals(ident, other.ident);
    }

    @AbacAttributt(value = "fnr", masker = true)
    public String getIdent() {
        return erGyldigFnr(ident) ? ident : null;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktørId() {
        return erAktørId() ? ident : null;
    }

    public boolean erAktørId() {
        return ident != null && ident.length() == AKTOR_ID_LENGDE;
    }

    /** Er FNR eller DNR. (ikke FDAT, eller AktørId). */
    public boolean erNorskIdent() {
        return erFnr() || erDnr();
    }

    public boolean erFnr() {
        return erGyldigFnr(ident) && !erDnr();
    }

    public boolean erDnr() {
        int n = Character.digit(ident.charAt(0), 10);
        return n > 3 && n <= 7 && erGyldigFnr(ident);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ident);
    }

    /**
     * Hvorvidt dette er et Fdat Nummer (dvs. gjelder person uten tildelt fødselsnummer).
     */
    public boolean erFdatNummer() {
        return isFdatPersonNummer(getPersonnummer(ident));
    }

}
