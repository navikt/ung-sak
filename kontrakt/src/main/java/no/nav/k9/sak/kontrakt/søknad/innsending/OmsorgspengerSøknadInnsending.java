package no.nav.k9.sak.kontrakt.søknad.innsending;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(OmsorgspengerSøknadInnsending.YTELSE_TYPE)
public class OmsorgspengerSøknadInnsending extends InnsendingInnhold {

    public static final String YTELSE_TYPE = "OMP";

    public OmsorgspengerSøknadInnsending() {
        super(FagsakYtelseType.OMSORGSPENGER);
    }
    
}
