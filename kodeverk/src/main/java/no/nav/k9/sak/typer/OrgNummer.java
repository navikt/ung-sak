package no.nav.k9.sak.typer;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Id som genereres fra NAV Aktør Register. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker går fra
 * DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 *
 * Støtter også kunstige orgnummer (internt definert konstant i fp - orgnummer=342352362)
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OrgNummer implements Comparable<OrgNummer>, IndexKey {

    /**
     * Orgnr for KUNSTIG organisasjoner. Går sammen med OrganisasjonType#KUNSTIG.
     * (p.t. kun en kunstig organisasjon som holder på arbeidsforhold lagt til av saksbehandler.)
     */
    public static final String KUNSTIG_ORG = "342352362"; // magic constant

    @JsonValue
    @NotNull
    @Size(max=20)
    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String orgNummer; // NOSONAR

    @JsonCreator
    public OrgNummer(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer");
        if (!erGyldigOrgnr(orgNummer)) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            throw new IllegalArgumentException("Ikke gyldig orgnummer: " + orgNummer);
        }
        this.orgNummer = orgNummer.trim();
    }

    protected OrgNummer() {
        // for hibernate
    }

    public static boolean erKunstig(String orgNr) {
        return KUNSTIG_ORG.equals(orgNr);
    }

    @Override
    public int compareTo(OrgNummer o) {
        // TODO: Burde ikke finnes
        return orgNummer.compareTo(o.orgNummer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof OrgNummer)) {
            return false;
        }
        OrgNummer other = (OrgNummer) obj;
        return Objects.equals(orgNummer, other.orgNummer);
    }

    private static String masker(String identifikator) {
        return identifikator.substring(0, Math.min(identifikator.length(), 3))
            + "...";
    }

    public String getId() {
        return orgNummer;
    }

    public String getOrgNummer() {
        return orgNummer;
    }

    @Override
    public String getIndexKey() {
        return orgNummer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgNummer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + masker(orgNummer) + ">";
    }

    /** @return false hvis ikke gyldig orgnr. */
    public static boolean erGyldigOrgnr(String ident) {
        return erKunstig(ident) || OrganisasjonsNummerValidator.erGyldig(ident);
    }
}
