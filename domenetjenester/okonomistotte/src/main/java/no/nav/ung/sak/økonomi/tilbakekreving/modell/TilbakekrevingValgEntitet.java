package no.nav.ung.sak.økonomi.tilbakekreving.modell;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@Table(name = "TILBAKEKREVING_VALG")
@Entity(name = "TilbakekrevingValgEntitet")
class TilbakekrevingValgEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILBAKEKREVING_VALG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;


    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private Long versjon;

    @Column(name = "tbk_vilkaar_oppfylt")
    private Boolean vilkarOppfylt = Boolean.FALSE;

    @Column(name = "grunn_til_reduksjon")
    private Boolean grunnTilReduksjon = Boolean.FALSE;

    @Column(name = "varseltekst")
    private String varseltekst;

    @Convert(converter = TilbakekrevingVidereBehandlingKodeverdiConverter.class)
    @Column(name="videre_behandling")
    private TilbakekrevingVidereBehandling tilbakekrevningsVidereBehandling;

    TilbakekrevingValgEntitet() {
        // For hibernate
    }

    public Boolean erVilkarOppfylt() {
        return vilkarOppfylt;
    }

    public Boolean erGrunnTilReduksjon() {
        return grunnTilReduksjon;
    }

    public TilbakekrevingVidereBehandling getTilbakekrevningsVidereBehandling() {
        return tilbakekrevningsVidereBehandling;
    }

    public String getVarseltekst() {
        return varseltekst;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    void reaktiver() {
        this.aktiv = true;
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private TilbakekrevingValgEntitet kladd = new TilbakekrevingValgEntitet();

        public Builder medBehandling(Behandling behandling) {
            kladd.behandlingId = behandling.getId();
            return this;
        }

        public Builder medVilkarOppfylt(Boolean vilkarOppfylt) {
            kladd.vilkarOppfylt = vilkarOppfylt;
            return this;
        }

        public Builder medGrunnTilReduksjon(Boolean grunnTilReduksjon) {
            kladd.grunnTilReduksjon = grunnTilReduksjon;
            return this;
        }

        public Builder medTilbakekrevningsVidereBehandling(TilbakekrevingVidereBehandling tilbakekrevningsVidereBehandling) {
            kladd.tilbakekrevningsVidereBehandling = tilbakekrevningsVidereBehandling;
            return this;
        }

        public Builder medVarseltekst(String varseltekst) {
            kladd.varseltekst = varseltekst;
            return this;
        }

        public TilbakekrevingValgEntitet build() {
            Objects.requireNonNull(kladd.behandlingId, "behandlingId");
            Objects.requireNonNull(kladd.tilbakekrevningsVidereBehandling, "tilbakekrevningsVidereBehandling");
            return kladd;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + id //$NON-NLS-1$
            + ", behandling=" + behandlingId //$NON-NLS-1$
            + ", aktiv=" + aktiv //$NON-NLS-1$
            + ">"; //$NON-NLS-1$

    }

}
