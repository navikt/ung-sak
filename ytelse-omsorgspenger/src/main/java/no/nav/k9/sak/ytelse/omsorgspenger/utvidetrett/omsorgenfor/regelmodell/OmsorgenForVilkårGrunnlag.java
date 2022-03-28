package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell;

import java.util.List;
import java.util.Map;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;
import no.nav.k9.sak.typer.Periode;

public class OmsorgenForVilkårGrunnlag implements VilkårGrunnlag {

    private final Relasjon relasjonMellomSøkerOgBarn;
    private final List<BostedsAdresse> søkersAdresser;
    private final List<BostedsAdresse> barnsAdresser;
    private final Map<Periode, List<BostedsAdresse>> perioderMedDeltBosted;

    public OmsorgenForVilkårGrunnlag(Relasjon relasjonMellomSøkerOgPleietrengende, List<BostedsAdresse> søkersAdresser, List<BostedsAdresse> pleietrengendeAdresser, Map<Periode, List<BostedsAdresse>> perioderMedDeltBosted) {
        this.relasjonMellomSøkerOgBarn = relasjonMellomSøkerOgPleietrengende;
        this.søkersAdresser = søkersAdresser;
        this.barnsAdresser = pleietrengendeAdresser;
        this.perioderMedDeltBosted = perioderMedDeltBosted;
    }

    public Relasjon getRelasjonMellomSøkerOgBarn() {
        return relasjonMellomSøkerOgBarn;
    }

    public List<BostedsAdresse> getSøkersAdresser() {
        return søkersAdresser;
    }

    public List<BostedsAdresse> getBarnsAdresser() {
        return barnsAdresser;
    }

    public Map<Periode, List<BostedsAdresse>> getPerioderMedDeltBosted() {
        return perioderMedDeltBosted;
    }

    @Override
    public String toString() {
        return "OmsorgenForVilkårGrunnlag{" +
            ", relasjonMellomSøkerOgBarn=" + relasjonMellomSøkerOgBarn +
            ", søkersAdresser=" + søkersAdresser +
            ", barnsAdresser=" + barnsAdresser +
            ", perioderMedDeltBosted=" + perioderMedDeltBosted +
            '}';
    }

}
