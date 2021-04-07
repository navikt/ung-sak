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

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
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


@Dependent
class SøknadOppgittFraværMapper {

    private AktørTjeneste aktørTjeneste;

    public SøknadOppgittFraværMapper() {
        // CDI
    }

    @Inject
    public SøknadOppgittFraværMapper(AktørTjeneste aktørTjeneste) {
        this.aktørTjeneste = aktørTjeneste;
    }


    Set<OppgittFraværPeriode> map(OmsorgspengerUtbetaling søknadsinnhold, Søker søker, JournalpostId journalpostId, Collection<ArbeidsforholdReferanse> arbeidsforhold) {
        var opptj = Objects.requireNonNull(søknadsinnhold.getAktivitet());

        var atAktiviteter = Optional.ofNullable(opptj.getArbeidstaker()).orElse(Collections.emptyList());
        var snAktiviteter = Optional.ofNullable(opptj.getSelvstendigNæringsdrivende()).orElse(Collections.emptyList());
        var frilanser = opptj.getFrilanser();

        Set<OppgittFraværPeriode> oppgittFraværPerioder = new LinkedHashSet<>();
        for (FraværPeriode fp : FraværPeriodeSammenslåer.slåSammen(søknadsinnhold.getFraværsperioder())) {
            LocalDate fom = fp.getPeriode().getFraOgMed();
            LocalDate tom = fp.getPeriode().getTilOgMed();
            Duration varighet = fp.getDuration();
            FraværÅrsak fraværÅrsak = FraværÅrsak.fraKode(fp.getÅrsak().getKode());
            List<AktivitetFravær> aktivitetFravær = Objects.requireNonNull(fp.getAktivitetFravær());

            if (aktivitetFravær.contains(AktivitetFravær.SELVSTENDIG_VIRKSOMHET)) {
                oppgittFraværPerioder.addAll(mapSn(snAktiviteter, søker, journalpostId, fom, tom, varighet, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.ARBEIDSTAKER)) {
                oppgittFraværPerioder.addAll(mapAt(atAktiviteter, søker, journalpostId, fom, tom, varighet, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.FRILANSER)) {
                oppgittFraværPerioder.addAll(mapFl(frilanser, journalpostId, fom, tom, varighet, fraværÅrsak));
            }
        }

        return oppgittFraværPerioder;
    }

    private Set<OppgittFraværPeriode> mapSn(List<SelvstendigNæringsdrivende> snAktiviteter, Søker søker, JournalpostId journalpostId, LocalDate fom, LocalDate tom, Duration varighet, FraværÅrsak fraværÅrsak) {
        return snAktiviteter.stream()
            .map(sn -> {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(sn.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                return new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE,
                    arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);
            })
            .collect(Collectors.toSet());
    }

    private Set<OppgittFraværPeriode> mapAt(List<Arbeidstaker> atAktiviteter, Søker søker, JournalpostId journalpostId, LocalDate fom, LocalDate tom, Duration varighet, FraværÅrsak fraværÅrsak) {
        return atAktiviteter.stream()
            .map(arbeidstaker -> {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(arbeidstaker.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                return new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.ARBEIDSTAKER,
                    arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak);

            })
            .collect(Collectors.toSet());
    }

    private Set<OppgittFraværPeriode> mapFl(Frilanser frilanser, JournalpostId journalpostId, LocalDate fom, LocalDate tom, Duration varighet, FraværÅrsak fraværÅrsak) {
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
        if (organisasjonsnummer != null) {
            return Arbeidsgiver.virksomhet(organisasjonsnummer.getVerdi());
        } else if (personIdent != null) {
            var aktørId = aktørTjeneste.hentAktørIdForPersonIdent(new no.nav.k9.sak.typer.PersonIdent(personIdent.getVerdi())).orElseThrow(() -> new IllegalArgumentException("Finner ingen aktørid for fnr"));
            return Arbeidsgiver.fra(aktørId);
        } else {
            return null;
        }
    }
}
