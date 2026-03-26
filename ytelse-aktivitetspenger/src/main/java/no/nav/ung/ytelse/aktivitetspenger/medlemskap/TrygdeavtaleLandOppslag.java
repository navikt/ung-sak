package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import no.nav.k9.søknad.felles.type.Landkode;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * Periodisert oppslag for land med gyldig trygdeavtale iht. folketrygdloven §11-3 jf. §1-3b.
 *
 * Dekker:
 * - EØS-land (EU + EFTA/EØS: NOR, ISL, LIE) med historisk korrekte inntredelsesdatoer
 * - Sveits (CHE) via EFTA-konvensjonen §1-3b(b)
 * - Storbritannia (GBR) — EØS fra 1994, deretter §1-3b(c,d,e) etter Brexit uten gap
 */
public final class TrygdeavtaleLandOppslag {

    private static final LocalDate EØS_START = LocalDate.of(1994, 1, 1);
    private static final LocalDate UTVIDELSE_2004 = LocalDate.of(2004, 5, 1);
    private static final LocalDate UTVIDELSE_2007 = LocalDate.of(2007, 1, 1);

    private static final Map<String, LocalDate> GYLDIG_FRA = Map.ofEntries(
        // EFTA/EØS — EØS-avtalen trer i kraft 1. januar 1994
        Map.entry("NOR", EØS_START),
        Map.entry("ISL", EØS_START),
        Map.entry("LIE", EØS_START),
        // EU-12 — opprinnelige EØS-land
        Map.entry("AUT", EØS_START),
        Map.entry("BEL", EØS_START),
        Map.entry("DNK", EØS_START),
        Map.entry("FIN", EØS_START),
        Map.entry("FRA", EØS_START),
        Map.entry("DEU", EØS_START),
        Map.entry("GRC", EØS_START),
        Map.entry("IRL", EØS_START),
        Map.entry("ITA", EØS_START),
        Map.entry("LUX", EØS_START),
        Map.entry("NLD", EØS_START),
        Map.entry("PRT", EØS_START),
        Map.entry("ESP", EØS_START),
        Map.entry("SWE", EØS_START),
        // Storbritannia — EØS fra 1994, deretter §1-3b(c,d,e) etter Brexit uten gap
        Map.entry("GBR", EØS_START),
        // Sveits — EFTA-konvensjonen / bilaterale avtaler med EU (§1-3b(b)), i kraft 1. juni 2002
        Map.entry("CHE", LocalDate.of(2002, 6, 1)),
        // EU/EØS-utvidelse 1. mai 2004
        Map.entry("CYP", UTVIDELSE_2004),
        Map.entry("CZE", UTVIDELSE_2004),
        Map.entry("EST", UTVIDELSE_2004),
        Map.entry("HUN", UTVIDELSE_2004),
        Map.entry("LVA", UTVIDELSE_2004),
        Map.entry("LTU", UTVIDELSE_2004),
        Map.entry("MLT", UTVIDELSE_2004),
        Map.entry("POL", UTVIDELSE_2004),
        Map.entry("SVK", UTVIDELSE_2004),
        Map.entry("SVN", UTVIDELSE_2004),
        // EU/EØS-utvidelse 1. januar 2007
        Map.entry("BGR", UTVIDELSE_2007),
        Map.entry("ROU", UTVIDELSE_2007),
        // Kroatia inn i EU/EØS 1. juli 2013
        Map.entry("HRV", LocalDate.of(2013, 7, 1))
    );

    private TrygdeavtaleLandOppslag() {
    }

    /**
     * Sjekker om et land er et gyldig EØS-/trygdeavtaleland fra og med den oppgitte startdatoen.
     * Bostedsperioden anses som gyldig dersom den starter på eller etter landets inntredelsesdato.
     */
    public static boolean erGyldigTrygdeavtaleLand(Landkode landkode, LocalDate fom) {
        Objects.requireNonNull(landkode, "landkode kan ikke være null");
        Objects.requireNonNull(landkode.getLandkode(), "landkode.getLandkode() kan ikke være null");
        Objects.requireNonNull(fom, "fom kan ikke være null");

        LocalDate gyldigFra = GYLDIG_FRA.get(landkode.getLandkode());
        if (gyldigFra == null) {
            return false;
        }

        return !fom.isBefore(gyldigFra);
    }
}
