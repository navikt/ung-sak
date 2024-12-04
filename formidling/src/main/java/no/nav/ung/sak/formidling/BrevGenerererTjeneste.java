package no.nav.ung.sak.formidling;


import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.dto.GenerertBrev;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.kodeverk.IdType;
import no.nav.ung.sak.formidling.kodeverk.RolleType;

public class BrevGenerererTjeneste {

    public GenerertBrev genererPdf(BrevbestillingDto brevbestillingDto) {
        PartResponseDto halvorsen = new PartResponseDto("123", "halvorsen", IdType.AKTØRID, RolleType.BRUKER);
        return new GenerertBrev(
            "123".getBytes(),
            halvorsen,
            halvorsen,
            brevbestillingDto.malType()
        );
    }
}
