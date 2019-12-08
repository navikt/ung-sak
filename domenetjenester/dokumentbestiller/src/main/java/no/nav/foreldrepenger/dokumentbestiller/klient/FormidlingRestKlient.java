package no.nav.foreldrepenger.dokumentbestiller.klient;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.TekstFraSaksbehandlerDto;

public interface FormidlingRestKlient {
    void bestillDokument(DokumentbestillingDto dokumentbestillingDto);

    List<BrevmalDto> hentBrevMaler(BehandlingUuidDto behandlingUuidDto);

    Boolean erDokumentProdusert(DokumentProdusertDto dokumentProdusertDto);

    void lagreTekstFraSaksbehandler(TekstFraSaksbehandlerDto tekstFraSaksbehandlerDto);

    Optional<TekstFraSaksbehandlerDto> hentTekstFraSaksbehandler(BehandlingUuidDto behandlingUuidDto);
}
