package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TilbakekrevingVidereBehandlingKodeverdiConverter;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;

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
