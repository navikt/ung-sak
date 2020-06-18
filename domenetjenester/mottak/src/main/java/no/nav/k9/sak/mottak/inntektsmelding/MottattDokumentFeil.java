package no.nav.k9.sak.mottak.inntektsmelding;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface MottattDokumentFeil extends DeklarerteFeil {

    MottattDokumentFeil FACTORY = FeilFactory.create(MottattDokumentFeil.class);

    @TekniskFeil(feilkode = "FP-947147", feilmelding = "Ukjent dokument %s", logLevel = WARN)
    Feil ukjentSkjemaType(String skjemaType);

    @TekniskFeil(feilkode = "FP-921156", feilmelding = "Kjenner ikke igjen format på søknad XML med namespace %s", logLevel = WARN)
    Feil ukjentSoeknadXMLFormat(String skjemaType);

    @TekniskFeil(feilkode = "FP-947148", feilmelding = "Mer enn en implementasjon funnet for skjematype %s", logLevel = WARN)
    Feil flereImplementasjonerAvSkjemaType(String skjemaType);

    @TekniskFeil(feilkode = "FP-931148", feilmelding = "Søknad på behandling %s mangler RelasjonsRolleType", logLevel = WARN)
    Feil dokumentManglerRelasjonsRolleType(long behandlingId);

    @TekniskFeil(feilkode = "FP-454131", feilmelding = "Ukjent type tilrettelegging for svangerskapspenger", logLevel = WARN)
    Feil ukjentTilretteleggingType();

    @TekniskFeil(feilkode = "FP-545381", feilmelding = "Fant ikke personident for arbeidsgiver som er privatperson i TPS", logLevel = WARN)
    Feil finnerIkkeArbeidsgiverITPS();

    @TekniskFeil(feilkode = "FP-187531", feilmelding = "Ukjent type arbeidsforhold i svangerskapspengesøknad", logLevel = WARN)
    Feil ukjentArbeidsForholdType();

    @TekniskFeil(feilkode = "FP-187532", feilmelding = "Behandling [%s] pågår, avventer å håndtere mottatt dokument til det er prosessert", logLevel = WARN)
    Feil behandlingPågårAvventerKnytteMottattDokumentTilBehandling(Long id);
    
    @TekniskFeil(feilkode = "FP-187533", feilmelding = "Ugyldig inntektsmelding [journalpost=%s], bryter med forretningsregel validering: %s", logLevel = WARN)
    Feil inntektsmeldingSemantiskValideringFeil(String feilmelding);
}
