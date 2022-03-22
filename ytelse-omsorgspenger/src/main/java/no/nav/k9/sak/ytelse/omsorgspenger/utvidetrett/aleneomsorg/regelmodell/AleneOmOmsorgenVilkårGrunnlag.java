package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import java.util.List;
import java.util.Optional;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;
import no.nav.k9.sak.typer.AktørId;

public class AleneOmOmsorgenVilkårGrunnlag implements VilkårGrunnlag {

    private final AktørId annenForelder;
    private final List<BostedsAdresse> andreForeldersAdresser;
    private final List<BostedsAdresse> søkersAdresser;

    public AleneOmOmsorgenVilkårGrunnlag(AktørId annenForelder, List<BostedsAdresse> andreForeldersAdresser, List<BostedsAdresse> søkersAdresser) {
        this.annenForelder = annenForelder;
        this.andreForeldersAdresser = andreForeldersAdresser;
        this.søkersAdresser = søkersAdresser;
    }

    public Optional<AktørId> getAnnenForelder() {
        return Optional.of(annenForelder);
    }

    public List<BostedsAdresse> getAndreForeldersAdresser() {
        return andreForeldersAdresser;
    }

    public List<BostedsAdresse> getSøkersAdresser() {
        return søkersAdresser;
    }


    @Override
    public String toString() {
        return "AleneOmOmsorgenVilkårGrunnlag{" +
            "annenForelder=" + annenForelder +
            ", andreForeldersAdresser=" + andreForeldersAdresser +
            ", søkersAdresser=" + søkersAdresser +
            '}';
    }
}
