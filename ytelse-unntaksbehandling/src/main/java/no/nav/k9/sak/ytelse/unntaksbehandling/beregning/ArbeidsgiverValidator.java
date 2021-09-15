package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static no.nav.k9.felles.feil.LogLevel.INFO;
import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.ArbeidsgiverValidator.ArbeidsgiverLookupFeil.FACTORY;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

@Dependent
class ArbeidsgiverValidator {
    private static final List<Inntektskategori> INNTEKTKATEGORI_UTEN_ARBEIDSGIVER = List.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.FRILANSER);

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidsgiverValidator() {
        // CDI
    }

    @Inject
    ArbeidsgiverValidator(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    /**
     * Ident for arbeidsgiver kan angis fra GUI
     * Valider at ident finnes som arbeidsgiver i Enhetsregisteret (orgnummer), eller er bruker selv (aktørid)
     */
    void valider(List<TilkjentYtelsePeriodeDto> perioder, AktørId fagsakAktørId) {
        var andeler = perioder.stream().flatMap(p -> p.getAndeler().stream()).collect(Collectors.toList());

        var andelerUtenArbeidsgiver = andeler.stream()
            .filter(andel -> INNTEKTKATEGORI_UTEN_ARBEIDSGIVER.contains(andel.getInntektskategori()))
            .collect(Collectors.toList());
        var perioderMedArbeidsgiver = andeler.stream()
            .filter(andel -> !INNTEKTKATEGORI_UTEN_ARBEIDSGIVER.contains(andel.getInntektskategori()))
            .collect(Collectors.toList());

        andelerUtenArbeidsgiver.forEach(andel -> validerAndelUtenArbeidsgiver(andel));
        perioderMedArbeidsgiver.forEach(andel -> validerArbeidsgiver(andel.getArbeidsgiverOrgnr(), andel.getAktørId(), fagsakAktørId));
    }

    private void validerAndelUtenArbeidsgiver(TilkjentYtelseAndelDto andel) {
        var refusjon = Optional.ofNullable(andel.getRefusjon()).orElse(0);
        if (refusjon > 0) {
            throw new IllegalArgumentException("Må oppgi arbeidstaker dersom andel er refusjon");
        }

    }

    void validerArbeidsgiver(OrgNummer orgNummer, AktørId aktørId, AktørId faksakAktørid) {
        if (orgNummer == null && aktørId == null) {
            throw new IllegalArgumentException("Mangler id for arbeidsgiver");
        }
        if (orgNummer != null && aktørId != null) {
            throw new IllegalArgumentException("Kan bare opplyse om orgNr eller personId, ikke begge");
        }
        if (orgNummer != null) {
            validerOrgnummer(orgNummer.getOrgNummer());
        } else {
            validerAktørId(aktørId, faksakAktørid);
        }
    }

    private void validerOrgnummer(String identifikator) {
        if (!OrgNummer.erGyldigOrgnr(identifikator)) {
            throw FACTORY.ugyldigOrgnummer(identifikator).toException();
        }

        ArbeidsgiverOpplysninger arbeidsgiverOpplysninger;
        try {
            arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.virksomhet(identifikator));
        } catch (RuntimeException e) {
            throw FACTORY.ukjentOrgnummer(identifikator, e).toException();
        }
        if (arbeidsgiverOpplysninger == null) {
            throw FACTORY.ukjentOrgnummer(identifikator).toException();
        }
    }

    private void validerAktørId(AktørId identifikator, AktørId fagsakAktørId) {
        if (identifikator.equals(fagsakAktørId)) {
            throw FACTORY.egenArbeidsgiver().toException();
        }
    }

    interface ArbeidsgiverLookupFeil extends DeklarerteFeil {
        ArbeidsgiverValidator.ArbeidsgiverLookupFeil FACTORY = FeilFactory.create(ArbeidsgiverValidator.ArbeidsgiverLookupFeil.class);

        @FunksjonellFeil(feilkode = "K9-564221", feilmelding = "Arbeidsgiver for andel er ikke et gyldig orgnummer: %s", løsningsforslag = "Forsøk med gyldig orgnummer", logLevel = INFO)
        Feil ugyldigOrgnummer(String feilmelding);

        @FunksjonellFeil(feilkode = "K9-187651", feilmelding = "Arbeidsgiver for andel finnes ikke i Enhetsregisteret: %s", løsningsforslag = "", logLevel = INFO)
        Feil ukjentOrgnummer(String feilmelding);

        @FunksjonellFeil(feilkode = "K9-886241", feilmelding = "Arbeidsgiver for andel finnes ikke i Enhetsregisteret: %s", løsningsforslag = "", logLevel = INFO)
        Feil ukjentOrgnummer(String feilmelding, Throwable e);

        @FunksjonellFeil(feilkode = "K9-146118", feilmelding = "Arbeidsgiver for andel er brukeren selv, det gir ikke helt mening. Skulle vært selvstendig næringsdrivende?", løsningsforslag = "", logLevel = INFO)
        Feil egenArbeidsgiver();
    }
}
