package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

class SøknadOppgittFraværMapper {
    private final OmsorgspengerUtbetaling søknadsinnhold;
    private final Søker søker;

    public SøknadOppgittFraværMapper(Søknad søknad) {
        søknadsinnhold = søknad.getYtelse();
        søker = søknad.getSøker();
    }

    List<OppgittFraværPeriode> map() {

        var snAktiviteter = Optional.ofNullable(søknadsinnhold.getAktivitet().getSelvstendigNæringsdrivende())
            .orElse(Collections.emptyList());
        if (søknadsinnhold.getAktivitet().getArbeidstaker() != null){
            // TODO: Arbeistaker
            throw new UnsupportedOperationException("Støtter ikke arbeidstaker for OMS");
        }
        if (søknadsinnhold.getAktivitet().getFrilanser() != null){
            // TODO: Frilans
            throw new UnsupportedOperationException("Støtter ikke frilanser for OMS");
        }

        var fraværsperioder = søknadsinnhold.getFraværsperioder();
        List<OppgittFraværPeriode> oppgittFraværPerioder = new ArrayList<>();
        for (FraværPeriode fraværPeriode : fraværsperioder) {
            for (SelvstendigNæringsdrivende sn : snAktiviteter) {
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her, tolker om til InternArbeidsforholdRef.nullRef() ved fastsette uttak.
                BigDecimal skalJobbeProsent = null; // får ikke fra søknad, må settes til null dersom jobberNormaltPerUke er satt til null
                var arbeidsgiver = sn.organisasjonsnummer != null
                    ? Arbeidsgiver.virksomhet(sn.organisasjonsnummer.verdi)
                    : (søker.norskIdentitetsnummer != null
                    ? Arbeidsgiver.fra(new AktørId(søker.norskIdentitetsnummer.verdi))
                    : null);

                OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(fraværPeriode.getPeriode().getFraOgMed(), fraværPeriode.getPeriode().getTilOgMed(),
                    UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, arbeidsforholdRef, fraværPeriode.getDuration());
                oppgittFraværPerioder.add(oppgittFraværPeriode);
            }
        }
        return oppgittFraværPerioder;
    }
}
