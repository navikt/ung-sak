package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class KroniskSykSøknadsperiodeUtleder {
    private Logger logger = LoggerFactory.getLogger(KroniskSykSøknadsperiodeUtleder.class);


    public DatoIntervallEntitet utledFaktiskSøknadsperiode(NavigableSet<DatoIntervallEntitet> søktePerioder, LocalDate barnetsFødselsdato) {
        return utledFaktiskSøknadsperiode(søktePerioder.first(), barnetsFødselsdato);
    }

    public DatoIntervallEntitet utledFaktiskSøknadsperiode(DatoIntervallEntitet søktPeriode, LocalDate barnetsFødselsdato) {

        // ikke åpne fagsaken før barnets fødselsdato
        var fødselsdato = barnetsFødselsdato;
        // 1. jan minst 3 år før søknad sendt inn (spesielle særtilfeller tillater at et går an å sette tilbake it itid

        LocalDate søknadFom = søktPeriode.getFomDato();
        var fristFørSøknadsdato = søknadFom.minusYears(3).withMonth(1).withDayOfMonth(1);

        var mindato = List.of(fødselsdato, fristFørSøknadsdato).stream().max(LocalDate::compareTo).get();

        // kan ikke gå lenger enn til 18 år (kun oppfylt i årskvantum om kronisk syk også fins
        var maksdato = barnetsFødselsdato.plusYears(18).withMonth(12).withDayOfMonth(31);

        if (maksdato.isBefore(mindato) || søknadFom.isAfter(maksdato)) {
            logger.warn("Har ingen reell periode å vurdere. mindato {}, maksdato {}, søknadsdato {}", mindato, maksdato, søknadFom);
            return DatoIntervallEntitet.fraOgMedTilOgMed(søknadFom, søknadFom);
        } else {
            return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato);
        }
    }
}
