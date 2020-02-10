package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.Optional;

import no.nav.k9.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.vedtak.util.StringUtils;

final class UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold {

    private UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold() {
        // Skjul default constructor
    }

    static Optional<VurderArbeidsforholdHistorikkinnslag> utled(AvklarArbeidsforholdDto arbeidsforholdDto) {
        if (!StringUtils.nullOrEmpty(arbeidsforholdDto.getErstatterArbeidsforholdId())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.SLÅTT_SAMMEN_MED_ANNET);
        }
        if (Boolean.TRUE.equals(arbeidsforholdDto.getErNyttArbeidsforhold())) {
            return Optional.of(VurderArbeidsforholdHistorikkinnslag.NYTT_ARBEIDSFORHOLD);
        }
        return Optional.empty();
    }

}
