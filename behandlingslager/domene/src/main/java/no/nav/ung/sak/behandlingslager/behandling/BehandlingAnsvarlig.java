package no.nav.ung.sak.behandlingslager.behandling;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;

import java.util.Collection;
import java.util.List;

@Entity(name = "BehandlingAnsvarlig")
@Table(name = "BEHANDLING_ANSVARLIG")
public class BehandlingAnsvarlig extends no.nav.ung.sak.behandlingslager.BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_ANSVARLIG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = BehandlingDelKodeverdiConverter.class)
    @Column(name = "behandling_del", nullable = false)
    private BehandlingDel behandlingDel;

    @Column(name = "totrinnsbehandling", nullable = false)
    private boolean toTrinnsBehandling = false;

    @Column(name = "ansvarlig_saksbehandler")
    private String ansvarligSaksbehandler;

    @Column(name = "ansvarlig_beslutter")
    private String ansvarligBeslutter;

    @Column(name = "behandlende_enhet")
    private String behandlendeEnhet;

    @Column(name = "behandlende_enhet_navn")
    private String behandlendeEnhetNavn;

    @Column(name = "behandlende_enhet_arsak")
    private String behandlendeEnhetÅrsak;

    public BehandlingAnsvarlig() {
        // For JPA
    }

    public BehandlingAnsvarlig(Long behandlingId, BehandlingDel behandlingDel) {
        this.behandlingId = behandlingId;
        this.behandlingDel = behandlingDel;
    }

    public BehandlingAnsvarlig kopierBehandlendeEnhet(Long nyBehandlingId) {
        BehandlingAnsvarlig ba = new BehandlingAnsvarlig(nyBehandlingId, behandlingDel);
        ba.behandlendeEnhet = this.behandlendeEnhet;
        ba.behandlendeEnhetNavn = this.behandlendeEnhetNavn;
        ba.behandlendeEnhetÅrsak = this.behandlendeEnhetÅrsak;
        return ba;
    }

    public static List<BehandlingAnsvarlig> kopierBehandlendeEnhet(Long nyBehandligId, Collection<BehandlingAnsvarlig> ansvarlige) {
        return ansvarlige.stream().map(it -> it.kopierBehandlendeEnhet(nyBehandligId)).toList();
    }


    public BehandlingDel getBehandlingDel() {
        return behandlingDel;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }

    public OrganisasjonsEnhet getBehandlendeOrganisasjonsEnhet() {
        if (behandlendeEnhet == null){
            throw new IllegalArgumentException("Behandlende enhet er ikke satt");
        }
        return new OrganisasjonsEnhet(behandlendeEnhet, behandlendeEnhetNavn);
    }

    public String getBehandlendeEnhetNavn() {
        return behandlendeEnhetNavn;
    }

    public String getBehandlendeEnhetÅrsak() {
        return behandlendeEnhetÅrsak;
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        if (ansvarligBeslutter != null && ansvarligBeslutter.equals(ansvarligSaksbehandler)) {
            throw new IllegalArgumentException("Kan ikke sette beslutter lik saksbehandler");
        }
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    public void setBehandlendeEnhet(OrganisasjonsEnhet enhet) {
        this.behandlendeEnhet = enhet != null? enhet.getEnhetId() : null;
        this.behandlendeEnhetNavn = enhet != null ? enhet.getEnhetNavn() : null;
    }

    public void setBehandlendeEnhetÅrsak(String behandlendeEnhetÅrsak) {
        this.behandlendeEnhetÅrsak = behandlendeEnhetÅrsak;
    }

    public boolean erTotrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public Long getBehandlingId() {
            return behandlingId;
    }

}


