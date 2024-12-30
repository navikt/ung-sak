package no.nav.ung.sak.formidling.domene;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import no.nav.ung.kodeverk.formidling.IdType;

@Embeddable
public class BrevMottaker {
    @Column(name = "mottaker_id", updatable = false, nullable = false)
    private String mottakerId;
    @Column(name = "mottaker_id_type", updatable = false, nullable = false)
    @Enumerated(EnumType.STRING)
    private IdType mottakerIdType;

    public BrevMottaker(String mottakerId, IdType mottakerIdType) {
        this.mottakerId = mottakerId;
        this.mottakerIdType = mottakerIdType;
    }

    public String getMottakerId() {
        return mottakerId;
    }

    public IdType getMottakerIdType() {
        return mottakerIdType;
    }
}
