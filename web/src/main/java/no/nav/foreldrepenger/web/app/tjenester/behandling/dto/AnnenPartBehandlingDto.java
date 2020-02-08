package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import no.nav.k9.sak.typer.Saksnummer;

public class AnnenPartBehandlingDto {

    private Saksnummer saksnr;

    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

    public AnnenPartBehandlingDto(Saksnummer saksnummer, Long behandlingId) {
        this.saksnr = saksnummer;
        this.behandlingId = behandlingId;
    }

    protected AnnenPartBehandlingDto() {
        //
    }

    public Saksnummer getSaksnr() {
        return saksnr;
    }

    public void setSaksnr(Saksnummer saksnr) {
        this.saksnr = saksnr;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }
}
