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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
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
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public SøknadOppgittFraværMapper(AktørTjeneste aktørTjeneste, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.aktørTjeneste = aktørTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    Set<OppgittFraværPeriode> mapFraværskorringeringIm(JournalpostId jpId, OmsorgspengerUtbetaling ytelse, Behandling behandling) {
        var fraværsperioderKorrigeringIm = ytelse.getFraværsperioderKorrigeringIm();
        if (fraværsperioderKorrigeringIm == null) {
            return Set.of();
        }

        Set<OppgittFraværPeriode> resultat = new LinkedHashSet<>();
        for (FraværPeriode fp : fraværsperioderKorrigeringIm) {
            resultat.add(mapAt(jpId, fp, FraværÅrsak.UDEFINERT, behandling));
        }

        return resultat;
    }

    Set<OppgittFraværPeriode> mapFraværFraSøknad(JournalpostId jpId, OmsorgspengerUtbetaling ytelse, Søker søker, Behandling behandling) {
        var fraværsperioder = ytelse.getFraværsperioder();
        if (fraværsperioder == null) {
            return Set.of();
        }
        var opptj = Objects.requireNonNull(ytelse.getAktivitet());
        var snAktiviteter = Optional.ofNullable(opptj.getSelvstendigNæringsdrivende()).orElse(Collections.emptyList());
        var frilanser = opptj.getFrilanser();

        Set<OppgittFraværPeriode> resultat = new LinkedHashSet<>();
        for (FraværPeriode fp : OmsorspengerFraværPeriodeSammenslåer.fjernHelgOgSlåSammen(fraværsperioder)) {
            LocalDate fom = fp.getPeriode().getFraOgMed();
            LocalDate tom = fp.getPeriode().getTilOgMed();
            FraværÅrsak fraværÅrsak = FraværÅrsak.fraKode(fp.getÅrsak().getKode());
            List<AktivitetFravær> aktivitetFravær = Objects.requireNonNull(fp.getAktivitetFravær());

            Set<OppgittFraværPeriode> mappedePerioder = new LinkedHashSet<>();
            if (aktivitetFravær.contains(AktivitetFravær.SELVSTENDIG_VIRKSOMHET)) {
                mappedePerioder.addAll(mapSn(snAktiviteter, søker, jpId, fp, fraværÅrsak));
            }
            if (aktivitetFravær.contains(AktivitetFravær.ARBEIDSTAKER)) {
                mappedePerioder.add(mapAt(jpId, fp, fraværÅrsak, behandling));
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
                    arbeidsgiver, arbeidsforholdRef, varighet, null, fraværÅrsak, SøknadÅrsak.UDEFINERT);
            })
            .collect(Collectors.toSet());
    }

    private OppgittFraværPeriode mapAt(JournalpostId jpId, FraværPeriode fp, FraværÅrsak fraværÅrsak, Behandling behandling) {
        Organisasjonsnummer organisasjonsnummer = Objects.requireNonNull(fp.getArbeidsgiverOrgNr(), "mangler orgnummer for arbeidsgiver i søknaden");
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(organisasjonsnummer.getVerdi());
        InternArbeidsforholdRef arbeidsforholdRef = mapEksternArbeidsforholdId(fp.getArbeidsforholdId(), behandling);

        LocalDate fom = fp.getPeriode().getFraOgMed();
        LocalDate tom = fp.getPeriode().getTilOgMed();
        Duration varighet = fp.getDuration();
        Beløp refusjonsbeløp = null;

        SøknadÅrsak søknadsÅrsak = fp.getSøknadÅrsak() != null ? SøknadÅrsak.fraKode(fp.getSøknadÅrsak().getKode()) : SøknadÅrsak.UDEFINERT;
        Objects.requireNonNull(søknadsÅrsak, "fant ingen søknadÅrsak kode for:" + fp.getSøknadÅrsak());

        return new OppgittFraværPeriode(jpId, fom, tom, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, varighet, refusjonsbeløp, fraværÅrsak, søknadsÅrsak);
    }

    private InternArbeidsforholdRef mapEksternArbeidsforholdId(String arbeidsforholdId, Behandling behandling) {
        InternArbeidsforholdRef arbeidsforholdRef = null;
        if (arbeidsforholdId != null) {
            var referanse = BehandlingReferanse.fra(behandling);
            var inntekstmelding = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType())
                .stream()
                .filter(im ->
                    im.getEksternArbeidsforholdRef().isPresent() &&
                    im.getEksternArbeidsforholdRef().get().equals(EksternArbeidsforholdRef.ref(arbeidsforholdId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Finner ikke intern arbeidsforholdId for arbeidsforholdId=" + arbeidsforholdId));
            arbeidsforholdRef = inntekstmelding.getArbeidsforholdRef();
        }
        return arbeidsforholdRef;
    }

    private Set<OppgittFraværPeriode> mapFl(Frilanser frilanser, JournalpostId journalpostId, FraværPeriode fp, FraværÅrsak fraværÅrsak) {
        LocalDate fom = fp.getPeriode().getFraOgMed();
        LocalDate tom = fp.getPeriode().getTilOgMed();
        Duration varighet = fp.getDuration();

        if (frilanser != null) {
            Arbeidsgiver arbeidsgiver = null;
            InternArbeidsforholdRef arbeidsforholdRef = null;
            Beløp refusjonsbeløp = null;
            OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, UttakArbeidType.FRILANSER,
                arbeidsgiver, arbeidsforholdRef, varighet, refusjonsbeløp, fraværÅrsak, SøknadÅrsak.UDEFINERT);

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
