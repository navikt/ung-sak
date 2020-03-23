package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import java.util.Optional;

import no.nav.k9.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;

final class UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon {

    private UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon() {
        // Skjul default constructor
    }

    static Optional<VurderArbeidsforholdHistorikkinnslag> utled(AvklarArbeidsforholdDto arbeidsforholdDto) {
        if (Boolean.TRUE.equals(arbeidsforholdDto.getBrukPermisjon())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON);
        }
        if (Boolean.FALSE.equals(arbeidsforholdDto.getBrukPermisjon())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON);
        }
        return Optional.empty();
    }

}
