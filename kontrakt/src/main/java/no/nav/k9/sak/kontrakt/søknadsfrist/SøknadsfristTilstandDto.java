package no.nav.k9.sak.kontrakt.søknadsfrist;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SøknadsfristTilstandDto {

    private List<KravDokumentStatus> kravDokumentStatus;

    public SøknadsfristTilstandDto(List<KravDokumentStatus> kravDokumentStatus) {
        this.kravDokumentStatus = kravDokumentStatus;
    }

    public List<KravDokumentStatus> getKravDokumentStatus() {
        return kravDokumentStatus;
    }
}
