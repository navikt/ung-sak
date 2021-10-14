package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.felles.fravær.AktivitetFravær;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.opptjening.Frilanser;
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.Organisasjonsnummer;
import no.nav.k9.søknad.felles.type.PersonIdent;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;


@Dependent
class SøknadOppgittFraværMapper {

    private static final Logger logger = LoggerFactory.getLogger(SøknadOppgittFraværMapper.class);

    private AktørTjeneste aktørTjeneste;

    @Inject
    public SøknadOppgittFraværMapper(AktørTjeneste aktørTjeneste) {
        this.aktørTjeneste = aktørTjeneste;
    }

    Set<OppgittFraværPeriode> map(OmsorgspengerUtbetaling søknadsinnhold, Søker søker, JournalpostId jpId) {
        Set<OppgittFraværPeriode> resultat = new LinkedHashSet<>();

        var fraværskorrigeringIm = søknadsinnhold.getFraværsperioderKorrigeringIm();
        var fraværPerioderFraSøknad = OmsorspengerFraværPeriodeSammenslåer.fjernHelgOgSlåSammen(søknadsinnhold.getFraværsperioder());
        resultat.addAll(mapFraværskorringeringIm(jpId, fraværskorrigeringIm));
        resultat.addAll(mapFraværFraSøknad(jpId, fraværPerioderFraSøknad, søknadsinnhold, søker));

        return resultat;
    }

    private Set<OppgittFraværPeriode> mapFraværskorringeringIm(JournalpostId jpId, List<FraværPeriode> fraværPerioder) {
        Set<OppgittFraværPeriode> resultat = new LinkedHashSet<>();
        for (FraværPeriode fp : fraværPerioder) {
            FraværÅrsak fraværÅrsak = FraværÅrsak.fraKode(fp.getÅrsak().getKode());
            resultat.add(mapAt(jpId, fp, fraværÅrsak));
        }
        return resultat;
    }

    private Set<OppgittFraværPeriode> mapFraværFraSøknad(JournalpostId jpId, List<FraværPeriode> fraværPerioder, OmsorgspengerUtbetaling søknadsinnhold, Søker søker) {
        var opptj = Objects.requireNonNull(søknadsinnhold.getAktivitet());
        var snAktiviteter = Optional.ofNullable(opptj.getSelvstendigNæringsdrivende()).orElse(Collections.emptyList());
        var frilanser = opptj.getFrilanser();

        Set<OppgittFraværPeriode> resultat = new LinkedHashSet<>();
        for (FraværPeriode fp : fraværPerioder) {
            LocalDate fom = fp.getPeriode().getFraOgMed();
            LocalDate tom = fp.getPeriode().getTilOgMed();
            Duration varighet = fp.getDuration();
            FraværÅrsak fraværÅrsak = FraværÅrsak.fraKode(fp.getÅrsak().getKode());
            List<AktivitetFravær> aktivitetFravær = Objects.requireNonNull(fp.getAktivitetFravær());

            Set<OppgittFraværPeriode> mappedePerioder = new LinkedHashSet<>();
            if (aktivitetFravær.contains(AktivitetFravær.SELVSTENDIG_VIRKSOMHET)) {
                mappedePerioder.addAll(mapSn(snAktiviteter, søker, jpId, fp, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.ARBEIDSTAKER)) {
                mappedePerioder.add(mapAt(jpId, fp, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.FRILANSER)) {
                mappedePerioder.addAll(mapFl(frilanser, jpId, fp, fraværÅrsak));
            }
            if (mappedePerioder.isEmpty()) {
                logger.warn("Klarte ikke koble fraværsperioden {} til {} til aktivitet. Gjelder journalpostId {}", fom, tom, jpId.getVerdi());
            }

            resultat.addAll(mappedePerioder);
        }
        return resultat;
    }

    private Set<OppgittFraværPeriode> mapSn(List<SelvstendigNæringsdrivende> snAktiviteter, Søker søker, JournalpostId journalpostId, FraværPeriode fp, FraværÅrsak fraværÅrsak) {
        LocalDate fom = fp.getPeriode().getFraOgMed();
        LocalDate tom = fp.getPeriode().getTilOgMed();
        Duration varighet = fp.getDuration();
        return snAktiviteter.stream()
            .map(sn -> {
                Arbeidsgiver arbeidsgiver = byggArbeidsgiver(sn.getOrganisasjonsnummer(), søker.getPersonIdent());
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her
                return new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE,
                    arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak, SøknadÅrsak.UDEFINERT);
            })
            .collect(Collectors.toSet());
    }

    private OppgittFraværPeriode mapAt(JournalpostId jpId, FraværPeriode fp, FraværÅrsak fraværÅrsak) {
        if (fp.getArbeidsforholdId() != null) {
            throw new UnsupportedOperationException("Korrigering med arbeidsforholdId er ikke støttet ennå");
        }
        Organisasjonsnummer organisasjonsnummer = Objects.requireNonNull(fp.getArbeidsgiverOrgNr(), "mangler orgnummer for arbeidsgiver i søknaden");
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(organisasjonsnummer.getVerdi());
        LocalDate fom = fp.getPeriode().getFraOgMed();
        LocalDate tom = fp.getPeriode().getTilOgMed();
        Duration varighet = fp.getDuration();

        SøknadÅrsak søknadsÅrsak = fp.getSøknadÅrsak() != null ? SøknadÅrsak.fraKode(fp.getSøknadÅrsak().getKode()) : SøknadÅrsak.UDEFINERT;
        Objects.requireNonNull(søknadsÅrsak, "fant ingen søknadÅrsak kode for:" + fp.getSøknadÅrsak());

        return new OppgittFraværPeriode(jpId, fom, tom, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, varighet, fraværÅrsak, søknadsÅrsak);
    }

    private Set<OppgittFraværPeriode> mapFl(Frilanser frilanser, JournalpostId journalpostId, FraværPeriode fp, FraværÅrsak fraværÅrsak) {
        LocalDate fom = fp.getPeriode().getFraOgMed();
        LocalDate tom = fp.getPeriode().getTilOgMed();
        Duration varighet = fp.getDuration();

        if (frilanser != null) {
            Arbeidsgiver arbeidsgiver = null;
            InternArbeidsforholdRef arbeidsforholdRef = null;
            OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.FRILANSER,
                arbeidsgiver, arbeidsforholdRef, varighet, fraværÅrsak, SøknadÅrsak.UDEFINERT);

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
