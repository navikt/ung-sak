package no.nav.k9.sak.behandlingslager.notat;

import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.FagsakYtelseTypeKodeverdiConverter;
import no.nav.k9.sak.typer.AktørId;


@Entity(name = "NotatAktørEntitet")
@Table(name = "notat_aktoer")
public class NotatAktørEntitet extends BaseEntitet implements Notat {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_aktoer")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @OneToOne(mappedBy = "notatAktørEntitet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NotatAktørTekst notatAktørTekst;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "skjult", nullable = false, updatable = true)
    private boolean skjult;

    @Convert(converter = FagsakYtelseTypeKodeverdiConverter.class)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private FagsakYtelseType ytelseType = FagsakYtelseType.UDEFINERT;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NotatAktørEntitet(AktørId aktørId, String notatTekst, boolean skjult) {
        this.notatAktørTekst = new NotatAktørTekst(notatTekst);
        this.aktørId = aktørId;
        this.skjult = skjult;
        this.uuid = UUID.randomUUID();
    }

    NotatAktørEntitet() { }

    @Override
    public String getNotatTekst() {
        return notatAktørTekst.getTekst();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public long getVersjon() {
        return versjon;
    }

    public NotatGjelderType getGjelderType() {
        return NotatGjelderType.PLEIETRENGENDE;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @Override
    public Long getId() {
        return id;
    }

    public boolean isSkjult() {
        return skjult;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void skjul(boolean skjul) {
        this.skjult = skjul;
    }
}
