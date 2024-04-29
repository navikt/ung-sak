package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningSteg;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

public class FagsakYtelseTypeMapper {
    private FagsakYtelseTypeMapper() {
    }

    public static no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType mapFagsakYtelseType(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case FRISINN -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.FRISINN;
            case PLEIEPENGER_SYKT_BARN -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case OMSORGSPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.OPPLÆRINGSPENGER;
            default -> throw new IllegalArgumentException("Kalkulus kan ikke beregning ytelse " + ytelseType.getKode());
        };
    }

}
