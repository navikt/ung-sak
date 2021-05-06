package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering.AktoerId;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "TmpAktoerId")
@Table(name = "TMP_AKTOER_ID")
public class TmpAktoerId extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TMP_AKTOER_ID")
    @Column(name = "id")
    private Long id;

    @Column(name = "aktoer_id", nullable = false, updatable = false)
    private AktørId aktoerId;

    @Column(name = "ident", updatable = false)
    private String ident;

    @Column(name = "ident_type")
    private String identType = "FNR";

    TmpAktoerId() {
        //
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