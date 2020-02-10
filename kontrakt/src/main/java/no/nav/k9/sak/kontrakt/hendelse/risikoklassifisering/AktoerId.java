package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.kafka;

public class AktoerIdDto {

    private String aktoerId;

    public AktoerIdDto(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public String getAktoerId() {
        return aktoerId;
    }

}
