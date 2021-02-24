package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.felles.aktivitet.Frilanser;
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;


class SøknadOppgittFraværMapper {

    private final OmsorgspengerUtbetaling søknadsinnhold;
    private final Søker søker;
    private final JournalpostId journalpostId;

    public SøknadOppgittFraværMapper(OmsorgspengerUtbetaling søknadsinnhold, Søker søker, JournalpostId journalpostId) {
        this.søknadsinnhold = søknadsinnhold;
        this.søker = søker;
        this.journalpostId = journalpostId;
    }

    Set<OppgittFraværPeriode> map() {
        Frilanser frilanser = søknadsinnhold.getAktivitet().getFrilanser();
        var snAktiviteter = Optional.ofNullable(søknadsinnhold.getAktivitet().getSelvstendigNæringsdrivende())
            .orElse(Collections.emptyList());

        if (søknadsinnhold.getAktivitet().getArbeidstaker() != null) {
            // TODO: Arbeidstaker. Se også {@link KravDokumentFravær#trekkUtAlleFraværOgValiderOverlapp}
            throw new UnsupportedOperationException("Støtter ikke arbeidstaker for OMS");
        }

        var fraværsperioder = søknadsinnhold.getFraværsperioder();
        Set<OppgittFraværPeriode> oppgittFraværPerioder;
        oppgittFraværPerioder = new LinkedHashSet<>();
        for (FraværPeriode fp : fraværsperioder) {
            LocalDate fom = fp.getPeriode().getFraOgMed();
            LocalDate tom = fp.getPeriode().getTilOgMed();
            Duration varighet = fp.getDuration();
            for (SelvstendigNæringsdrivende sn : snAktiviteter) {
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her, tolker om til InternArbeidsforholdRef.nullRef() ved fastsette uttak.
                var arbeidsgiver = sn.organisasjonsnummer != null
                    ? Arbeidsgiver.virksomhet(sn.organisasjonsnummer.verdi)
                    : (søker.getPersonIdent() != null
                    ? Arbeidsgiver.fra(new AktørId(søker.getPersonIdent().getVerdi()))
                    : null);

                OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, arbeidsforholdRef, varighet);
                oppgittFraværPerioder.add(oppgittFraværPeriode);
            }
            if (frilanser != null) {
                //TODO skal filtrere/bruke frilanser.jobberFortsattSomFrilanser og frilanser.startdato?
                Arbeidsgiver arbeidsgiver = null;
                InternArbeidsforholdRef arbeidsforholdRef = null;
                OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.FRILANSER, arbeidsgiver, arbeidsforholdRef, varighet);
                oppgittFraværPerioder.add(oppgittFraværPeriode);
            }
        }
        return oppgittFraværPerioder;
    }
}
