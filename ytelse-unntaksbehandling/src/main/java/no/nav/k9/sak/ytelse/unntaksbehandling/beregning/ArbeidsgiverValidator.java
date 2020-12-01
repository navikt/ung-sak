package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.ArbeidsgiverValidator.ArbeidsgiverLookupFeil.FACTORY;
import static no.nav.vedtak.feil.LogLevel.INFO;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

@Dependent
class ArbeidsgiverValidator {
    static final int ORGNUMMER_LENGDE = 9;

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
        perioder.stream()
            .flatMap(p -> p.getAndeler().stream())
            .map(TilkjentYtelseAndelDto::getArbeidsgiver)
            .forEach(arbeidsgiver -> validerArbeidsgiver(arbeidsgiver.getIdentifikator(), fagsakAktørId));
    }

    void validerArbeidsgiver(String identifikator, AktørId faksakAktørid) {
        Objects.requireNonNull(identifikator, "identifikator for arbeidsgiver kan ikke være tom");

        if (identifikator.length() == ORGNUMMER_LENGDE) {
            validerOrgnummer(identifikator);
        } else {
            validerAktørId(identifikator, faksakAktørid);
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

    private void validerAktørId(String identifikator, AktørId fagsakAktørId) {
        if (!identifikator.equals(fagsakAktørId.getId())) {
            throw FACTORY.ukjentIdentifikator(identifikator).toException();
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

        @FunksjonellFeil(feilkode = "K9-146118", feilmelding = "Arbeidsgiver for andel er verken orgnummer eller bruker. Ident mottatt: %s", løsningsforslag = "", logLevel = INFO)
        Feil ukjentIdentifikator(String feilmelding);
    }
}
