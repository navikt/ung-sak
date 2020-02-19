package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.util.Map;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

class MapperForInntektskategori {

    private static final Map<Inntektskategori, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori> INNTEKTSKATEGORI_MAP = Map.of(
        Inntektskategori.ARBEIDSTAKER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.ARBEIDSTAKER,
        Inntektskategori.FRILANSER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.FRILANSER,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
        Inntektskategori.DAGPENGER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.DAGPENGER,
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.ARBEIDSAVKLARINGSPENGER,
        Inntektskategori.SJØMANN, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.SJØMANN,
        Inntektskategori.DAGMAMMA, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.DAGMAMMA,
        Inntektskategori.JORDBRUKER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.JORDBRUKER,
        Inntektskategori.FISKER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.FISKER,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER
    );

    private MapperForInntektskategori() {
        //for å unngå instansiering, slik at SonarQube blir glad
    }

    static no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori mapInntektskategori(Inntektskategori inntektskategori) {
        no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori resultat = INNTEKTSKATEGORI_MAP.get(inntektskategori);
        if (resultat != null) {
            return resultat;
        }
        throw new IllegalArgumentException("Utvikler-feil: Inntektskategorien " + inntektskategori + " er ikke støttet i mapping");
    }
}
