package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.klage.KlageAvvistÅrsak;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KlageFormkravResultatDto {
    private UUID påklagdBehandlingRef;
    private BehandlingType påklagdBehandlingType;
    private String begrunnelse;
    private boolean erKlagerPart;
    private boolean erKlageKonkret;
    private boolean erKlagefirstOverholdt;
    private boolean erSignert;
    private List<KlageAvvistÅrsak> avvistArsaker;


    public KlageFormkravResultatDto() {
    }

    public List<KlageAvvistÅrsak> getAvvistArsaker() {
        return avvistArsaker;
    }

    public void setAvvistArsaker(List<KlageAvvistÅrsak> avvistArsaker) {
        this.avvistArsaker = avvistArsaker;
    }

    public UUID getPåklagdBehandlingRef() {
        return påklagdBehandlingRef;
    }

    public void setPaKlagdBehandlingRef(UUID påklagdBehandlingRef) {
        this.påklagdBehandlingRef = påklagdBehandlingRef;
    }

    public BehandlingType getPåklagdBehandlingType() {
        return påklagdBehandlingType;
    }

    public void setPåklagdBehandlingType(BehandlingType påklagdBehandlingType) {
        this.påklagdBehandlingType = påklagdBehandlingType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean isErKlagerPart() {
        return erKlagerPart;
    }

    public void setErKlagerPart(boolean erKlagerPart) {
        this.erKlagerPart = erKlagerPart;
    }

    public boolean isErKlageKonkret() {
        return erKlageKonkret;
    }

    public void setErKlageKonkret(boolean erKlageKonkret) {
        this.erKlageKonkret = erKlageKonkret;
    }

    public boolean isErKlagefirstOverholdt() {
        return erKlagefirstOverholdt;
    }

    public void setErKlagefirstOverholdt(boolean erKlagefirstOverholdt) {
        this.erKlagefirstOverholdt = erKlagefirstOverholdt;
    }

    public boolean isErSignert() {
        return erSignert;
    }

    public void setErSignert(boolean erSignert) {
        this.erSignert = erSignert;
    }
}
