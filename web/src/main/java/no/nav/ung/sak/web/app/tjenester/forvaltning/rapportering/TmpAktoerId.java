package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@Entity(name = "TmpAktoerId")
@Table(name = "TMP_AKTOER_ID")
public class TmpAktoerId extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TMP_AKTOER_ID")
    @Column(name = "id")
    private Long id;

    @NaturalId
    @Column(name = "aktoer_id", nullable = false)
    private AktørId aktoerId;

    @NaturalId
    @Column(name = "ident")
    private String ident;

    @NaturalId
    @Column(name = "ident_type")
    private String identType = "FNR";

    TmpAktoerId() {
        //
    }

    public TmpAktoerId(AktørId aktoerId, PersonIdent ident) {
        this.aktoerId = aktoerId;
        this.ident = ident == null ? null : ident.getIdent();
    }

    public TmpAktoerId(AktørId aktoerId, String ident) {
        this.aktoerId = aktoerId;
        this.ident = ident;
    }

    public AktørId getAktoerId() {
        return aktoerId;
    }

    public String getIdent() {
        return ident;
    }
}
