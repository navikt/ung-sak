package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor;

import java.util.List;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;

public class OmsorgenForGrunnlag implements VilkårGrunnlag {

    private final Relasjon relasjonMellomSøkerOgPleietrengende;
    private final List<BostedsAdresse> søkersAdresser;
    private final List<BostedsAdresse> pleietrengendeAdresser;

    public OmsorgenForGrunnlag(Relasjon relasjonMellomSøkerOgPleietrengende, List<BostedsAdresse> søkersAdresser, List<BostedsAdresse> pleietrengendeAdresser) {

        this.relasjonMellomSøkerOgPleietrengende = relasjonMellomSøkerOgPleietrengende;
        this.søkersAdresser = søkersAdresser;
        this.pleietrengendeAdresser = pleietrengendeAdresser;
    }

    public Relasjon getRelasjonMellomSøkerOgPleietrengende() {
        return relasjonMellomSøkerOgPleietrengende;
    }

    public List<BostedsAdresse> getSøkersAdresser() {
        return søkersAdresser;
    }

    public List<BostedsAdresse> getPleietrengendeAdresser() {
        return pleietrengendeAdresser;
    }

    @Override
    public String toString() {
        return "OmsorgenForGrunnlag{" +
            "relasjonMellomSøkerOgPleietrengende=" + relasjonMellomSøkerOgPleietrengende +
            ", søkersAdresser=" + søkersAdresser +
            ", pleietrengendeAdresser=" + pleietrengendeAdresser +
            '}';
    }
}
