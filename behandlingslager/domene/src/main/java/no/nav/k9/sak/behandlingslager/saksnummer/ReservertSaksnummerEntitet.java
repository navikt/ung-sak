package no.nav.k9.sak.behandlingslager.saksnummer;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.FagsakYtelseTypeKodeverdiConverter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "ReservertSaksnummer")
@Table(name = "RESERVERT_SAKSNUMMER")
public class ReservertSaksnummerEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RESERVERT_SAKSNUMMER")
    @Column(name = "id")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", nullable = false, updatable = false)))
    private Saksnummer saksnummer;

    @Convert(converter = FagsakYtelseTypeKodeverdiConverter.class)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private FagsakYtelseType ytelseType = FagsakYtelseType.UDEFINERT;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", nullable = false, updatable = false)))
    private AktørId brukerAktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "pleietrengende_aktoer_id")))
    private AktørId pleietrengendeAktørId;

    @Column(name = "slettet", nullable = false)
    private boolean slettet = false;

    ReservertSaksnummerEntitet() {
        // for hibernate
    }

    ReservertSaksnummerEntitet(Saksnummer saksnummer, FagsakYtelseType ytelseType, String brukerAktørId, String pleietrengendeAktørId) {
        this.saksnummer = saksnummer;
        this.ytelseType = ytelseType;
        this.brukerAktørId = new AktørId(brukerAktørId);
        this.pleietrengendeAktørId = pleietrengendeAktørId != null ? new AktørId(pleietrengendeAktørId) : null;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public AktørId getBrukerAktørId() {
        return brukerAktørId;
    }

    public AktørId getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    void setSlettet() {
        this.slettet = true;
    }
}
