package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.opptjening.Arbeidstaker;
import no.nav.k9.søknad.felles.opptjening.Organisasjonsnummer;
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.PersonIdent;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;


class SøknadOppgittFraværMapper {

    private final OmsorgspengerUtbetaling søknadsinnhold;
    private final Søker søker;
    private final JournalpostId journalpostId;
    private Collection<ArbeidsforholdReferanse> arbeidsforholdene;

    public SøknadOppgittFraværMapper(OmsorgspengerUtbetaling søknadsinnhold, Søker søker, JournalpostId journalpostId, Collection<ArbeidsforholdReferanse> arbeidsforhold) {
        this.søknadsinnhold = søknadsinnhold;
        this.søker = søker;
        this.journalpostId = journalpostId;
        this.arbeidsforholdene = arbeidsforhold;
    }

    Set<OppgittFraværPeriode> map() {
        var opptj = Objects.requireNonNull(søknadsinnhold.getAktivitet());

        var atAktiviteter = Optional.ofNullable(opptj.getArbeidstaker()).orElse(Collections.emptyList());
        var snAktiviteter = Optional.ofNullable(opptj.getSelvstendigNæringsdrivende()).orElse(Collections.emptyList());
        var frilanser = opptj.getFrilanser();

        Set<OppgittFraværPeriode> oppgittFraværPerioder = new LinkedHashSet<>();
        for (FraværPeriode fp : søknadsinnhold.getFraværsperioder()) {
            LocalDate fom = fp.getPeriode().getFraOgMed();
            LocalDate tom = fp.getPeriode().getTilOgMed();
            Duration varighet = fp.getDuration();
            FraværÅrsak fraværÅrsak = FraværÅrsak.fraKode(fp.getÅrsak().getKode());

            for (SelvstendigNæringsdrivende sn : snAktiviteter) {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(sn.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);
                oppgittFraværPerioder.add(oppgittFraværPeriode);
            }
            for (Arbeidstaker arbeidstaker : atAktiviteter) {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(arbeidstaker.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);
                oppgittFraværPerioder.add(oppgittFraværPeriode);
            }
            if (frilanser != null) {
                Arbeidsgiver arbeidsgiver = null;
                InternArbeidsforholdRef arbeidsforholdRef = null;
                OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.FRILANSER, arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);
                oppgittFraværPerioder.add(oppgittFraværPeriode);
            }
        }

        return oppgittFraværPerioder;
    }

    private Arbeidsgiver byggArbeidsgiver(Organisasjonsnummer organisasjonsnummer, PersonIdent personIdent) {
        return organisasjonsnummer != null
            ? Arbeidsgiver.virksomhet(organisasjonsnummer.getVerdi())
            : (personIdent != null
            ? Arbeidsgiver.fra(new AktørId(personIdent.getVerdi()))
            : null);
    }
}
