package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import java.util.List;

/**
 * Response objekt - skal ikke sendes inn
 *
 * @param harBrev             true hvis det finnes brev for behandling
 * @param vedtaksbrevValg     liste vedtaksbrev og valg
 */
public record VedtaksbrevValgResponse(
    boolean harBrev,
    List<VedtaksbrevValg> vedtaksbrevValg) {

}




