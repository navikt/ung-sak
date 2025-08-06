package no.nav.ung.sak.behandlingslager.fritekst;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;

@Entity(name = "FritekstEntitet")
@Table(name = "KLAGE_FRITEKST")
@DynamicInsert
@DynamicUpdate
public class FritekstEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FRITEKST")
    private Long id;

    @Column(name = "behandling_id")
    private Long behandlingId;

    @Column(name = "fritekst_skrevet_av")
    private String skrevetAv;

    @Column(name = "fritekst")
    private String fritekst;


    public FritekstEntitet() {
        // Hibernate
    }

    public FritekstEntitet(Long behandlingId, String skrevetAv, String fritekst) {
        this.behandlingId = behandlingId;
        this.skrevetAv = skrevetAv;
        this.fritekst = fritekst;
    }

    public Long getId() {
        return id;
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getSkrevetAv() {
        return skrevetAv;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof FritekstEntitet)) {
            return false;
        }
        FritekstEntitet other = (FritekstEntitet) obj;
        return Objects.equals(this.behandlingId, other.behandlingId)
            && Objects.equals(this.skrevetAv, other.skrevetAv)
            && Objects.equals(this.fritekst, other.fritekst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, skrevetAv, fritekst);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "behandlingId=" + behandlingId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "skrevetAv=" + skrevetAv + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "fritekstTilBrev=" + fritekst //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }
}
