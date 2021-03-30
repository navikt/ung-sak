package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.felles.fravær.AktivitetFravær;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.opptjening.Arbeidstaker;
import no.nav.k9.søknad.felles.opptjening.Frilanser;
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
            List<AktivitetFravær> aktivitetFravær = Objects.requireNonNull(fp.getAktivitetFravær());

            if (aktivitetFravær.contains(AktivitetFravær.SELVSTENDIG_VIRKSOMHET)) {
                oppgittFraværPerioder.addAll(mapSn(snAktiviteter, fom, tom, varighet, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.ARBEIDSTAKER)) {
                oppgittFraværPerioder.addAll(mapAt(atAktiviteter, fom, tom, varighet, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.FRILANSER)) {
                oppgittFraværPerioder.addAll(mapFl(frilanser, fom, tom, varighet, fraværÅrsak));
            }
        }

        return oppgittFraværPerioder;
    }

    private Set<OppgittFraværPeriode> mapSn(List<SelvstendigNæringsdrivende> snAktiviteter, LocalDate fom, LocalDate tom, Duration varighet, FraværÅrsak fraværÅrsak) {
        return snAktiviteter.stream()
            .map(sn -> {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(sn.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                return new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE,
                    arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);
            })
            .collect(Collectors.toSet());
    }

    private Set<OppgittFraværPeriode> mapAt(List<Arbeidstaker> atAktiviteter, LocalDate fom, LocalDate tom, Duration varighet, FraværÅrsak fraværÅrsak) {
        return atAktiviteter.stream()
            .map(arbeidstaker -> {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(arbeidstaker.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                return new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.ARBEIDSTAKER,
                    arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);

            })
            .collect(Collectors.toSet());
    }

    private Set<OppgittFraværPeriode> mapFl(Frilanser frilanser, LocalDate fom, LocalDate tom, Duration varighet, FraværÅrsak fraværÅrsak) {
        if (frilanser != null) {
            Arbeidsgiver arbeidsgiver = null;
            InternArbeidsforholdRef arbeidsforholdRef = null;
            OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.FRILANSER,
                arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);

            return Set.of(oppgittFraværPeriode);
        }
        return Set.of();
    }

    private Arbeidsgiver byggArbeidsgiver(Organisasjonsnummer organisasjonsnummer, PersonIdent personIdent) {
        return organisasjonsnummer != null
            ? Arbeidsgiver.virksomhet(organisasjonsnummer.getVerdi())
            : (personIdent != null
            ? Arbeidsgiver.fra(new AktørId(personIdent.getVerdi()))
            : null);
    }
}
