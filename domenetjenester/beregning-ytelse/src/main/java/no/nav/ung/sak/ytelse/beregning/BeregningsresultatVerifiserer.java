package no.nav.ung.sak.ytelse.beregning;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.ung.sak.typer.Arbeidsgiver;

public final class BeregningsresultatVerifiserer {

    private BeregningsresultatVerifiserer() {
        // SKjule default constructor
    }

    public static void verifiserBeregningsresultat(BeregningsresultatEntitet beregningsresultat) {
        Objects.requireNonNull(beregningsresultat, "Beregningsresultat");
        Objects.requireNonNull(beregningsresultat.getRegelInput(), "Regelinput beregningsresultat");
        Objects.requireNonNull(beregningsresultat.getRegelSporing(), "Regelsporing beregningsresultat");
        verifiserPerioder(beregningsresultat.getBeregningsresultatPerioder());
    }

    private static void verifiserPerioder(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        beregningsresultatPerioder.forEach(BeregningsresultatVerifiserer::verifiserPeriode);
    }

    private static void verifiserPeriode(BeregningsresultatPeriode periode) {
        Objects.requireNonNull(periode, "beregningsresultatperiode");
        Objects.requireNonNull(periode.getBeregningsresultatPeriodeFom(), "beregningsresultatperiodeFom");
        Objects.requireNonNull(periode.getBeregningsresultatPeriodeTom(), "beregningsresultatperiodeTom");
        verifiserDagsats(periode.getDagsats(), "beregningsresultatperiode");
        verifiserAndeler(periode.getBeregningsresultatAndelList());
    }

    private static void verifiserAndeler(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        beregningsresultatAndelList.forEach(BeregningsresultatVerifiserer::verifiserAndel);
    }

    private static void verifiserAndel(BeregningsresultatAndel andel) {
        Objects.requireNonNull(andel.getAktivitetStatus(), "beregningsresultatandelAktivitetstatus");
        Objects.requireNonNull(andel.getInntektskategori(), "beregningsresultatandelInntektskategori");

        verifiserDagsats(andel.getDagsats(), "beregningsresultatandel");

        if (!andel.erBrukerMottaker()) {
            verifiserArbeidsgiver(andel.getArbeidsgiver());
        }
    }

    private static void verifiserArbeidsgiver(Optional<Arbeidsgiver> arbeidsgiverOpt) {
        if (arbeidsgiverOpt.isEmpty()) {
            throw BeregningsresultatVerifiserer.BeregningsresultatVerifisererFeil.FEILFACTORY.verifiserAtArbeidsgiverErSatt().toException();
        }
        Arbeidsgiver arbeidsgiver = arbeidsgiverOpt.get();
        if (arbeidsgiver.erAktørId()) {
            Objects.requireNonNull(arbeidsgiver.getAktørId(), "aktørId");
        } else {
            Objects.requireNonNull(arbeidsgiver.getOrgnr(), "orgnr");
        }
    }

    private static void verifiserDagsats(int dagsats, String obj) {
        if (dagsats < 0) {
            throw BeregningsresultatVerifiserer.BeregningsresultatVerifisererFeil.FEILFACTORY.verifiserIkkeNegativDagsats(obj).toException();
        }
    }

    private interface BeregningsresultatVerifisererFeil extends DeklarerteFeil {
        BeregningsresultatVerifiserer.BeregningsresultatVerifisererFeil FEILFACTORY = FeilFactory.create(BeregningsresultatVerifiserer.BeregningsresultatVerifisererFeil.class);

        @TekniskFeil(feilkode = "FP-370744", feilmelding = "Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. Dagsatsen på %s er mindre enn 0, men skulle ikke vært det.", logLevel = LogLevel.ERROR)
        Feil verifiserIkkeNegativDagsats(String obj);

        @TekniskFeil(feilkode = "FP-370745", feilmelding = "Postcondition feilet: Beregningsresultat i ugyldig tilstand etter steg. Dagsats på andel skal til arbeidsgiver men arbeidsgiver er ikke satt", logLevel = LogLevel.ERROR)
        Feil verifiserAtArbeidsgiverErSatt();
    }
}
