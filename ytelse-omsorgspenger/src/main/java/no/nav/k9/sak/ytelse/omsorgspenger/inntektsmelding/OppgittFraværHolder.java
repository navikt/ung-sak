package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class OppgittFraværHolder {
    private OppgittFraværVerdi søknad;
    private Map<InternArbeidsforholdRef, OppgittFraværVerdi> refusjonskrav;
    private Map<InternArbeidsforholdRef, OppgittFraværVerdi> imUtenRefusjonskrav;

    private OppgittFraværHolder(OppgittFraværVerdi søknad, Map<InternArbeidsforholdRef, OppgittFraværVerdi> refusjonskrav, Map<InternArbeidsforholdRef, OppgittFraværVerdi> imUtenRefusjonskrav) {
        this.refusjonskrav = refusjonskrav;
        this.imUtenRefusjonskrav = imUtenRefusjonskrav;
        this.søknad = søknad;
    }

    public static OppgittFraværHolder fraSøknad(OppgittFraværVerdi oppgittFraværVerdi) {
        return new OppgittFraværHolder(oppgittFraværVerdi, Map.of(), Map.of());
    }

    public static OppgittFraværHolder fraRefusjonskrav(InternArbeidsforholdRef arbeidsforholdRef, OppgittFraværVerdi oppgittFraværVerdi) {
        return new OppgittFraværHolder(null, Map.of(arbeidsforholdRef, oppgittFraværVerdi), Map.of());
    }

    public static OppgittFraværHolder fraImUtenRefusjonskrav(InternArbeidsforholdRef arbeidsforholdRef, OppgittFraværVerdi oppgittFraværVerdi) {
        return new OppgittFraværHolder(null, Map.of(), Map.of(arbeidsforholdRef, oppgittFraværVerdi));
    }

    public OppgittFraværHolder oppdaterMed(OppgittFraværHolder nyeVerdier) {
        OppgittFraværHolder resultat = this;
        for (var entry : nyeVerdier.refusjonskrav.entrySet()) {
            resultat = resultat.leggTilRefusjonskrav(entry.getKey(), entry.getValue());
        }
        for (var entry : nyeVerdier.imUtenRefusjonskrav.entrySet()) {
            resultat = resultat.leggTilImUtenRefusjonskrav(entry.getKey(), entry.getValue());
        }
        if (nyeVerdier.søknad != null) {
            resultat = resultat.leggTilSøknad(nyeVerdier.søknad);
        }
        return resultat;
    }

    public OppgittFraværVerdi getSøknad() {
        return søknad;
    }

    public Map<InternArbeidsforholdRef, OppgittFraværVerdi> getRefusjonskrav() {
        return Collections.unmodifiableMap(refusjonskrav);
    }

    public Map<InternArbeidsforholdRef, OppgittFraværVerdi> getImUtenRefusjonskrav() {
        return Collections.unmodifiableMap(imUtenRefusjonskrav);
    }

    public SamtidigKravStatus samtidigKravStatus() {
        return new SamtidigKravStatus(
            finnStatusSøknad(søknad),
            finnStatusIm(refusjonskrav),
            finnStatusIm(imUtenRefusjonskrav)
        );
    }

    public SøknadÅrsak søknadÅrsak() {
        return søknad != null
            ? søknad.søknadÅrsak()
            : SøknadÅrsak.UDEFINERT;
    }

    public FraværÅrsak fraværÅrsak() {
        return søknad != null
            ? søknad.fraværÅrsak()
            : FraværÅrsak.UDEFINERT;
    }

    public boolean refusjonskravGjelder() {
        SamtidigKravStatus.KravStatus statusRefusjonskrav = finnStatusIm(refusjonskrav);
        SamtidigKravStatus.KravStatus statusSøknad = finnStatusSøknad(søknad);
        return statusRefusjonskrav == SamtidigKravStatus.KravStatus.FINNES
            || statusRefusjonskrav == SamtidigKravStatus.KravStatus.TREKT && statusSøknad != SamtidigKravStatus.KravStatus.FINNES;
    }

    public boolean søknadGjelder() {
        return !refusjonskravGjelder() && finnStatusSøknad(søknad) != SamtidigKravStatus.KravStatus.FINNES_IKKE;
    }

    private static SamtidigKravStatus.KravStatus finnStatusSøknad(OppgittFraværVerdi søknad) {
        if (søknad == null) {
            return SamtidigKravStatus.KravStatus.FINNES_IKKE;
        }
        return søknad.erTrektPeriode()
            ? SamtidigKravStatus.KravStatus.TREKT
            : SamtidigKravStatus.KravStatus.FINNES;
    }

    private static SamtidigKravStatus.KravStatus finnStatusIm(Map<InternArbeidsforholdRef, OppgittFraværVerdi> refusjonskrav) {
        if (refusjonskrav.isEmpty()) {
            return SamtidigKravStatus.KravStatus.FINNES_IKKE;
        }
        return refusjonskrav.values().stream().allMatch(OppgittFraværVerdi::erTrektPeriode)
            ? SamtidigKravStatus.KravStatus.TREKT
            : SamtidigKravStatus.KravStatus.FINNES;
    }

    private OppgittFraværHolder leggTilSøknad(OppgittFraværVerdi søknad) {
        return new OppgittFraværHolder(søknad, refusjonskrav, imUtenRefusjonskrav);
    }

    private OppgittFraværHolder leggTilRefusjonskrav(InternArbeidsforholdRef arbeidsforholdRef, OppgittFraværVerdi nyttRefusjonskrav) {
        return new OppgittFraværHolder(
            søknad,
            leggTilIm(refusjonskrav, arbeidsforholdRef, nyttRefusjonskrav),
            håndterTrekk(imUtenRefusjonskrav, arbeidsforholdRef, nyttRefusjonskrav)
        );
    }

    private OppgittFraværHolder leggTilImUtenRefusjonskrav(InternArbeidsforholdRef arbeidsforholdRef, OppgittFraværVerdi nyImUtenRefusjonskrav) {
        return new OppgittFraværHolder(
            søknad,
            håndterTrekk(refusjonskrav, arbeidsforholdRef, nyImUtenRefusjonskrav),
            leggTilIm(imUtenRefusjonskrav, arbeidsforholdRef, nyImUtenRefusjonskrav)
        );
    }

    private static Map<InternArbeidsforholdRef, OppgittFraværVerdi> håndterTrekk(Map<InternArbeidsforholdRef, OppgittFraværVerdi> tilstand, InternArbeidsforholdRef arbeidsforholdRef, OppgittFraværVerdi imVerdi) {
        if (imVerdi.erTrektPeriode() && harNoeÅTrekke(tilstand, arbeidsforholdRef)) {
            return leggTilIm(tilstand, arbeidsforholdRef, imVerdi);
        } else {
            return tilstand;
        }
    }

    private static boolean harNoeÅTrekke(Map<InternArbeidsforholdRef, OppgittFraværVerdi> tilstand, InternArbeidsforholdRef arbeidsforholdRef) {
        if (InternArbeidsforholdRef.nullRef().equals(arbeidsforholdRef)) {
            return !tilstand.isEmpty();
        } else {
            return tilstand.containsKey(arbeidsforholdRef) || tilstand.containsKey(InternArbeidsforholdRef.nullRef());
        }
    }

    private static Map<InternArbeidsforholdRef, OppgittFraværVerdi> leggTilIm(Map<InternArbeidsforholdRef, OppgittFraværVerdi> tilstand, InternArbeidsforholdRef arbeidsforholdRef, OppgittFraværVerdi imVerdi) {
        if (InternArbeidsforholdRef.nullRef().equals(arbeidsforholdRef)) {
            //arbeidsgiver opplyser ikke (lenger) arbeidsforhold, alt som tidligere er rapportert på arbeidsforhold fjernes
            return Map.of(InternArbeidsforholdRef.nullRef(), imVerdi);
        } else {
            //arbeidsgiver opplyser arbeidsforhold, alt som tidligere er rapportert uten arbeidsforhold fjernes
            var resultat = new LinkedHashMap<>(tilstand);
            resultat.remove(InternArbeidsforholdRef.nullRef());
            resultat.put(arbeidsforholdRef, imVerdi);
            return resultat;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittFraværHolder that = (OppgittFraværHolder) o;
        return Objects.equals(søknad, that.søknad)
            && refusjonskrav.equals(that.refusjonskrav)
            && imUtenRefusjonskrav.equals(that.imUtenRefusjonskrav);
    }

    @Override
    public int hashCode() {
        return Objects.hash(søknad, refusjonskrav, imUtenRefusjonskrav);
    }
}
