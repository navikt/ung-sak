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
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

@Dependent
class ArbeidsgiverValidator {
    private static final List<Inntektskategori> INNTEKTKATEGORI_UTEN_ARBEIDSGIVER = List.of(
        Inntektskategori.FRILANSER,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
        Inntektskategori.DAGMAMMA,
        Inntektskategori.JORDBRUKER,
        Inntektskategori.FISKER
    );

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
    void valider(List<TilkjentYtelsePeriodeDto> perioder) {
        var andeler = perioder.stream().flatMap(p -> p.getAndeler().stream()).collect(Collectors.toList());

        var andelerUtenArbeidsgiver = andeler.stream()
            .filter(andel -> INNTEKTKATEGORI_UTEN_ARBEIDSGIVER.contains(andel.getInntektskategori()))
            .collect(Collectors.toList());
        var perioderMedArbeidsgiver = andeler.stream()
            .filter(andel -> !INNTEKTKATEGORI_UTEN_ARBEIDSGIVER.contains(andel.getInntektskategori()))
            .collect(Collectors.toList());

        andelerUtenArbeidsgiver.forEach(andel -> validerAndelUtenArbeidsgiver(andel));
        perioderMedArbeidsgiver.forEach(andel -> validerArbeidsgiver(andel.getArbeidsgiverOrgnr()));
    }

    private void validerAndelUtenArbeidsgiver(TilkjentYtelseAndelDto andel) {
        var refusjon = Optional.ofNullable(andel.getRefusjonsbeløp()).orElse(0);
        if (refusjon > 0) {
            throw new IllegalArgumentException("Må oppgi arbeidstaker dersom andel er refusjon");
        }

    }

    void validerArbeidsgiver(OrgNummer orgNummer) {
        if (orgNummer == null || orgNummer.getOrgNummer() == null) {
            throw new IllegalArgumentException("Mangler id for arbeidsgiver");
        }
        validerOrgnummer(orgNummer.getOrgNummer());
    }

    private void validerOrgnummer(String identifikator) {
        if (!OrgNummer.erGyldigOrgnr(identifikator)) {
            throw FACTORY.ugyldigOrgnummer().toException();
        }

        ArbeidsgiverOpplysninger arbeidsgiverOpplysninger;
        try {
            arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.virksomhet(identifikator));
        } catch (RuntimeException e) {
            throw FACTORY.ukjentOrgnummer(e).toException();
        }
        if (arbeidsgiverOpplysninger == null) {
            throw FACTORY.ukjentOrgnummer().toException();
        }
    }

    interface ArbeidsgiverLookupFeil extends DeklarerteFeil {
        ArbeidsgiverValidator.ArbeidsgiverLookupFeil FACTORY = FeilFactory.create(ArbeidsgiverValidator.ArbeidsgiverLookupFeil.class);

        @FunksjonellFeil(feilkode = "K9-564221", feilmelding = "Arbeidsgiver for andel er ikke et gyldig orgnummer", løsningsforslag = "Forsøk med gyldig orgnummer", logLevel = INFO)
        Feil ugyldigOrgnummer();

        @FunksjonellFeil(feilkode = "K9-187651", feilmelding = "Arbeidsgiver for andel finnes ikke i Enhetsregisteret", løsningsforslag = "", logLevel = INFO)
        Feil ukjentOrgnummer();

        @FunksjonellFeil(feilkode = "K9-886241", feilmelding = "Arbeidsgiver for andel finnes ikke i Enhetsregisteret", løsningsforslag = "", logLevel = INFO)
        Feil ukjentOrgnummer(Throwable e);
    }
}
